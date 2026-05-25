package com.chessdigitizer.backend.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FenTest {

    @Test
    void constructor_rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Fen(null));
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
}
