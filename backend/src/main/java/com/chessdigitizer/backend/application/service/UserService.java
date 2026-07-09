package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.domain.exception.InvalidCredentialsException;
import com.chessdigitizer.backend.domain.exception.UsernameAlreadyExistsException;
import com.chessdigitizer.backend.domain.model.User;
import com.chessdigitizer.backend.domain.port.in.UserAuthUseCase;
import com.chessdigitizer.backend.domain.port.out.PasswordHasher;
import com.chessdigitizer.backend.domain.port.out.TokenIssuer;
import com.chessdigitizer.backend.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService implements UserAuthUseCase {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public User register(String username, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException("El nombre de usuario '" + username + "' ya está en uso");
        }

        String passwordHash = passwordHasher.hash(rawPassword);
        User user = new User(UUID.randomUUID(), username, passwordHash);
        return userRepository.save(user);
    }

    @Override
    public String login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario o contraseña incorrectos"));

        if (!passwordHasher.matches(rawPassword, user.passwordHash())) {
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }

        return tokenIssuer.issueToken(user.id());
    }
}