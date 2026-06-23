package com.chessdigitizer.backend.domain.model;

import java.util.UUID;

public record Book(
        UUID id,
        String title,
        String originalFilename,
        int totalPages,
        BookCategory category,
        UUID ownerId
) {
    public Book {
        if (category == null) {
            category = BookCategory.GENERAL;
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("Un libro siempre debe tener un propietario");
        }
    }
}
