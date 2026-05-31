package com.chessdigitizer.backend.domain.model;


public record EngineAnalysis(int evalCp, String bestMove) {

    public static EngineAnalysis of(int evalCp, String bestMove) {
        return new EngineAnalysis(evalCp, bestMove);
    }

    /** Convierte centipeones a peones para mostrar en UI (ej: 45 → "+0.45") */
    public String formattedEval() {
        double pawns = evalCp / 100.0;
        return pawns >= 0 ? String.format("+%.2f", pawns) : String.format("%.2f", pawns);
    }
}