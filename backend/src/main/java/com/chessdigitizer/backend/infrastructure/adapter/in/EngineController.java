package com.chessdigitizer.backend.infrastructure.adapter.in;

import com.chessdigitizer.backend.domain.model.EngineAnalysis;
import com.chessdigitizer.backend.domain.model.Fen;
import com.chessdigitizer.backend.domain.port.in.AnalyzePositionUseCase;
import com.chessdigitizer.backend.domain.port.out.EngineService;
import com.chessdigitizer.backend.infrastructure.adapter.in.response.EngineAnalysisResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
public class EngineController {

    private final AnalyzePositionUseCase analyzePositionUseCase;
    private final EngineService engineService;


    public EngineController(AnalyzePositionUseCase analyzePositionUseCase, EngineService engineService) {
        this.analyzePositionUseCase = analyzePositionUseCase;
        this.engineService = engineService;
    }

    @PostMapping("/api/books/{bookId}/boards/{boardId}/engine/analyze")
    public ResponseEntity<EngineAnalysisResponse> analyze(
            @PathVariable UUID bookId,
            @PathVariable String boardId,
            @RequestParam(defaultValue = "1000") int moveTimeMs) {

        EngineAnalysis result = analyzePositionUseCase.analyze(bookId, boardId, moveTimeMs);
        return ResponseEntity.ok(EngineAnalysisResponse.fromDomain(result));
    }

    @PostMapping("/api/engine/analyze-fen")
    public ResponseEntity<EngineAnalysisResponse> analyzeFen(
            @RequestBody Map<String, String> body,
            @RequestParam(defaultValue = "1000") int moveTimeMs) {

        Fen fen = new Fen(body.get("fen"));
        EngineAnalysis result = engineService.analyze(fen, moveTimeMs);
        return ResponseEntity.ok(EngineAnalysisResponse.fromDomain(result));
    }
}