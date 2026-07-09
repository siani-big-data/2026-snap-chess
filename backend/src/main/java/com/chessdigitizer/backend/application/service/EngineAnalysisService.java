package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.domain.exception.IllegalPositionException;
import com.chessdigitizer.backend.domain.model.EngineAnalysis;
import com.chessdigitizer.backend.domain.model.Fen;
import com.chessdigitizer.backend.domain.model.FenLegalityValidator;
import com.chessdigitizer.backend.domain.model.FenValidationResult;
import com.chessdigitizer.backend.domain.port.in.AnalyzePositionUseCase;
import com.chessdigitizer.backend.domain.port.out.BookRepository;
import com.chessdigitizer.backend.domain.port.out.EngineService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EngineAnalysisService implements AnalyzePositionUseCase {

    private final BookRepository bookRepository;
    private final EngineService engineService;
    private final FenLegalityValidator fenLegalityValidator;

    public EngineAnalysisService(BookRepository bookRepository,
                                 EngineService engineService, FenLegalityValidator fenLegalityValidator) {
        this.bookRepository = bookRepository;
        this.engineService = engineService;
        this.fenLegalityValidator = fenLegalityValidator;
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

        FenValidationResult validation = fenLegalityValidator.validate(fen);
        if (!validation.valid()) {
            throw new IllegalPositionException(validation.errors());
        }

        EngineAnalysis result = engineService.analyze(fen, moveTimeMs);

        bookRepository.updateBoardEval(bookId, boardId, result.evalCp());

        return result;
    }
}