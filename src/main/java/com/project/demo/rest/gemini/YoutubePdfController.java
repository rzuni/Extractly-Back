package com.project.demo.rest.gemini;

import com.project.demo.service.GoogleCloudApiService;
import com.project.demo.service.PdfGeneratorService;
import com.project.demo.service.YoutubeToPromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/youtube")
public class YoutubePdfController {

    @Autowired
    private YoutubeToPromptService youtubeToPromptService;

    @Autowired
    private GoogleCloudApiService googleCloudApiService;

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    @GetMapping("/resumen/pdf")
    public ResponseEntity<byte[]> generarPdfDesdeYoutube(
            @RequestParam String youtubeUrl,
            @RequestParam(defaultValue = "es-ES") String languageCode,
            @RequestParam(required = false) String customPrompt
    ) {
        if (youtubeUrl == null || youtubeUrl.trim().isEmpty()) {
            System.err.println("Error: La URL de YouTube no puede estar vacía.");
            return ResponseEntity.badRequest()
                    .body("La URL de YouTube no puede estar vacía.".getBytes());
        }

        try {
            // Paso 1: Obtener transcripción del video
            System.out.println("Iniciando Paso 1: Obteniendo transcripción del video de YouTube: " + youtubeUrl);
            String transcripcion = youtubeToPromptService.generatePromptFromYoutubeUrl(youtubeUrl, languageCode);

            if (transcripcion == null || transcripcion.trim().isEmpty() ||
                    transcripcion.contains("No se pudo extraer una transcripción útil") ||
                    transcripcion.contains("La transcripción resultante está vacía")) {
                System.err.println("Error: No se pudo obtener una transcripción útil del video. Transcripción: " + transcripcion);
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body("No se pudo obtener una transcripción útil del video. El video podría no tener audio o el idioma no ser compatible.".getBytes());
            }

            // Paso 2: Generar el prompt final para Gemini y resumir
            String baseGeminiPrompt = "Genera un resumen en español, claro y conciso, del siguiente contenido de un video de YouTube, organizando los puntos clave en párrafos separados:\n\n" + transcripcion;
            String finalGeminiPrompt = (customPrompt != null && !customPrompt.trim().isEmpty()) ?
                    customPrompt.trim() + "\n\nContenido a resumir:\n" + transcripcion :
                    baseGeminiPrompt;

            String resumen = googleCloudApiService.askGemini(finalGeminiPrompt);

            if (resumen == null || resumen.trim().isEmpty() || resumen.contains("No se pudo obtener una respuesta de texto de Gemini.")) { // Check for specific error message
                System.err.println("Error: El modelo Gemini no pudo generar un resumen útil. Respuesta: " + resumen);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("El modelo Gemini no pudo generar un resumen útil. Intenta con un prompt diferente o verifica el contenido del video.".getBytes());
            }

            // Paso 3: Generar el PDF con el resumen
            String nombreArchivo = "resumen_video_gemini.pdf";
            byte[] pdf = pdfGeneratorService.generatePdfFromText(resumen, nombreArchivo);

            if (pdf == null || pdf.length == 0) {
                System.err.println("Error: El PdfGeneratorService generó un PDF vacío o nulo.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al generar el archivo PDF del resumen.".getBytes());
            }

            // Paso 4: Retornar el PDF como archivo descargable
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (IllegalArgumentException e) {
            System.err.println("Error en la URL o argumentos: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(("Error en los parámetros de la solicitud: " + e.getMessage()).getBytes());
        } catch (IOException e) {
            System.err.println("Error de E/S al generar el PDF (ej. fuente no encontrada): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error al generar el PDF: " + e.getMessage() + ". Asegúrate de que la fuente esté disponible.").getBytes());
        }
        catch (Exception e) {
            System.err.println("Error interno del servidor al procesar la solicitud: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error interno del servidor: " + e.getLocalizedMessage()).getBytes());
        }
    }
}