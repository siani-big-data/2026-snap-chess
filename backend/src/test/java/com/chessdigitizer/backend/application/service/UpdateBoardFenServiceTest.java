package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.domain.exception.IllegalPositionException;
import com.chessdigitizer.backend.domain.model.*;
import com.chessdigitizer.backend.domain.port.out.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateBoardFenServiceTest {

    @Mock private BookRepository bookRepository;

    private UpdateBoardFenService service;

    @BeforeEach
    void setUp() {
        // FenLegalityValidator instanciado sin mocks — es lógica de dominio pura
        service = new UpdateBoardFenService(bookRepository, new FenLegalityValidator());
    }

    @Test
    void updateFen_happyPath_persistsValidFen() {
        UUID bookId = UUID.randomUUID();
        String boardId = "board-p1-1";
        String validFen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1";

        service.updateFen(bookId, boardId, validFen);

        ArgumentCaptor<Fen> fenCaptor = ArgumentCaptor.forClass(Fen.class);
        verify(bookRepository).updateBoardFen(eq(bookId), eq(boardId), fenCaptor.capture());
        assertEquals(validFen, fenCaptor.getValue().value(),
                "El FEN persistido debe ser el mismo que el proporcionado");
    }

    @Test
    void updateFen_throwsIllegalArgumentException_whenFenIsStructurallyMalformed() {
        // FEN sin campo de turno — inválido estructuralmente
        assertThrows(IllegalArgumentException.class,
                () -> service.updateFen(UUID.randomUUID(), "board-1", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"),
                "Un FEN sin campo de turno debe ser rechazado antes de la validación de legalidad");
        verify(bookRepository, never()).updateBoardFen(any(), any(), any());
    }

    @Test
    void updateFen_throwsIllegalPositionException_whenPositionIsIllegal() {
        // Dos reyes blancos: estructura correcta pero posición imposible
        String illegalFen = "4k3/8/8/8/8/8/8/3KK3 w - - 0 1";

        var ex = assertThrows(IllegalPositionException.class,
                () -> service.updateFen(UUID.randomUUID(), "board-1", illegalFen),
                "Una posición con dos reyes blancos debe lanzar IllegalPositionException");
        assertFalse(ex.errors().isEmpty(),
                "La excepción debe incluir los errores de validación");
        verify(bookRepository, never()).updateBoardFen(any(), any(), any());
    }

    @Test
    void updateFen_throwsIllegalPositionException_withDescriptiveErrors() {
        // Peón en fila 8: verificamos que el mensaje sea informativo
        String fenWithPawnOnRank8 = "P3k3/8/8/8/8/8/8/4K3 w - - 0 1";

        var ex = assertThrows(IllegalPositionException.class,
                () -> service.updateFen(UUID.randomUUID(), "board-1", fenWithPawnOnRank8));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("primera o última fila")),
                "El error debe indicar que hay peones en fila 1/8, errores: " + ex.errors());
    }
}
