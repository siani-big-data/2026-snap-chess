package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.domain.model.*;
import com.chessdigitizer.backend.domain.port.out.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock private BookRepository bookRepository;

    private AnalysisService service;

    @BeforeEach
    void setUp() {
        service = new AnalysisService(bookRepository);
    }

    private ChessFile buildChessFileWithBoard(UUID bookId, String boardId, AnalysisNode root) {
        UUID ownerId = UUID.randomUUID();
        ChessBoard board = new ChessBoard(boardId, 1, new BoundingBox(0, 0, 10, 10),
                Fen.STARTING_POSITION, root);
        return new ChessFile(bookId, "Título", "f.pdf", 1, BookCategory.GENERAL, ownerId, List.of(board));
    }

    // ────────────────────────────────────────────────────────────
    // addMove
    // ────────────────────────────────────────────────────────────

    @Test
    void addMove_happyPath_addsChildToRoot_andPersistsTree() {
        UUID bookId = UUID.randomUUID();
        String boardId = "board-p1-1";
        AnalysisNode root = new AnalysisNode(null, "", null);
        ChessFile chessFile = buildChessFileWithBoard(bookId, boardId, root);

        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(chessFile));

        AnalysisNode result = service.addMove(bookId, boardId, List.of(), "e4");

        assertFalse(result.getChildren().isEmpty(),
                "Tras addMove la raíz debe tener al menos un hijo");
        assertTrue(result.findChild("e4").isPresent(),
                "El movimiento 'e4' debe estar como hijo de la raíz");
        verify(bookRepository).updateBoardAnalysis(eq(bookId), eq(boardId), any());
    }

    @Test
    void addMove_happyPath_navigatesExistingPathBeforeAdding() {
        UUID bookId = UUID.randomUUID();
        String boardId = "board-p1-1";
        AnalysisNode root = new AnalysisNode(null, "", null);
        root.addOrGetChild("e4").addOrGetChild("e5"); // preparamos árbol previo
        ChessFile chessFile = buildChessFileWithBoard(bookId, boardId, root);

        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(chessFile));

        service.addMove(bookId, boardId, List.of("e4", "e5"), "Nf3");

        AnalysisNode e5Node = root.findChild("e4").orElseThrow().findChild("e5").orElseThrow();
        assertTrue(e5Node.findChild("Nf3").isPresent(),
                "'Nf3' debe haberse añadido como hijo de 'e5'");
    }

    @Test
    void addMove_throwsIllegalArgument_whenBookNotFound() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.addMove(bookId, "board-1", List.of(), "e4"),
                "Libro no encontrado debe lanzar IllegalArgumentException");
    }

    @Test
    void addMove_throwsIllegalArgument_whenBoardNotFound() {
        UUID bookId = UUID.randomUUID();
        ChessFile chessFile = buildChessFileWithBoard(bookId, "board-p1-1",
                new AnalysisNode(null, "", null));
        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(chessFile));

        assertThrows(IllegalArgumentException.class,
                () -> service.addMove(bookId, "board-inexistente", List.of(), "e4"),
                "Tablero no encontrado debe lanzar IllegalArgumentException");
    }

    @Test
    void addMove_throwsIllegalArgument_whenPathStepIsNotInTree() {
        UUID bookId = UUID.randomUUID();
        String boardId = "board-p1-1";
        AnalysisNode root = new AnalysisNode(null, "", null);
        ChessFile chessFile = buildChessFileWithBoard(bookId, boardId, root);

        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(chessFile));

        // Path ["e4"] no existe en el árbol vacío
        assertThrows(IllegalArgumentException.class,
                () -> service.addMove(bookId, boardId, List.of("e4"), "e5"),
                "Un path que no existe en el árbol debe lanzar IllegalArgumentException");
        verify(bookRepository, never()).updateBoardAnalysis(any(), any(), any());
    }

    // ────────────────────────────────────────────────────────────
    // updateComment
    // ────────────────────────────────────────────────────────────

    @Test
    void updateComment_happyPath_updatesCommentOnTargetNode() {
        UUID bookId = UUID.randomUUID();
        String boardId = "board-p1-1";
        AnalysisNode root = new AnalysisNode(null, "", null);
        root.addOrGetChild("e4");
        ChessFile chessFile = buildChessFileWithBoard(bookId, boardId, root);

        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(chessFile));

        service.updateComment(bookId, boardId, List.of("e4"), "Jugada central clásica");

        AnalysisNode e4Node = root.findChild("e4").orElseThrow();
        assertEquals("Jugada central clásica", e4Node.getComment(),
                "El comentario del nodo 'e4' debe haberse actualizado");
        verify(bookRepository).updateBoardAnalysis(eq(bookId), eq(boardId), any());
    }

    @Test
    void updateComment_happyPath_updatesCommentOnRoot_whenPathIsEmpty() {
        UUID bookId = UUID.randomUUID();
        String boardId = "board-p1-1";
        AnalysisNode root = new AnalysisNode(null, "comentario inicial", null);
        ChessFile chessFile = buildChessFileWithBoard(bookId, boardId, root);

        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(chessFile));

        service.updateComment(bookId, boardId, List.of(), "nuevo comentario de raíz");

        assertEquals("nuevo comentario de raíz", root.getComment(),
                "El comentario de la raíz (path vacío) debe haberse actualizado");
    }

    @Test
    void updateComment_throwsIllegalArgument_whenPathStepIsNotInTree() {
        UUID bookId = UUID.randomUUID();
        String boardId = "board-p1-1";
        AnalysisNode root = new AnalysisNode(null, "", null);
        ChessFile chessFile = buildChessFileWithBoard(bookId, boardId, root);

        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(chessFile));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateComment(bookId, boardId, List.of("e4"), "comentario"),
                "Un path inválido en updateComment debe lanzar IllegalArgumentException");
        verify(bookRepository, never()).updateBoardAnalysis(any(), any(), any());
    }

    // ────────────────────────────────────────────────────────────
    // Bug documentado: addMove llama addOrGetChild dos veces
    // ────────────────────────────────────────────────────────────

    @Test
    void addMove_callsAddOrGetChildTwice_documentedBehavior() {
        // NOTA: AnalysisService.addMove() llama target.addOrGetChild(move) en la línea 37
        // y de nuevo en la línea 39 (posible copiar-pegar). addOrGetChild es idempotente,
        // por lo que el comportamiento observable es correcto (no crea duplicados),
        // pero la llamada doble es innecesaria. Este test documenta que el resultado
        // final es correcto a pesar del bug de código redundante.
        UUID bookId = UUID.randomUUID();
        String boardId = "board-p1-1";
        AnalysisNode root = new AnalysisNode(null, "", null);
        ChessFile chessFile = buildChessFileWithBoard(bookId, boardId, root);

        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(chessFile));

        service.addMove(bookId, boardId, List.of(), "e4");

        assertEquals(1, root.getChildren().size(),
                "A pesar de la llamada doble a addOrGetChild, solo debe existir un hijo 'e4'");
    }
}
