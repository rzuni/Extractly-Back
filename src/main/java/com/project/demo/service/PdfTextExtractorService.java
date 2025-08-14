package com.project.demo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfTextExtractorService {
    public String extractTextFromPdf(MultipartFile pdfFile) throws IOException {
        if (pdfFile.isEmpty()) {
            throw new IllegalArgumentException("El archivo PDF está vacío.");
        }
        if (!"application/pdf".equals(pdfFile.getContentType())) {
            throw new IllegalArgumentException("El archivo no es un PDF válido.");
        }

        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }
}
