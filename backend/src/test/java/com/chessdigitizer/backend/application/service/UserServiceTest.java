package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.domain.exception.InvalidCredentialsException;
import com.chessdigitizer.backend.domain.exception.UsernameAlreadyExistsException;
import com.chessdigitizer.backend.domain.model.User;
import com.chessdigitizer.backend.domain.port.out.PasswordHasher;
import com.chessdigitizer.backend.domain.port.out.TokenIssuer;
import com.chessdigitizer.backend.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para UserService.
 * Mockea los puertos de salida (UserRepository, PasswordHasher, TokenIssuer),
 * no las implementaciones concretas.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private TokenIssuer tokenIssuer;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordHasher, tokenIssuer);
    }

    // ────────────────────────────────────────────────────────────
    // register
    // ────────────────────────────────────────────────────────────

    @Test
    void register_happyPath_hashesPasswordAndPersistsUser() {
        String rawPassword = "contraseñaSegura";
        String hash = "$2a$hash";
        UUID savedId = UUID.randomUUID();
        User savedUser = new User(savedId, "alice", hash);

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordHasher.hash(rawPassword)).thenReturn(hash);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.register("alice", rawPassword);

        assertEquals("alice", result.username(),
                "El usuario devuelto debe tener el username correcto");
        assertEquals(hash, result.passwordHash(),
                "El usuario devuelto debe tener el hash de la contraseña");
        verify(passwordHasher).hash(rawPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsUsernameAlreadyExists_whenUsernameIsTaken() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class,
                () -> userService.register("alice", "contraseñaSegura"),
                "Debe lanzar UsernameAlreadyExistsException si el username ya existe");
        verify(passwordHasher, never()).hash(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throwsIllegalArgument_whenPasswordIsTooShort() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register("alice", "corta"),
                "Contraseña de menos de 8 caracteres debe ser rechazada");
        assertTrue(ex.getMessage().contains("8 caracteres"),
                "El mensaje debe indicar el mínimo de 8 caracteres, fue: " + ex.getMessage());
        verify(userRepository, never()).existsByUsername(anyString());
    }

    @Test
    void register_throwsIllegalArgument_whenPasswordIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.register("alice", null),
                "Contraseña null debe ser rechazada");
    }

    @Test
    void register_acceptsPasswordOfExactlyEightCharacters() {
        String rawPassword = "12345678"; // exactamente 8 caracteres
        String hash = "$2a$hash";
        UUID savedId = UUID.randomUUID();
        User savedUser = new User(savedId, "bob", hash);

        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordHasher.hash(rawPassword)).thenReturn(hash);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        assertDoesNotThrow(() -> userService.register("bob", rawPassword),
                "Una contraseña de exactamente 8 caracteres debe ser aceptada");
    }

    // ────────────────────────────────────────────────────────────
    // login
    // ────────────────────────────────────────────────────────────

    @Test
    void login_happyPath_returnsToken() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "alice", "$2a$hash");
        String expectedToken = "token-xyz";

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("miPassword", "$2a$hash")).thenReturn(true);
        when(tokenIssuer.issueToken(userId)).thenReturn(expectedToken);

        String token = userService.login("alice", "miPassword");

        assertEquals(expectedToken, token,
                "El token devuelto debe coincidir con el emitido por TokenIssuer");
    }

    @Test
    void login_throwsInvalidCredentials_whenUsernameNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> userService.login("unknown", "cualquierPassword"),
                "Usuario no existente debe lanzar InvalidCredentialsException");
        verify(passwordHasher, never()).matches(anyString(), anyString());
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordDoesNotMatch() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "alice", "$2a$hash");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("passwordMal", "$2a$hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.login("alice", "passwordMal"),
                "Contraseña incorrecta debe lanzar InvalidCredentialsException");
        verify(tokenIssuer, never()).issueToken(any());
    }
}
