package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.application.config.GlobalProperties.RenderProperties;
import com.chessdigitizer.backend.domain.port.in.GetPageRenderUseCase;
import com.chessdigitizer.backend.domain.port.out.PdfRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Slf4j
public class PdfRenderService implements GetPageRenderUseCase {

    private final PdfRenderer pdfRenderer;
    private final RenderProperties renderProperties;

    public PdfRenderService(PdfRenderer pdfRenderer, RenderProperties renderProperties) {
        this.pdfRenderer = pdfRenderer;
        this.renderProperties = renderProperties;
    }

    @Override
    public byte[] renderPage(UUID bookId, int pageNumber, int dpi) {
        log.info("Rendering page {} for book {}", pageNumber, bookId);
        return pdfRenderer.renderPage(bookId, pageNumber, dpi);
    }

    @Override
    public byte[] renderPageDefault(UUID bookId, int pageNumber) {
        return renderPage(bookId, pageNumber, renderProperties.getDefaultDpi());
    }
}
