package com.project.demo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfGeneratorService {

    private final String FONT_PATH = "fonts/OpenSans-Regular.ttf"; // Ruta relativa en resources

    public byte[] generatePdfFromText(String text, String filename) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 50;
            float yStart = page.getMediaBox().getHeight() - margin;
            float xStart = margin;
            float width = page.getMediaBox().getWidth() - 2 * margin;
            float fontSize = 12;
            float leading = 14.5f;
            float currentY = yStart;

            // Cargar fuente personalizada
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream(FONT_PATH);
            if (fontStream == null) {
                throw new IOException("No se pudo cargar la fuente desde: " + FONT_PATH);
            }
            PDFont font = PDType0Font.load(document, fontStream);

            PDPageContentStream contentStream = null;

            try {
                contentStream = new PDPageContentStream(document, page);
                contentStream.setFont(font, fontSize);
                contentStream.setLeading(leading);

                contentStream.beginText();
                contentStream.newLineAtOffset(xStart, currentY);
                String[] paragraphs = text.split("\n");

                for (String paragraph : paragraphs) {
                    List<String> wrappedLines = splitTextIntoLines(paragraph, font, fontSize, width);

                    for (String lineToDraw : wrappedLines) {
                        if (currentY - leading < margin) {
                            contentStream.endText();
                            contentStream.close();

                            page = new PDPage(PDRectangle.A4);
                            document.addPage(page);
                            contentStream = new PDPageContentStream(document, page);
                            contentStream.setFont(font, fontSize);
                            contentStream.setLeading(leading);
                            contentStream.beginText();
                            currentY = yStart;
                            contentStream.newLineAtOffset(xStart, currentY);
                        }

                        contentStream.showText(lineToDraw);
                        contentStream.newLine();
                        currentY -= leading;
                    }

                    if (currentY - leading < margin && wrappedLines.size() > 0) {
                        contentStream.endText();
                        contentStream.close();

                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.setFont(font, fontSize);
                        contentStream.setLeading(leading);
                        contentStream.beginText();
                        currentY = yStart;
                        contentStream.newLineAtOffset(xStart, currentY);
                    }
                    currentY -= leading;
                }

                contentStream.endText();

            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private List<String> splitTextIntoLines(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float testLineWidth = font.getStringWidth(testLine) / 1000 * fontSize;

            if (testLineWidth > maxWidth) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                    currentLine = new StringBuilder();
                }
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}
