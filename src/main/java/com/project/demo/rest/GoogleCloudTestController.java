package com.project.demo.rest;

import com.project.demo.service.GoogleCloudApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleCloudTestController {
    private final GoogleCloudApiService googleCloudApiService;

    @Autowired
    public GoogleCloudTestController(GoogleCloudApiService googleCloudApiService) {
        this.googleCloudApiService = googleCloudApiService;
    }

    @GetMapping("/api/test/gemini")
    public String testGemini(@RequestParam String prompt) {
        try {
            String response = googleCloudApiService.askGemini(prompt);
            return "Respuesta de Gemini: " + response;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al llamar a Gemini: " + e.getMessage();
        }
    }
}
