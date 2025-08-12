package com.project.demo.rest.gemini;

import com.project.demo.service.GoogleCloudApiService;
import com.project.demo.service.PptxGeneratorService;
import com.project.demo.service.YoutubeToPromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/youtube")
public class YoutubePptxController {

    @Autowired
    private YoutubeToPromptService youtubeToPromptService;

    @Autowired
    private GoogleCloudApiService googleCloudApiService;

    @Autowired
    private PptxGeneratorService pptxGeneratorService;

    @GetMapping(value = "/create-pptx-from-url", produces = "application/vnd.openxmlformats-officedocument.presentationml.presentation")
    public ResponseEntity<byte[]> createPptxFromYoutubeUrl(
            @RequestParam String youtubeUrl,
            @RequestParam(defaultValue = "es-ES") String languageCode) {

        try {
            // Paso 1: Obtener la transcripción del video de YouTube
            String transcript = youtubeToPromptService.generatePromptFromYoutubeUrl(youtubeUrl, languageCode);

            // Paso 2: Usar Gemini para resumir la transcripción
            String geminiPrompt = String.format(
                    "Crea un resumen detallado y estructurado en forma de viñetas o puntos clave del siguiente texto: '%s'. El resumen debe ser ideal para una presentación de PowerPoint, con títulos de secciones y puntos concisos. Responde únicamente con el resumen, sin ninguna otra explicación.",
                    transcript
            );
            String summary = googleCloudApiService.askGemini(geminiPrompt);

            // Paso 3: Generar el archivo PPTX con el resumen
            byte[] pptxBytes = pptxGeneratorService.generatePptxFromSummary(summary);

            String filename = "resumen_video_" + System.currentTimeMillis() + ".pptx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                    .body(pptxBytes);

        } catch (IllegalArgumentException e) {
            System.err.println("Error en los parámetros de entrada: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        } catch (Exception e) {
            System.err.println("Error al procesar el video y generar el PPTX: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error al procesar el video y generar el PPTX: " + e.getMessage()).getBytes());
        }
    }
}