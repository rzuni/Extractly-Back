package com.project.demo.rest.gemini;

import com.project.demo.service.GoogleCloudApiService; // <<< Importación correcta para Gemini
import com.project.demo.service.PptxGeneratorService;
import com.project.demo.service.PdfTextExtractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping; // Para la anotación @RequestMapping

import java.io.IOException;

@RestController
@RequestMapping("/api/google-cloud/gemini/pptx-summary")
public class PdfToPptxController {

    private final PdfTextExtractorService pdfTextExtractorService;
    private final GoogleCloudApiService googleCloudApiService; // <<< Servicio correcto para Gemini
    private final PptxGeneratorService pptxGeneratorService;

    @Autowired
    public PdfToPptxController(
            PdfTextExtractorService pdfTextExtractorService,
            GoogleCloudApiService googleCloudApiService, // <<< Inyección correcta
            PptxGeneratorService pptxGeneratorService
    ) {
        this.pdfTextExtractorService = pdfTextExtractorService;
        this.googleCloudApiService = googleCloudApiService;
        this.pptxGeneratorService = pptxGeneratorService;
    }

    @PostMapping("/pdf-to-pptx")
    public ResponseEntity<byte[]> uploadPdfAndSummarizeToPptx(
            @RequestParam("file") MultipartFile pdfFile, // Cambiado el nombre de variable a pdfFile para consistencia
            @RequestParam(value = "customPrompt", required = false) String customPrompt) {
        try {
            // 1. Extraer el texto del PDF
            String extractedText = pdfTextExtractorService.extractTextFromPdf(pdfFile);

            // 2. Construir el prompt para Gemini (para resumen)
            String promptForGemini;
            if (customPrompt != null && !customPrompt.isEmpty()) {
                // Si el usuario proporciona un prompt personalizado, úsalo con el texto extraído
                promptForGemini = customPrompt + "\n\nTexto a resumir: " + extractedText; // Texto a resumir en lugar de "Texto original"
            } else {
                // Prompt por defecto para resumen
                promptForGemini = "Genera un resumen detallado del siguiente texto:\n\n" + extractedText;
            }

            // 3. Obtener la respuesta de Gemini (el resumen)
            String geminiSummaryResponse = googleCloudApiService.askGemini(promptForGemini); // <<< Uso de GoogleCloudApiService y askGemini

            // 4. Generar el archivo PPTX a partir del texto de resumen
            byte[] pptxBytes = pptxGeneratorService.generatePptxFromSummary(geminiSummaryResponse);

            // 5. Configurar los headers para la descarga del archivo PPTX
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
            headers.setContentDispositionFormData("attachment", pdfFile.getOriginalFilename() + "-summary.pptx"); // Usa el nombre original del PDF
            headers.setContentLength(pptxBytes.length);

            // 6. Devolver la respuesta con el archivo PPTX
            return new ResponseEntity<>(pptxBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(("Error de archivo: " + e.getMessage()).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(("Error de E/S al procesar el archivo o generar PPTX: " + e.getMessage()).getBytes());
        } catch (Exception e) { // Captura cualquier otra excepción genérica
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(("Error general del servicio: " + e.getMessage()).getBytes());
        }
    }
}