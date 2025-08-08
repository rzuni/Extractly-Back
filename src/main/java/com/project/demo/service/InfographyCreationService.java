package com.project.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
public class InfographyCreationService {

    @Autowired
    private PdfTextExtractorService pdfTextExtractorService;

    @Autowired
    private GoogleCloudApiService googleCloudApiService;

    public byte[] createInfographyImageFromPdf(MultipartFile pdfFile, String title) throws IOException, Exception {

        // Paso 1: Extraer texto del PDF
        String extractedText = pdfTextExtractorService.extractTextFromPdf(pdfFile);

        // Paso 2: Usar Gemini para resumir el texto y generar un prompt para la infografía
        String infographicPrompt = createInfographicPromptWithGemini(extractedText, title);

        // Paso 3: Usar la IA de Imagen para generar la imagen de la infografía

        byte[] infographicImageBytes = googleCloudApiService.generateImageInfographic(infographicPrompt);

        // Devolver los bytes de la imagen directamente
        return infographicImageBytes;
    }

    private String createInfographicPromptWithGemini(String text, String title) throws Exception {
        String geminiPrompt = String.format(
                "Eres un experto en diseño gráfico de infografías. Tu tarea es generar un prompt para una IA de creación de imágenes, con el fin de crear una infografía vertical y profesional, en un estilo de diseño plano (flat design) y minimalista. " +
                        "La infografía debe incluir el título principal: '%s', y debe estar basada en la información clave del siguiente texto: '%s'. " +
                        "Organiza el contenido en secciones claras, utilizando íconos relevantes, gráficos sencillos o ilustraciones. " +
                        "El prompt debe especificar un diseño limpio, con una paleta de colores coherente y profesional. " +
                        "No incluyas texto de relleno o explicaciones en tu respuesta, solo el prompt listo para ser usado.",
                title, text
        );
        return googleCloudApiService.askGemini(geminiPrompt);
    }
}