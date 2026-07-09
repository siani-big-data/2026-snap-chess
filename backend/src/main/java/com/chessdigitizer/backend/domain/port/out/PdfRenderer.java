package com.chessdigitizer.backend.domain.port.out;

import java.util.UUID;

public interface PdfRenderer {

    byte[] renderPage(UUID bookId, int pageNumber, int dpi);
}

