package com.chessdigitizer.backend.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FenTest {

    @Test
    void constructor_rejectsNull() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new Fen(null));
        assertTrue(ex.getMessage().contains("null"), "El mensaje debería mencionar 'null', fue: " + ex.getMessage());
    }

    @Test
    void constructor_rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Fen("   "));
    }

    @Test
    void startingPosition_isValid() {
        assertNotNull(Fen.STARTING_POSITION);
        assertTrue(Fen.STARTING_POSITION.value().contains("rnbqkbnr"));
    }

    @Test
    void constructor_rejects_fenWithoutTurnField() {
        // Solo colocación de piezas, falta el campo de turno obligatorio
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new Fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"));
        assertTrue(ex.getMessage().contains("2 campos"),
                "El mensaje debería indicar que faltan campos, fue: " + ex.getMessage());
    }

    @Test
    void constructor_rejects_fenWithSevenRowsInsteadOfEight() {
        // Solo 7 filas separadas por '/'
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new Fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1"));
        assertTrue(ex.getMessage().contains("8 filas"),
                "El mensaje debería indicar que se esperaban 8 filas, fue: " + ex.getMessage());
    }

    @Test
    void constructor_rejects_rowWithInvalidCharacter() {
        // La fila 1 contiene 'x', que no es ni pieza ni dígito válido
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new Fen("rnbqkxnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
        assertTrue(ex.getMessage().contains("inválidos"),
                "El mensaje debería indicar caracteres inválidos, fue: " + ex.getMessage());
    }

    @Test
    void constructor_rejects_rowWithColumnCountDifferentFromEight() {
        // "RNBQKBN" solo cuenta 7 columnas (falta una pieza/número)
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new Fen("RNBQKBN/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
        assertTrue(ex.getMessage().contains("8 columnas"),
                "El mensaje debería indicar que no suma 8 columnas, fue: " + ex.getMessage());
    }

    @Test
    void constructor_rejects_rowWithColumnCountGreaterThanEight() {
        // "RNBQKBNRR" suma 9 columnas
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new Fen("RNBQKBNRR/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
        assertTrue(ex.getMessage().contains("8 columnas"),
                "El mensaje debería indicar que no suma 8 columnas, fue: " + ex.getMessage());
    }

    @Test
    void constructor_accepts_validMinimalFen() {
        // Posición con solo dos reyes — válida estructuralmente
        Fen fen = new Fen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        assertEquals("4k3/8/8/8/8/8/8/4K3 w - - 0 1", fen.value());
    }
}
