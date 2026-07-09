package com.chessdigitizer.backend.domain.model;

import java.util.regex.Pattern;

public record Fen(String value) {

    private static final Pattern PIECE_PLACEMENT_ROW =
            Pattern.compile("^[pnbrqkPNBRQK1-8]+$");

    public static final Fen STARTING_POSITION =
            new Fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

    public Fen {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Fen cannot be null or blank");
        }
        validateStructure(value);
    }

    private static void validateStructure(String value) {
        String[] fields = value.trim().split("\\s+");
        if (fields.length < 2) {
            throw new IllegalArgumentException(
                    "Fen mal formado: se esperaban al menos 2 campos (colocación y turno), se encontraron " + fields.length);
        }

        String[] rows = fields[0].split("/");
        if (rows.length != 8) {
            throw new IllegalArgumentException(
                    "Fen mal formado: se esperaban 8 filas separadas por '/', se encontraron " + rows.length);
        }

        for (int i = 0; i < rows.length; i++) {
            String row = rows[i];
            if (!PIECE_PLACEMENT_ROW.matcher(row).matches()) {
                throw new IllegalArgumentException(
                        "Fen mal formado: la fila " + (i + 1) + " contiene caracteres inválidos: '" + row + "'");
            }
            if (countColumns(row) != 8) {
                throw new IllegalArgumentException(
                        "Fen mal formado: la fila " + (i + 1) + " no suma 8 columnas: '" + row + "'");
            }
        }
    }

    private static int countColumns(String row) {
        int columns = 0;
        for (char c : row.toCharArray()) {
            columns += Character.isDigit(c) ? (c - '0') : 1;
        }
        return columns;
    }
}