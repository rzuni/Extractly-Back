package com.project.demo.service;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleCloudApiService {

    @Value("${gcp.project.id:extractly-465000}")
    private String projectId;

    @Value("${gcp.vertexai.location:us-central1}")
    private String vertexAiLocation;

    public String askGemini(String prompt) throws Exception {
        try (VertexAI vertexAi = new VertexAI(projectId, vertexAiLocation)) {
            GenerativeModel model = new GenerativeModel("gemini-2.0-flash", vertexAi);
            GenerateContentResponse response = model.generateContent(prompt);

            // Código que maneja la respuesta de Gemini
            if (response != null && !response.getCandidatesList().isEmpty()) {
                return response.getCandidatesList().get(0).getContent().getPartsList().get(0).getText();
            } else {
                return "No se pudo obtener una respuesta de texto de Gemini.";
            }

        }
    }
}