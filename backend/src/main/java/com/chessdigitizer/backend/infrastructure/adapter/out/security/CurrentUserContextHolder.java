package com.chessdigitizer.backend.infrastructure.adapter.out.security;

import java.util.Optional;
import java.util.UUID;

public final class CurrentUserContextHolder {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContextHolder() {}

    public static void set(UUID userId) {
        CURRENT_USER.set(userId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}