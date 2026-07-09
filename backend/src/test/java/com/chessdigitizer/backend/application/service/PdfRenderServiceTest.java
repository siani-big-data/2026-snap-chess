package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.application.config.GlobalProperties;
import com.chessdigitizer.backend.domain.port.out.PdfRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfRenderServiceTest {

    @Mock
    private PdfRenderer pdfRenderer;

    private GlobalProperties.RenderProperties renderProperties;
    private PdfRenderService pdfRenderService;

    @BeforeEach
    void setUp() {
        renderProperties = new GlobalProperties.RenderProperties();
        renderProperties.setDefaultDpi(150);
        pdfRenderService = new PdfRenderService(pdfRenderer, renderProperties);
    }

    @Test
    void renderPageDefault_usesConfiguredDpi() {
        UUID bookId = UUID.randomUUID();
        byte[] expected = {1, 2, 3};
        when(pdfRenderer.renderPage(bookId, 3, 150)).thenReturn(expected);

        byte[] actual = pdfRenderService.renderPageDefault(bookId, 3);

        assertArrayEquals(expected, actual);
        verify(pdfRenderer).renderPage(eq(bookId), eq(3), eq(150));
    }

    @Test
    void renderPage_forwardsDpi() {
        UUID bookId = UUID.randomUUID();
        byte[] expected = {9};
        when(pdfRenderer.renderPage(bookId, 1, 72)).thenReturn(expected);

        assertArrayEquals(expected, pdfRenderService.renderPage(bookId, 1, 72));
    }
}
