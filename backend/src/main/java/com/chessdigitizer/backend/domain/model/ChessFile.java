package com.chessdigitizer.backend.domain.model;

import java.util.List;
import java.util.UUID;

public record ChessFile(

        UUID id,
        String title,
        String originalFilename,
        int totalPages,
        BookCategory category,
        UUID ownerId,
        List<ChessBoard> boards
        ) {
    public ChessFile {
        if (category == null) {
            category = BookCategory.GENERAL;
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("Un libro siempre debe tener un propietario");
        }
    }
}
