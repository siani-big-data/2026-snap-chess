package com.chessdigitizer.backend.infrastructure.adapter.in.response;

import com.chessdigitizer.backend.domain.model.EngineAnalysis;

public record EngineAnalysisResponse(
        int evalCp,
        String formattedEval,
        String bestMove
) {
    public static EngineAnalysisResponse fromDomain(EngineAnalysis analysis) {
        return new EngineAnalysisResponse(
                analysis.evalCp(),
                analysis.formattedEval(),
                analysis.bestMove()
        );
    }
}