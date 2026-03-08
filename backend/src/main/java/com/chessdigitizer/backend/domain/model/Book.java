package com.chessdigitizer.backend.domain.model;

import java.util.UUID;

public record Book(
        UUID id,
        String title,
        String originalFilename,
        int totalPages
) {
}
