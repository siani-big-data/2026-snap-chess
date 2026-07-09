package com.chessdigitizer.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FenValidationResultTest {

    @Test
    void ok_producesValidResultWithNoErrors() {
        FenValidationResult result = FenValidationResult.ok();
        assertTrue(result.valid(), "FenValidationResult.ok() debe ser válido");
        assertTrue(result.errors().isEmpty(),
                "FenValidationResult.ok() no debe tener errores, tuvo: " + result.errors());
    }

    @Test
    void invalid_producesInvalidResultWithErrors() {
        List<String> errors = List.of("Error A", "Error B");
        FenValidationResult result = FenValidationResult.invalid(errors);
        assertFalse(result.valid(), "FenValidationResult.invalid() debe ser inválido");
        assertEquals(2, result.errors().size(),
                "Deben conservarse los 2 errores, se encontraron: " + result.errors().size());
    }

    @Test
    void invalid_throwsWhenNoErrorsProvided() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> FenValidationResult.invalid(List.of()));
        assertTrue(ex.getMessage().contains("al menos un error"),
                "El mensaje debería indicar que se necesita al menos un error, fue: " + ex.getMessage());
    }

    @Test
    void constructor_throwsWhenValidTrueButHasErrors() {
        // Invariante: un resultado marcado como válido no puede contener errores
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new FenValidationResult(true, List.of("no debería existir")));
        assertTrue(ex.getMessage().contains("válido"),
                "El mensaje debería mencionar que un resultado válido no puede tener errores, fue: " + ex.getMessage());
    }

    @Test
    void errors_returnedListIsImmutable() {
        FenValidationResult result = FenValidationResult.invalid(List.of("error"));
        assertThrows(UnsupportedOperationException.class,
                () -> result.errors().add("modificación"),
                "La lista de errores debe ser inmutable");
    }
}
