package com.chessdigitizer.backend.domain.port.out;

import com.chessdigitizer.backend.domain.model.EngineAnalysis;
import com.chessdigitizer.backend.domain.model.Fen;


public interface EngineService {

    EngineAnalysis analyze(Fen fen, int moveTimeMs);
}