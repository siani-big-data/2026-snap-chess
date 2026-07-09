package com.chessdigitizer.backend.domain.port.in;

import com.chessdigitizer.backend.domain.model.EngineAnalysis;

import java.util.UUID;


public interface AnalyzePositionUseCase {

    EngineAnalysis analyze(UUID bookId, String boardId, int moveTimeMs);
}