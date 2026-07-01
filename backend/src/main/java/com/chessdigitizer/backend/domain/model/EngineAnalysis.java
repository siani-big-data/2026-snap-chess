package com.chessdigitizer.backend.domain.model;

import java.util.Locale;

public record EngineAnalysis(int evalCp, String bestMove) {

    public static EngineAnalysis of(int evalCp, String bestMove) {
        return new EngineAnalysis(evalCp, bestMove);
    }

    /** Convierte centipeones a peones para mostrar en UI (ej: 45 → "+0.45").
     *  Usa Locale.ROOT para garantizar punto como separador decimal
     *  independientemente del locale del servidor. */
    public String formattedEval() {
        double pawns = evalCp / 100.0;
        return pawns >= 0
                ? String.format(Locale.ROOT, "+%.2f", pawns)
                : String.format(Locale.ROOT, "%.2f", pawns);
    }
}