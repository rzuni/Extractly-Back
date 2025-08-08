package com.project.demo.rest.gemini;

import com.project.demo.service.InfographyCreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/infography")
public class PdfToInfographyController {

    @Autowired
    private InfographyCreationService infographyCreationService;

    @PostMapping(value = "/create-image-from-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> createInfographyImageFromPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", defaultValue = "Infografía Generada") String title) {

        try {
            byte[] infographyImageBytes = infographyCreationService.createInfographyImageFromPdf(file, title);

            String filename = "infografia_" + System.currentTimeMillis() + ".png";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(infographyImageBytes);

        } catch (IllegalArgumentException e) {
            System.err.println("Error en los parámetros de entrada: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        } catch (IOException e) {
            System.err.println("Error de E/S al procesar el archivo o comunicarse con la IA: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error de E/S: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            System.err.println("Error al generar la infografía: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error al generar la infografía: " + e.getMessage()).getBytes());
        }
    }
}