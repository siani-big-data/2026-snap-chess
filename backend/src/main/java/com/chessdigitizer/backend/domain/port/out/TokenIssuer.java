package com.chessdigitizer.backend.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface TokenIssuer {
    String issueToken(UUID userId);
    Optional<UUID> resolveUserId(String token);
    void revokeToken(String token);
}