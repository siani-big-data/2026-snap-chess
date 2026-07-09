package com.chessdigitizer.backend.infrastructure.adapter.out.security;

import com.chessdigitizer.backend.domain.exception.UnauthenticatedException;
import com.chessdigitizer.backend.domain.port.out.CurrentUserPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserAdapter implements CurrentUserPort {

    @Override
    public UUID getCurrentUserId() {
        return CurrentUserContextHolder.get()
                .orElseThrow(() -> new UnauthenticatedException(
                        "No hay un usuario autenticado en esta petición"));
    }
}