package com.chessdigitizer.backend.domain.port.in;

import com.chessdigitizer.backend.domain.model.ChessFile;

import java.util.UUID;

public interface AnalyzeBoardUseCase {
    ChessFile analyzePage(UUID bookId, int pageNumber);
}
