package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.domain.exception.IllegalPositionException;
import com.chessdigitizer.backend.domain.model.Fen;
import com.chessdigitizer.backend.domain.model.FenLegalityValidator;
import com.chessdigitizer.backend.domain.model.FenValidationResult;
import com.chessdigitizer.backend.domain.port.in.UpdateBoardFenUseCase;
import com.chessdigitizer.backend.domain.port.out.BookRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UpdateBoardFenService implements UpdateBoardFenUseCase {

    private final BookRepository bookRepository;
    private final FenLegalityValidator fenLegalityValidator;

    public UpdateBoardFenService(BookRepository bookRepository, FenLegalityValidator fenLegalityValidator) {
        this.bookRepository = bookRepository;
        this.fenLegalityValidator = fenLegalityValidator;
    }

    @Override
    public void updateFen(UUID bookId, String boardId, String fen) {
        Fen candidate = new Fen(fen); // valida estructura (lanza IllegalArgumentException si está mal formado)

        FenValidationResult validation = fenLegalityValidator.validate(candidate);
        if (!validation.valid()) {
            throw new IllegalPositionException(validation.errors());
        }

        bookRepository.updateBoardFen(bookId, boardId, candidate);
    }
}