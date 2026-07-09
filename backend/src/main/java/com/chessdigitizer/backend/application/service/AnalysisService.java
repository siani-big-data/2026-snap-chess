package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.domain.model.AnalysisNode;
import com.chessdigitizer.backend.domain.model.ChessBoard;
import com.chessdigitizer.backend.domain.model.ChessFile;
import com.chessdigitizer.backend.domain.port.in.AnalysisUseCase;
import com.chessdigitizer.backend.domain.port.out.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AnalysisService implements AnalysisUseCase {

    private final BookRepository bookRepository;

    public AnalysisService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public AnalysisNode addMove(UUID bookId, String boardId, List<String> path, String move) {

        ChessFile chessFile = bookRepository.findChessFileById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("ChessFile not found: " + bookId));

        ChessBoard board = chessFile.boards().stream()
                .filter(b -> b.id().equals(boardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId));

        AnalysisNode root = board.analysis();
        AnalysisNode target = navigateTo(root, path);
        target.addOrGetChild(move);

        bookRepository.updateBoardAnalysis(bookId, boardId, root);
        log.info("Jugada '{}' añadida en tablero '{}' path={}", move, boardId, path);

        return root;
    }

    @Override
    public AnalysisNode updateComment(UUID bookId, String boardId, List<String> path, String comment) {
        ChessFile chessFile = bookRepository.findChessFileById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("ChessFile not found: " + bookId));

        ChessBoard board = chessFile.boards().stream()
                .filter(b -> b.id().equals(boardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId));

        AnalysisNode root = board.analysis();
        AnalysisNode target = navigateTo(root, path);
        target.setComment(comment);

        bookRepository.updateBoardAnalysis(bookId, boardId, root);
        log.info("Comentario actualizado en tablero '{}' path={}", boardId, path);

        return root;
    }

    private AnalysisNode navigateTo(AnalysisNode root, List<String> path) {
        AnalysisNode current = root;
        for (String step : path) {
            current = current.findChild(step)
                    .orElseThrow(() -> new IllegalArgumentException("Jugada no encontrada en el árbol: " + step));
        }
        return current;
    }

}