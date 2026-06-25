package com.chessdigitizer.backend.infrastructure.adapter.out.security;

import com.chessdigitizer.backend.domain.port.out.TokenIssuer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryTokenStore implements TokenIssuer {

    private final Map<String, UUID> activeTokens = new ConcurrentHashMap<>();

    @Override
    public String issueToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        activeTokens.put(token, userId);
        return token;
    }

    @Override
    public Optional<UUID> resolveUserId(String token) {
        return Optional.ofNullable(activeTokens.get(token));
    }

    @Override
    public void revokeToken(String token) {
        activeTokens.remove(token);
    }
}