package com.chessdigitizer.backend.infrastructure.adapter.in;

import com.chessdigitizer.backend.domain.exception.InvalidCredentialsException;
import com.chessdigitizer.backend.domain.exception.UsernameAlreadyExistsException;
import com.chessdigitizer.backend.domain.model.User;
import com.chessdigitizer.backend.domain.port.in.UserAuthUseCase;
import com.chessdigitizer.backend.infrastructure.adapter.in.response.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAuthUseCase userAuthUseCase;

    public AuthController(UserAuthUseCase userAuthUseCase) {
        this.userAuthUseCase = userAuthUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody AuthRequest request) {
        try {
            User user = userAuthUseCase.register(request.username(), request.password());
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromDomain(user));
        } catch (UsernameAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody AuthRequest request) {
        try {
            String token = userAuthUseCase.login(request.username(), request.password());
            return ResponseEntity.ok(new TokenResponse(token));
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    public record AuthRequest(String username, String password) {}
    public record TokenResponse(String token) {}
}