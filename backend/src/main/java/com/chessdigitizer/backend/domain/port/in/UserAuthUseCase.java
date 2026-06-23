package com.chessdigitizer.backend.domain.port.in;

import com.chessdigitizer.backend.domain.model.User;

public interface UserAuthUseCase {
    User register(String username, String rawPassword);
    String login(String username, String rawPassword);
}