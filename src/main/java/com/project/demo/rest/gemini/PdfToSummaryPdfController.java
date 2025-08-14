package com.project.demo.rest.gemini;

import com.project.demo.service.GoogleCloudApiService;
import com.project.demo.service.PdfTextExtractorService;
import com.project.demo.service.PdfGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class PdfToSummaryPdfController {
    private final PdfTextExtractorService pdfTextExtractorService;
    private final GoogleCloudApiService googleCloudApiService;
    private final PdfGeneratorService pdfGeneratorService;

    @Autowired
    public PdfToSummaryPdfController(
            PdfTextExtractorService pdfTextExtractorService,
            GoogleCloudApiService googleCloudApiService,
            PdfGeneratorService pdfGeneratorService) {
        this.pdfTextExtractorService = pdfTextExtractorService;
        this.googleCloudApiService = googleCloudApiService;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    @PostMapping("/api/google-cloud/gemini/pdf-to-summary-pdf")
    public ResponseEntity<byte[]> processPdfAndGenerateSummaryPdf(
            @RequestParam("file") MultipartFile pdfFile,
            @RequestParam(value = "customPrompt", required = false) String customPrompt) {
        try {

            String extractedText = pdfTextExtractorService.extractTextFromPdf(pdfFile);

            String promptForGemini;
            if (customPrompt != null && !customPrompt.isEmpty()) {
                promptForGemini = customPrompt + "\n\nTexto original: " + extractedText;
            } else {
                promptForGemini = "Genera un resumen detallado del siguiente:\n\n" + extractedText;
            }

            String geminiResponse = googleCloudApiService.askGemini(promptForGemini);

            byte[] pdfBytes = pdfGeneratorService.generatePdfFromText(geminiResponse, "resumen_gemini.pdf");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "resumen_gemini.pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(("Error de archivo: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(("Error al procesar PDF o generar resumen: " + e.getMessage()).getBytes());
        }
    }
}
