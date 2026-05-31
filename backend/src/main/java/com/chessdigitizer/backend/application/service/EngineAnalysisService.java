package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.domain.model.EngineAnalysis;
import com.chessdigitizer.backend.domain.model.Fen;
import com.chessdigitizer.backend.domain.port.in.AnalyzePositionUseCase;
import com.chessdigitizer.backend.domain.port.out.BookRepository;
import com.chessdigitizer.backend.domain.port.out.EngineService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EngineAnalysisService implements AnalyzePositionUseCase {

    private final BookRepository bookRepository;
    private final EngineService engineService;

    public EngineAnalysisService(BookRepository bookRepository,
                                 EngineService engineService) {
        this.bookRepository = bookRepository;
        this.engineService = engineService;
    }

    @Override
    public EngineAnalysis analyze(UUID bookId, String boardId, int moveTimeMs) {
        Fen fen = bookRepository.findChessFileById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Libro no encontrado: " + bookId))
                .boards().stream()
                .filter(b -> b.id().equals(boardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tablero no encontrado: " + boardId))
                .fen();

        EngineAnalysis result = engineService.analyze(fen, moveTimeMs);

        bookRepository.updateBoardEval(bookId, boardId, result.evalCp());

        return result;
    }
}