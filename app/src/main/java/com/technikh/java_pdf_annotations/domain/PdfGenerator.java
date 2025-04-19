package com.technikh.java_pdf_annotations.domain;

import android.content.Context;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfGenerator {
    public PdfGenerator(Context context) {
        PDFBoxResourceLoader.init(context);
    }

    public void createPdfWithAnnotations(List<String> imagePaths, File outputFile, String annotationText) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (String imagePath : imagePaths) {

                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                float pageHeight = page.getMediaBox().getHeight();
                float pageWidth = page.getMediaBox().getWidth();

                // Add image
                PDImageXObject pdImage = PDImageXObject.createFromFile(imagePath, document);
                
                float originalWidth = pdImage.getWidth();
                float originalHeight = pdImage.getHeight();
                float scaleFactor = pageWidth / originalWidth;
                float imageHeight = originalHeight * scaleFactor;

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    
                    float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(annotationText) / 1000 * 12;
                    float textX = (pageWidth - textWidth) / 2;
                    float textY = pageHeight - 30;
                    
                    contentStream.newLineAtOffset(textX, textY);
                    contentStream.showText(annotationText);
                    contentStream.endText();

                    contentStream.drawImage(pdImage, 0, pageHeight - imageHeight - 40, pageWidth, imageHeight);
                }
            }

            document.save(outputFile);
        }
    }
} 