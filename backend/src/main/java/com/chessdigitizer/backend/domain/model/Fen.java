package com.chessdigitizer.backend.domain.model;

public record Fen(String value) {

    public static final Fen STARTING_POSITION =
            new Fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

    public Fen {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Fen cannot be null or blank");
        }
    }
}