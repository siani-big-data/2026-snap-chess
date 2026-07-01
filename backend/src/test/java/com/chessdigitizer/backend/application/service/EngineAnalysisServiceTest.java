package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.domain.exception.IllegalPositionException;
import com.chessdigitizer.backend.domain.model.*;
import com.chessdigitizer.backend.domain.port.out.BookRepository;
import com.chessdigitizer.backend.domain.port.out.EngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EngineAnalysisServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private EngineService engineService;

    private EngineAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new EngineAnalysisService(bookRepository, engineService, new FenLegalityValidator());
    }

    private ChessFile buildChessFileWithBoard(UUID bookId, String boardId, Fen fen) {
        UUID ownerId = UUID.randomUUID();
        ChessBoard board = new ChessBoard(boardId, 1, new BoundingBox(0, 0, 10, 10), fen,
                new AnalysisNode(null, "", null));
        return new ChessFile(bookId, "Título", "f.pdf", 1, BookCategory.GENERAL, ownerId, List.of(board));
    }

    @Test
    void analyze_happyPath_returnsEngineResultAndPersistsEval() {
        UUID bookId = UUID.randomUUID();
        String boardId = "board-p1-1";
        Fen fen = new Fen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        ChessFile chessFile = buildChessFileWithBoard(bookId, boardId, fen);
        EngineAnalysis expectedAnalysis = EngineAnalysis.of(45, "e1e2");

        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(chessFile));
        when(engineService.analyze(fen, 1000)).thenReturn(expectedAnalysis);

        EngineAnalysis result = service.analyze(bookId, boardId, 1000);

        assertEquals(45, result.evalCp(),
                "La evaluación devuelta debe ser la del motor: " + result.evalCp());
        assertEquals("e1e2", result.bestMove());
        verify(bookRepository).updateBoardEval(bookId, boardId, 45);
    }

    @Test
    void analyze_throwsIllegalArgument_whenBookNotFound() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.analyze(bookId, "board-1", 1000),
                "Libro no encontrado debe lanzar IllegalArgumentException");
        verify(engineService, never()).analyze(any(), anyInt());
    }

    @Test
    void analyze_throwsIllegalArgument_whenBoardNotFound() {
        UUID bookId = UUID.randomUUID();
        ChessFile chessFile = buildChessFileWithBoard(bookId, "board-p1-1",
                new Fen("4k3/8/8/8/8/8/8/4K3 w - - 0 1"));
        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(chessFile));

        assertThrows(IllegalArgumentException.class,
                () -> service.analyze(bookId, "board-inexistente", 1000),
                "Tablero no encontrado debe lanzar IllegalArgumentException");
        verify(engineService, never()).analyze(any(), anyInt());
    }

    @Test
    void analyze_throwsIllegalPositionException_whenFenIsIllegal() {
        UUID bookId = UUID.randomUUID();
        String boardId = "board-p1-1";
        // Dos reyes blancos: posición estructuralmente válida pero ajedrecísticamente imposible
        Fen illegalFen = new Fen("4k3/8/8/8/8/8/8/3KK3 w - - 0 1");
        ChessFile chessFile = buildChessFileWithBoard(bookId, boardId, illegalFen);

        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(chessFile));

        assertThrows(IllegalPositionException.class,
                () -> service.analyze(bookId, boardId, 1000),
                "Una posición ilegal debe lanzar IllegalPositionException antes de invocar al motor");
        verify(engineService, never()).analyze(any(), anyInt());
        verify(bookRepository, never()).updateBoardEval(any(), any(), anyInt());
    }
}
