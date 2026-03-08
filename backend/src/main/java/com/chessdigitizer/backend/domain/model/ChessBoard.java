package com.chessdigitizer.backend.domain.model;

public record ChessBoard(
        String id,
        int page,
        BoundingBox boundingBox,
        Fen fen,
        AnalysisNode analysis

) {
}
