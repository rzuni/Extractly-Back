package com.project.demo.service;

import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.sl.usermodel.*;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.Rectangle;
import java.awt.Color;

@Service
public class PptxGeneratorService {

    public byte[] generatePptxFromSummary(String summaryText) {
        try (XMLSlideShow ppt = new XMLSlideShow()) {

            ppt.setPageSize(new java.awt.Dimension(1280, 720));

            XSLFSlideMaster defaultMaster = ppt.getSlideMasters().get(0);
            XSLFSlideLayout blankLayout = defaultMaster.getLayout(SlideLayout.BLANK);

            // Crear diapositiva de título
            XSLFSlide titleSlide = ppt.createSlide(defaultMaster.getLayout(SlideLayout.TITLE));
            XSLFTextShape titleShape = (XSLFTextShape) titleSlide.getPlaceholder(Placeholder.TITLE);
            if (titleShape != null) {
                titleShape.clearText();
                XSLFTextParagraph p = titleShape.addNewTextParagraph();
                XSLFTextRun r = p.addNewTextRun();
                r.setText("Resumen del Documento");
                r.setFontSize(48.0);
                r.setBold(true);
                titleShape.setHorizontalCentered(true);
            }

            XSLFTextShape subtitleShape = (XSLFTextShape) titleSlide.getPlaceholder(Placeholder.SUBTITLE);
            if (subtitleShape != null) {
                subtitleShape.clearText();
                XSLFTextParagraph p = subtitleShape.addNewTextParagraph();
                XSLFTextRun r = p.addNewTextRun();
                r.setText("Generado automáticamente por el sistema");
                r.setFontSize(24.0);
                subtitleShape.setHorizontalCentered(true);
            }

            // Configuración general de texto
            double fontSize = 24.0;
            int maxLinesPerSlide = 6;

            String[] lines;
            if (summaryText.contains("\n")) {
                lines = summaryText.split("\n");
            } else {
                // Dividir por bloques de palabras si no hay saltos de línea
                lines = splitIntoLineBlocks(summaryText, 90); // Estimar 90 caracteres por línea
            }

            XSLFSlide currentSlide = null;
            XSLFTextShape currentTextBox = null;
            int lineCounter = 0;

            for (String line : lines) {
                if (currentSlide == null || lineCounter >= maxLinesPerSlide) {
                    currentSlide = ppt.createSlide(blankLayout);
                    currentTextBox = currentSlide.createTextBox();
                    currentTextBox.setAnchor(new Rectangle(100, 100, 1080, 520));
                    currentTextBox.setVerticalAlignment(VerticalAlignment.TOP);
                    lineCounter = 0;
                }

                XSLFTextParagraph paragraph = currentTextBox.addNewTextParagraph();
                paragraph.setTextAlign(TextParagraph.TextAlign.LEFT);
                paragraph.setSpaceAfter(10.0); // Espacio entre líneas

                XSLFTextRun textRun = paragraph.addNewTextRun();
                textRun.setText(line.trim());
                textRun.setFontSize(fontSize);
                textRun.setFontFamily("OpenSans");
                textRun.setFontColor(Color.BLACK);
                textRun.setBold(false);

                lineCounter = getLineCounter(lineCounter);
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ppt.write(out);
                return out.toByteArray();
            }

        } catch (IOException e) {
            System.err.println("Error al generar PPTX: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al generar PPTX", e);
        }
    }

    private static int getLineCounter(int lineCounter) {
        lineCounter++;
        return lineCounter;
    }

    private String[] splitIntoLineBlocks(String text, int maxCharsPerLine) {
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();

        for (String word : words) {
            if (line.length() + word.length() + 1 > maxCharsPerLine) {
                lines.add(line.toString().trim());
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }

        if (!line.isEmpty()) {
            lines.add(line.toString().trim());
        }

        return lines.toArray(new String[0]);
    }
}
