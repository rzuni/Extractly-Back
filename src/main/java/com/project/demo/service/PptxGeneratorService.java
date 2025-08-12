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

    // Constantes para configuración de texto
    private static final int MAX_CHARS_PER_LINE = 90;
    private static final double LINE_SPACING = 10.0;
    private static final double DEFAULT_FONT_SIZE = 24.0;
    private static final int MAX_LINES_PER_SLIDE = 6;

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
                r.setFontSize(DEFAULT_FONT_SIZE);
                subtitleShape.setHorizontalCentered(true);
            }

            String[] lines;
            if (summaryText.contains("\n")) {
                lines = summaryText.split("\n");
            } else {
                lines = splitIntoLineBlocks(summaryText, MAX_CHARS_PER_LINE);
            }

            XSLFSlide currentSlide = null;
            XSLFTextShape currentTextBox = null;
            int lineCounter = 0;

            for (String line : lines) {
                if (currentSlide == null || lineCounter >= MAX_LINES_PER_SLIDE) {
                    currentSlide = ppt.createSlide(blankLayout);
                    currentTextBox = currentSlide.createTextBox();
                    currentTextBox.setAnchor(new Rectangle(100, 100, 1080, 520));
                    currentTextBox.setVerticalAlignment(VerticalAlignment.TOP);
                    lineCounter = 0;
                }

                XSLFTextParagraph paragraph = currentTextBox.addNewTextParagraph();
                paragraph.setTextAlign(TextParagraph.TextAlign.LEFT);
                paragraph.setSpaceAfter(LINE_SPACING);

                XSLFTextRun textRun = paragraph.addNewTextRun();
                textRun.setText(line.trim());
                textRun.setFontSize(DEFAULT_FONT_SIZE);
                textRun.setFontFamily("OpenSans");
                textRun.setFontColor(Color.BLACK);
                textRun.setBold(false);

                lineCounter++;
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