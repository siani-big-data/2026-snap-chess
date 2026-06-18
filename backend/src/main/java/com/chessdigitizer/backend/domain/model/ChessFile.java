package com.chessdigitizer.backend.domain.model;

import java.util.List;
import java.util.UUID;

public record ChessFile(

        UUID id,
        String title,
        String originalFilename,
        int totalPages,
        BookCategory category,
        List<ChessBoard> boards
        ) {
    public ChessFile {
        if (category == null) {
            category = BookCategory.GENERAL;
        }
    }
}
