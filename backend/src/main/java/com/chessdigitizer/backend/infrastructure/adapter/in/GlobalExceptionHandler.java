package com.chessdigitizer.backend.infrastructure.adapter.in;

import com.chessdigitizer.backend.domain.exception.IllegalPositionException;
import com.chessdigitizer.backend.domain.exception.UnauthenticatedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Void> handleUnauthenticated(UnauthenticatedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(IllegalPositionException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalPosition(IllegalPositionException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "ILLEGAL_POSITION");
        body.put("message", "La posición detectada no es una posición de ajedrez válida");
        body.put("details", ex.errors());
        return ResponseEntity.unprocessableEntity().body(body);
    }
}