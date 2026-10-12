package com.studentactivityhub.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;

import org.springframework.stereotype.Service;

@Service
public class ResumePdfService {

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 15;

    public byte[] generatePdf(String resumeText) {

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float y = page.getMediaBox().getHeight() - MARGIN;

            PDPageContentStream content =
                    new PDPageContentStream(document, page);

            String[] lines = resumeText.split("\\R");

            for (String originalLine : lines) {

                String line = originalLine.trim();

                if (line.isEmpty()) {
                    y -= 8;
                    continue;
                }

                line = line.replace("**", "")
                           .replace("###", "")
                           .replace("---", "");

                boolean heading = isHeading(line);

                if (y < MARGIN + 35) {
                    content.close();

                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);

                    content = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - MARGIN;
                }

                if (heading) {
                    y -= 5;

                    content.beginText();
                    content.setFont(
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                            12);
                    content.newLineAtOffset(MARGIN, y);
                    content.showText(cleanText(line.toUpperCase()));
                    content.endText();

                    y -= 8;

                    content.moveTo(MARGIN, y);
                    content.lineTo(pageWidth - MARGIN, y);
                    content.stroke();

                    y -= 12;
                } else {
                    String cleanedLine = cleanText(line);

                    boolean bullet = cleanedLine.startsWith("* ")
                            || cleanedLine.startsWith("- ")
                            || cleanedLine.startsWith("• ");

                    if (bullet) {
                        cleanedLine = "   • " + cleanedLine.substring(2);
                    }

                    Matcher matcher = Pattern
                            .compile(".{1,85}(?:\\s|$)|\\S+")
                            .matcher(cleanedLine);

                    while (matcher.find()) {

                        String part = matcher.group().trim();

                        if (part.isEmpty()) {
                            continue;
                        }

                        if (y < MARGIN) {
                            content.close();

                            page = new PDPage(PDRectangle.A4);
                            document.addPage(page);

                            content = new PDPageContentStream(document, page);
                            y = page.getMediaBox().getHeight() - MARGIN;
                        }

                        content.beginText();

                        content.setFont(
                                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                                10);

                        content.newLineAtOffset(MARGIN, y);
                        content.showText(part);
                        content.endText();

                        // Add a clickable annotation if this line is a URL.
                        addLinkIfPresent(document, page, part, y);

                        y -= LINE_HEIGHT;
                    }
                }
            }

            content.close();

            document.save(output);

            return output.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not generate the resume PDF.", e);
        }
    }

    private boolean isHeading(String line) {

        String text = line.replace("**", "").trim();

        return text.matches(
                "(?i)^(EDUCATION|TECHNICAL SKILLS|SOFT SKILLS|"
                + "EXPERIENCE|PROJECTS|ACHIEVEMENTS|CERTIFICATIONS|"
                + "POSITIONS OF RESPONSIBILITY|LANGUAGES|"
                + "CAREER OBJECTIVE|PROFESSIONAL SUMMARY)$");
    }

    private String cleanText(String text) {

        return text.replaceAll("[^\\x20-\\x7E]", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private void addLinkIfPresent(
            PDDocument document,
            PDPage page,
            String text,
            float y) throws IOException {

        Matcher matcher = Pattern.compile(
                "https?://\\S+").matcher(text);

        while (matcher.find()) {

            String url = matcher.group()
                    .replaceAll("[),.;]+$", "");

            float x = MARGIN + text.indexOf(url) * 5.2f;

            PDAnnotationLink link = new PDAnnotationLink();

            PDRectangle rectangle = new PDRectangle(
                    x,
                    y - 3,
                    Math.min(url.length() * 5.2f, 450),
                    14);

            link.setRectangle(rectangle);

            PDActionURI action = new PDActionURI();
            action.setURI(url);

            link.setAction(action);

            page.getAnnotations().add(link);
        }
    }
}