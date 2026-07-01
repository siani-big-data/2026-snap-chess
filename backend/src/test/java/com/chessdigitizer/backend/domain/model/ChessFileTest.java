package com.chessdigitizer.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChessFileTest {

    @Test
    void constructor_throwsWhenOwnerIdIsNull() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new ChessFile(UUID.randomUUID(), "Título", "f.pdf", 5, BookCategory.GENERAL, null, List.of()));
        assertTrue(ex.getMessage().contains("propietario"),
                "El mensaje debería mencionar 'propietario', fue: " + ex.getMessage());
    }

    @Test
    void constructor_defaultsCategoryToGeneral_whenNullProvided() {
        ChessFile file = new ChessFile(
                UUID.randomUUID(), "Título", "f.pdf", 5, null, UUID.randomUUID(), List.of());
        assertEquals(BookCategory.GENERAL, file.category(),
                "Una categoría null debe normalizarse a GENERAL");
    }

    @Test
    void constructor_preservesExplicitCategory() {
        ChessFile file = new ChessFile(
                UUID.randomUUID(), "Título", "f.pdf", 5, BookCategory.FINALES, UUID.randomUUID(), List.of());
        assertEquals(BookCategory.FINALES, file.category(),
                "Una categoría explícita no debe ser sobreescrita");
    }

    @Test
    void constructor_acceptsEmptyBoardList() {
        ChessFile file = new ChessFile(
                UUID.randomUUID(), "Título", "f.pdf", 3, BookCategory.GENERAL, UUID.randomUUID(), List.of());
        assertTrue(file.boards().isEmpty(),
                "Un ChessFile sin tableros detectados es válido");
    }
}
