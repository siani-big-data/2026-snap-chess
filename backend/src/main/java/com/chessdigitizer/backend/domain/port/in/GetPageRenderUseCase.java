package com.chessdigitizer.backend.domain.port.in;

import java.util.UUID;

public interface GetPageRenderUseCase {

    byte[] renderPage(UUID bookId, int pageNumber, int dpi);

    byte[] renderPageDefault(UUID bookId, int pageNumber);
}
