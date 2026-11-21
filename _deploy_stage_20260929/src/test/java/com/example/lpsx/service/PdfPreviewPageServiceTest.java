package com.example.lpsx.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfPreviewPageServiceTest {
    @Test
    void appendsOneClaimPageWithoutChangingTheSource() throws Exception {
        byte[] source;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            source = output.toByteArray();
        }

        PdfPreviewPageService service = new PdfPreviewPageService();
        service.loadPromptImage();
        byte[] result = service.appendClaimPage(source);

        assertTrue(result.length > source.length);
        try (PDDocument document = Loader.loadPDF(result)) {
            assertEquals(2, document.getNumberOfPages());
        }
        try (PDDocument document = Loader.loadPDF(source)) {
            assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void guidePreviewKeepsOnlyFirstPageAndAddsClaimPage() throws Exception {
        byte[] source;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.addPage(new PDPage());
            document.addPage(new PDPage());
            document.save(output);
            source = output.toByteArray();
        }

        PdfPreviewPageService service = new PdfPreviewPageService();
        service.loadPromptImage();
        byte[] result = service.firstPageWithClaimPage(source);

        try (PDDocument document = Loader.loadPDF(result)) {
            assertEquals(2, document.getNumberOfPages());
        }
        try (PDDocument document = Loader.loadPDF(source)) {
            assertEquals(3, document.getNumberOfPages());
        }
    }
}
