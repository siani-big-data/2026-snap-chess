package com.chessdigitizer.backend.domain.model;

import java.util.List;

/**
 * Resultado de validar la legalidad de una posición de ajedrez representada en FEN.
 * Inmutable: una vez construido, no puede pasar de inválido a válido ni viceversa.
 */
public record FenValidationResult(boolean valid, List<String> errors) {

    public FenValidationResult {
        errors = List.copyOf(errors);
        if (valid && !errors.isEmpty()) {
            throw new IllegalArgumentException("Un resultado válido no puede tener errores asociados");
        }
    }

    public static FenValidationResult ok() {
        return new FenValidationResult(true, List.of());
    }

    public static FenValidationResult invalid(List<String> errors) {
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("Un resultado inválido necesita al menos un error");
        }
        return new FenValidationResult(false, errors);
    }
}