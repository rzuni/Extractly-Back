package com.project.demo.service;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.storage.*;
import com.google.cloud.videointelligence.v1.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YoutubeToPromptService {

    @Value("${gcp.project.id}")
    private String projectId;

    @Value("${google.cloud.bucket-name}")
    private String bucketName;

    @Value("${yt-dlp.path}")
    private String ytDlpPath;

    private final Storage storage;

    public YoutubeToPromptService(
            @Value("${gcp.project.id}") String projectId,
            @Value("${google.cloud.bucket-name}") String bucketName,
            @Value("${yt-dlp.path}") String ytDlpPath) {
        this.projectId = projectId;
        this.bucketName = bucketName;
        this.ytDlpPath = ytDlpPath;
        this.storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    }


    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?(?:youtube\\.com|youtu\\.be)/(?:watch\\?v=|embed/|v/|)([^#&?\\n]{11})"
    );

    public String extractYoutubeId(String url) {
        Matcher matcher = YOUTUBE_VIDEO_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public String generatePromptFromYoutubeUrl(String youtubeUrl, String languageCode) throws Exception {
        String videoId = extractYoutubeId(youtubeUrl);
        if (videoId == null) {
            throw new IllegalArgumentException("No se pudo extraer el ID del video de la URL proporcionada: " + youtubeUrl);
        }

        // 1. Descargar video con yt-dlp (audio preferido)
        String outputFileName = videoId + "-" + UUID.randomUUID() + ".mp4";
        Path localPath = Paths.get(outputFileName);

        List<String> commandArgs = new ArrayList<>();
        commandArgs.add(ytDlpPath);
        commandArgs.add("-f");
        commandArgs.add("bestaudio[ext=m4a]");
        commandArgs.add("--merge-output-format");
        commandArgs.add("mp4");
        commandArgs.add("-o");
        commandArgs.add(localPath.toAbsolutePath().toString());
        commandArgs.add(youtubeUrl);

        ProcessBuilder builder = new ProcessBuilder(commandArgs);

        builder.redirectErrorStream(true);
        Process process = builder.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error al ejecutar yt-dlp. Código de salida: " + exitCode + " para URL: " + youtubeUrl + ". Revisa la salida de yt-dlp en los logs.");
        }


        // 2. Subir a GCS
        String gcsObjectName = "youtube-transcripts/" + outputFileName;

        BlobId blobId = BlobId.of(bucketName, gcsObjectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, Files.readAllBytes(localPath));
        String gcsUri = gcsUri(bucketName, gcsObjectName);

        // 3. Transcribir usando Video Intelligence
        String prompt = getTranscriptFromGcs(gcsUri, languageCode);

        // 4. Eliminar archivo local
        try {
            Files.deleteIfExists(localPath);
        } catch (IOException e) {
            System.err.println("Error al eliminar archivo local: " + localPath + " - " + e.getMessage());
        }

        // 5. Eliminar archivo de GCS (OPCIONAL, pero recomendado para archivos temporales)
        try {
            storage.delete(blobId);
        } catch (StorageException e) {
            System.err.println("Error al eliminar archivo de GCS: " + gcsUri + " - " + e.getMessage());
        }

        return prompt;
    }

    private String getTranscriptFromGcs(String gcsUri, String languageCode) throws Exception {
        try (VideoIntelligenceServiceClient client = VideoIntelligenceServiceClient.create()) {

            SpeechTranscriptionConfig config = SpeechTranscriptionConfig.newBuilder()
                    .setLanguageCode(languageCode)
                    .setEnableAutomaticPunctuation(true)
                    .build();

            VideoContext context = VideoContext.newBuilder()
                    .setSpeechTranscriptionConfig(config)
                    .build();

            AnnotateVideoRequest request = AnnotateVideoRequest.newBuilder()
                    .setInputUri(gcsUri)
                    .addFeatures(Feature.SPEECH_TRANSCRIPTION)
                    .setVideoContext(context)
                    .build();

            OperationFuture<AnnotateVideoResponse, AnnotateVideoProgress> future = client.annotateVideoAsync(request);

            // Espera hasta 15 minutos para la transcripción. Ajusta según la duración del video.
            AnnotateVideoResponse response = future.get(15, TimeUnit.MINUTES);
            StringBuilder transcriptBuilder = new StringBuilder();

            if (response.getAnnotationResultsList().isEmpty()) {
                return "No se encontraron resultados de anotación. El video puede no tener audio o el idioma no es compatible.";
            }

            for (VideoAnnotationResults result : response.getAnnotationResultsList()) {
                if (result.getSpeechTranscriptionsList().isEmpty()) {
                    continue;
                }
                for (SpeechTranscription transcription : result.getSpeechTranscriptionsList()) {
                    for (SpeechRecognitionAlternative alternative : transcription.getAlternativesList()) {
                        transcriptBuilder.append(alternative.getTranscript()).append(" ");
                    }
                }
            }

            String fullTranscript = transcriptBuilder.toString().trim();
            if (fullTranscript.isEmpty()) {
                return "La transcripción resultante está vacía. Es posible que no se haya detectado voz en el video.";
            }
            return fullTranscript;
        }
    }

    private String gcsUri(String bucketName, String objectName) {
        return String.format("gs://%s/%s", bucketName, objectName);
    }
}