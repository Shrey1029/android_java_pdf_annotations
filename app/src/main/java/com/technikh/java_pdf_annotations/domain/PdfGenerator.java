package com.technikh.java_pdf_annotations.domain;

import android.content.Context;

import androidx.annotation.NonNull;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Generates PDF documents with images and annotations.
 * Supports two annotation styles:
 * 1. Standard - Text embedded directly on the page
 * 2. Toggleable - Microsoft Office style clickable annotations
 */
public class PdfGenerator {

    /**
     * Initializes PDFBox resources
     */
    public PdfGenerator(Context context) {
        PDFBoxResourceLoader.init(context);
    }

    /**
     * Creates a text annotation with proper positioning and styling
     */
    @NonNull
    private static PDAnnotationText createAnnotation(String annotationText, float pageWidth, float pageHeight) {
        PDAnnotationText textAnnotation = new PDAnnotationText();
        textAnnotation.setContents(annotationText);

        // Position at the top of the page
        PDRectangle position = new PDRectangle();
        position.setLowerLeftX(pageWidth / 2 - 20);
        position.setLowerLeftY(pageHeight - 30);
        position.setUpperRightX(pageWidth / 2 + 20);
        position.setUpperRightY(pageHeight - 10);
        textAnnotation.setRectangle(position);

        // Set properties
        textAnnotation.setOpen(false); // Initially closed, shows only icon
        textAnnotation.setName("Comment"); // Appears as comment icon

        // Background color
        float[] yellowColor = new float[]{1f, 1f, 0.8f};
        PDColor color = new PDColor(yellowColor, PDDeviceRGB.INSTANCE);
        textAnnotation.setColor(color);

        return textAnnotation;
    }

    /**
     * Creates a PDF with standard text annotations directly drawn on the page
     */
    public void createPdfWithAnnotations(List<String> imagePaths, File outputFile, String annotationText) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (String imagePath : imagePaths) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                float pageHeight = page.getMediaBox().getHeight();
                float pageWidth = page.getMediaBox().getWidth();

                // Load and scale image
                PDImageXObject pdImage = PDImageXObject.createFromFile(imagePath, document);
                float originalWidth = pdImage.getWidth();
                float originalHeight = pdImage.getHeight();
                float scaleFactor = pageWidth / originalWidth;
                float imageHeight = originalHeight * scaleFactor;

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    // Draw image
                    contentStream.drawImage(pdImage, 0, pageHeight - imageHeight - 40, pageWidth, imageHeight);

                    // Draw text annotation
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);

                    // Center the text
                    float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(annotationText) / 1000 * 12;
                    float textX = (pageWidth - textWidth) / 2;
                    float textY = pageHeight - 30;

                    contentStream.newLineAtOffset(textX, textY);
                    contentStream.showText(annotationText);
                    contentStream.endText();
                }
            }

            document.save(outputFile);
        }
    }

    /**
     * Creates a PDF with toggleable Microsoft Office style annotations
     * Annotations appear as comment icons that can be clicked to show/hide text
     */
    public void createPdfWithToggleableAnnotations(List<String> imagePaths, File outputFile, String annotationText) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (String imagePath : imagePaths) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                float pageHeight = page.getMediaBox().getHeight();
                float pageWidth = page.getMediaBox().getWidth();

                // Load and scale image
                PDImageXObject pdImage = PDImageXObject.createFromFile(imagePath, document);
                float originalWidth = pdImage.getWidth();
                float originalHeight = pdImage.getHeight();
                float scaleFactor = pageWidth / originalWidth;
                float imageHeight = originalHeight * scaleFactor;

                // Draw only the image
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, 0, pageHeight - imageHeight - 40, pageWidth, imageHeight);
                }

                // Add clickable annotation
                PDAnnotationText textAnnotation = createAnnotation(annotationText, pageWidth, pageHeight);
                List<PDAnnotation> annotations = page.getAnnotations();
                annotations.add(textAnnotation);
                page.setAnnotations(annotations);
            }

            document.save(outputFile);
        }
    }
}