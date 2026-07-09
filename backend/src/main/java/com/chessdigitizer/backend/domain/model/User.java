package com.chessdigitizer.backend.domain.model;

import java.util.UUID;

public record User(
        UUID id,
        String username,
        String passwordHash
) {
    public User {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("El hash de contraseña no puede estar vacío");
        }
    }
}