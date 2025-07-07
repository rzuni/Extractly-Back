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

    @Value("${gcp.vertexai.location:us-central1}") // us-central1 es una región común para Vertex AI
    private String vertexAiLocation;

    /**
     * Realiza una pregunta basada en texto al modelo Gemini.
     * @param prompt El texto de la pregunta para Gemini.
     * @return La respuesta de texto de Gemini.
     * @throws Exception Si ocurre un error al llamar a la API.
     */
    public String askGemini(String prompt) throws Exception {
        System.out.println("Solicitando a Gemini con ID de proyecto: " + projectId + " y región: " + vertexAiLocation);
        try (VertexAI vertexAi = new VertexAI(projectId, vertexAiLocation)) {
            GenerativeModel model = new GenerativeModel("gemini-2.5-pro", vertexAi);
            GenerateContentResponse response = model.generateContent(prompt);

            // ***** CÓDIGO CLAVE para extraer el texto de la respuesta de Gemini *****
            if (response != null && !response.getCandidatesList().isEmpty()) {
                // Obtiene el texto del primer candidato y la primera parte (generalmente solo hay una parte de texto)
                return response.getCandidatesList().get(0).getContent().getPartsList().get(0).getText();
            } else {
                return "No se pudo obtener una respuesta de texto de Gemini.";
            }
            // *******************************************************************
        }
    }
}