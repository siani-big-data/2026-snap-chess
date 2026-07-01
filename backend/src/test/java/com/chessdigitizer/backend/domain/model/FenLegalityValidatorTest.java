package com.chessdigitizer.backend.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para FenLegalityValidator.
 * No usa mocks: instancia objetos reales de dominio.
 * Cada test protege un invariante ajedrecístico concreto.
 */
class FenLegalityValidatorTest {

    private FenLegalityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FenLegalityValidator();
    }

    // ────────────────────────────────────────────────────────────
    // Posiciones válidas
    // ────────────────────────────────────────────────────────────

    @Test
    void should_accept_startingPosition() {
        FenValidationResult result = validator.validate(Fen.STARTING_POSITION);
        assertTrue(result.valid(),
                "La posición inicial debe ser válida, errores: " + result.errors());
    }

    @Test
    void should_accept_validMidgamePosition() {
        // Posición de libro típica con piezas menores en el centro
        Fen fen = new Fen("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4");
        FenValidationResult result = validator.validate(fen);
        assertTrue(result.valid(),
                "Posición válida de apertura española rechazada con: " + result.errors());
    }

    @Test
    void should_accept_fenWithOnlyKings() {
        Fen fen = new Fen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertTrue(result.valid(),
                "Posición con solo dos reyes debe ser válida, errores: " + result.errors());
    }

    // ────────────────────────────────────────────────────────────
    // Reyes: cantidad incorrecta
    // ────────────────────────────────────────────────────────────

    @Test
    void should_rejectFen_when_noWhiteKing() {
        Fen fen = new Fen("4k3/8/8/8/8/8/8/8 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "Una posición sin rey blanco debería ser rechazada");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("rey blanco")),
                "El error debería mencionar 'rey blanco', errores: " + result.errors());
    }

    @Test
    void should_rejectFen_when_noBlackKing() {
        Fen fen = new Fen("8/8/8/8/8/8/8/4K3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "Una posición sin rey negro debería ser rechazada");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("rey negro")),
                "El error debería mencionar 'rey negro', errores: " + result.errors());
    }

    @Test
    void should_rejectFen_when_twoWhiteKings() {
        // Dos reyes blancos en e1 y d1
        Fen fen = new Fen("4k3/8/8/8/8/8/8/3KK3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "Dos reyes blancos deben ser rechazados");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("rey blanco")),
                "El error debería mencionar 'rey blanco', errores: " + result.errors());
    }

    @Test
    void should_rejectFen_when_twoBlackKings() {
        Fen fen = new Fen("3kk3/8/8/8/8/8/8/4K3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "Dos reyes negros deben ser rechazados");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("rey negro")),
                "El error debería mencionar 'rey negro', errores: " + result.errors());
    }

    // ────────────────────────────────────────────────────────────
    // Exceso de piezas
    // ────────────────────────────────────────────────────────────

    @Test
    void should_rejectFen_when_whiteHasSeventeenPieces() {
        // 16 peones blancos + rey blanco = 17 piezas (imposible)
        Fen fen = new Fen("4k3/8/8/PPPPPPPP/PPPPPPPP/8/8/PPPPKPPP w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "17 piezas blancas deben ser rechazadas");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("blancas")),
                "El error debería mencionar 'blancas', errores: " + result.errors());
    }

    @Test
    void should_rejectFen_when_whiteHasNinePawns() {
        // 9 peones blancos (imposible) + reyes
        Fen fen = new Fen("4k3/8/8/8/8/PPPPPPP1/PPPPPPPP/4K3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "9 peones blancos deben ser rechazados");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("peones") && e.contains("blancas")),
                "El error debería mencionar 'peones' y 'blancas', errores: " + result.errors());
    }

    // ────────────────────────────────────────────────────────────
    // Peones en filas imposibles
    // ────────────────────────────────────────────────────────────

    @Test
    void should_rejectFen_when_whitePawnOnRankEight() {
        // Peón blanco en la fila 8 (fila de coronación)
        Fen fen = new Fen("4kP2/8/8/8/8/8/8/4K3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "Peón en fila 8 debería ser rechazado");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("primera o última fila")),
                "El error debería indicar fila 1/8, errores: " + result.errors());
    }

    @Test
    void should_rejectFen_when_blackPawnOnRankOne() {
        // Peón negro en la fila 1
        Fen fen = new Fen("4k3/8/8/8/8/8/8/4Kp2 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "Peón negro en fila 1 debería ser rechazado");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("primera o última fila")),
                "El error debería indicar fila 1/8, errores: " + result.errors());
    }

    // ────────────────────────────────────────────────────────────
    // Reyes adyacentes
    // ────────────────────────────────────────────────────────────

    @Test
    void should_rejectFen_when_kingsAreAdjacent() {
        // Reyes en e1 y e2 — adyacentes verticalmente
        Fen fen = new Fen("8/8/8/8/8/8/4k3/4K3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "Reyes adyacentes deben ser rechazados");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("adyacentes")),
                "El error debería mencionar 'adyacentes', errores: " + result.errors());
    }

    @Test
    void should_rejectFen_when_kingsAreDiagonallyAdjacent() {
        // Reyes en e1 y f2 — adyacentes diagonalmente
        Fen fen = new Fen("8/8/8/8/8/8/5k2/4K3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "Reyes adyacentes en diagonal deben ser rechazados");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("adyacentes")),
                "El error debería mencionar 'adyacentes', errores: " + result.errors());
    }

    @Test
    void should_accept_kingsWithOneSquareBetween() {
        // Reyes en e1 y e3 — separados por una casilla, no adyacentes
        Fen fen = new Fen("8/8/8/8/8/4k3/8/4K3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertTrue(result.valid(),
                "Reyes separados por una casilla son válidos, errores: " + result.errors());
    }

    // ────────────────────────────────────────────────────────────
    // Jaque doble simultáneo
    // ────────────────────────────────────────────────────────────

    @Test
    void should_rejectFen_when_bothKingsInCheckSimultaneously() {
        // Torre blanca en e5 da jaque al rey negro en e8;
        // Torre negra en e4 da jaque al rey blanco en e1.
        // Ambos reyes en jaque = imposible (nunca puede ser turno de ambos a la vez).
        Fen fen = new Fen("4k3/8/8/4R3/4r3/8/8/4K3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "Jaque doble simultáneo debe ser rechazado");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("simultáneamente")),
                "El error debería mencionar 'simultáneamente', errores: " + result.errors());
    }

    @Test
    void should_accept_fenWhenOnlyOneKingIsInCheck() {
        // Solo el rey blanco está en jaque (torre negra en e4), negro no está en jaque
        Fen fen = new Fen("4k3/8/8/8/4r3/8/8/4K3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        // Un rey en jaque es perfectamente legal (el jugador que mueve está en jaque)
        assertTrue(result.valid(),
                "Un solo rey en jaque es posición legal, errores: " + result.errors());
    }

    // ────────────────────────────────────────────────────────────
    // Múltiples errores simultáneos
    // ────────────────────────────────────────────────────────────

    @Test
    void should_reportMultipleErrors_when_positionHasSeveralViolations() {
        // Dos reyes blancos + peón en fila 8: dos errores distintos
        Fen fen = new Fen("P3k3/8/8/8/8/8/8/3KK3 w - - 0 1");
        FenValidationResult result = validator.validate(fen);
        assertFalse(result.valid(), "Posición con múltiples errores debe ser rechazada");
        assertTrue(result.errors().size() >= 2,
                "Deberían reportarse al menos 2 errores, se reportaron: " + result.errors());
    }
}
