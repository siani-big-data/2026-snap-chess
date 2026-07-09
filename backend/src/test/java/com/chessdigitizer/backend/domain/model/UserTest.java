package com.chessdigitizer.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void constructor_rejectsNullUsername() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new User(UUID.randomUUID(), null, "hashValido"));
        assertTrue(ex.getMessage().contains("usuario"),
                "El mensaje debería mencionar 'usuario', fue: " + ex.getMessage());
    }

    @Test
    void constructor_rejectsBlankUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> new User(UUID.randomUUID(), "  ", "hashValido"));
    }

    @Test
    void constructor_rejectsNullPasswordHash() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new User(UUID.randomUUID(), "alice", null));
        assertTrue(ex.getMessage().contains("contraseña"),
                "El mensaje debería mencionar 'contraseña', fue: " + ex.getMessage());
    }

    @Test
    void constructor_rejectsBlankPasswordHash() {
        assertThrows(IllegalArgumentException.class,
                () -> new User(UUID.randomUUID(), "alice", "   "));
    }

    @Test
    void constructor_acceptsValidUser() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "alice", "$2a$10$hashValido");
        assertEquals(id, user.id());
        assertEquals("alice", user.username());
        assertEquals("$2a$10$hashValido", user.passwordHash());
    }
}
