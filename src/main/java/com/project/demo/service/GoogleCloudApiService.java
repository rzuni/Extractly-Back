package com.project.demo.service;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.Part;
import com.google.protobuf.ByteString;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GoogleCloudApiService {

    @Value("${gcp.project.id:extractly-465000}")
    private String projectId;

    @Value("${gcp.vertexai.location:us-central1}")
    private String vertexAiLocation;

    public String askGemini(String prompt) throws Exception {
        try (VertexAI vertexAi = new VertexAI(projectId, vertexAiLocation)) {
            GenerativeModel model = new GenerativeModel("gemini-2.5-flash", vertexAi);
            GenerateContentResponse response = model.generateContent(prompt);
            if (response != null && !response.getCandidatesList().isEmpty()) {
                return response.getCandidatesList().get(0).getContent().getPartsList().get(0).getText();
            } else {
                return "No se pudo obtener una respuesta de texto de Gemini.";
            }
        }
    }

    public byte[] generateImageInfographic(String imagePrompt) throws IOException {
        String imageGenerationModelName = "imagen-4.0-fast-generate-preview-06-06";
        try (VertexAI vertexAi = new VertexAI(projectId, vertexAiLocation)) {
            GenerativeModel model = new GenerativeModel(imageGenerationModelName, vertexAi);
            Content content = Content.newBuilder().addParts(Part.newBuilder().setText(imagePrompt)).build();
            GenerateContentResponse response = model.generateContent(content);
            if (response.getCandidatesCount() > 0 && response.getCandidates(0).getContent().getPartsCount() > 0) {
                for (Part part : response.getCandidates(0).getContent().getPartsList()) {
                    if (part.hasInlineData()) {
                        ByteString imageBytes = part.getInlineData().getData();
                        return imageBytes.toByteArray();
                    }
                }
            }
            throw new IOException("No se pudo obtener datos de imagen de la respuesta del modelo de IA.");
        } catch (Exception e) {
            System.err.println("Error al generar imagen de infografía con Vertex AI: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Error al generar imagen de infografía con IA: " + e.getLocalizedMessage(), e);
        }
    }
}