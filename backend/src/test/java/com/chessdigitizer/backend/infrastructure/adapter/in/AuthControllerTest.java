package com.chessdigitizer.backend.infrastructure.adapter.in;

import com.chessdigitizer.backend.domain.exception.InvalidCredentialsException;
import com.chessdigitizer.backend.domain.exception.UsernameAlreadyExistsException;
import com.chessdigitizer.backend.domain.model.User;
import com.chessdigitizer.backend.domain.port.in.UserAuthUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración web para AuthController usando MockMvcBuilders.standaloneSetup().
 * No usa @WebMvcTest (eliminado en Spring Boot 4.0). Instancia el controlador
 * directamente con un mock del caso de uso.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserAuthUseCase userAuthUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(userAuthUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ────────────────────────────────────────────────────────────
    // POST /api/auth/register
    // ────────────────────────────────────────────────────────────

    @Test
    void register_returns201_whenRegistrationSucceeds() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "alice", "$2a$hash");
        when(userAuthUseCase.register("alice", "contraseñaSegura")).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"contraseñaSegura"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void register_returns409_whenUsernameAlreadyExists() throws Exception {
        when(userAuthUseCase.register(eq("alice"), anyString()))
                .thenThrow(new UsernameAlreadyExistsException("El username 'alice' ya está en uso"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"contraseñaSegura"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns400_whenPasswordIsTooShort() throws Exception {
        when(userAuthUseCase.register(eq("alice"), anyString()))
                .thenThrow(new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"corta"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ────────────────────────────────────────────────────────────
    // POST /api/auth/login
    // ────────────────────────────────────────────────────────────

    @Test
    void login_returns200WithToken_whenCredentialsAreValid() throws Exception {
        when(userAuthUseCase.login("alice", "miPassword")).thenReturn("token-xyz");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"miPassword"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-xyz"));
    }

    @Test
    void login_returns401_whenCredentialsAreInvalid() throws Exception {
        when(userAuthUseCase.login(eq("alice"), anyString()))
                .thenThrow(new InvalidCredentialsException("Usuario o contraseña incorrectos"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"passwordMal"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
