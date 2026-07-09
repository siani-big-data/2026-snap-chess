package com.chessdigitizer.backend.domain.exception;

import java.util.List;

/**
 * Se lanza cuando un Fen es estructuralmente correcto pero ajedrecísticamente
 * imposible (ej. dos reyes del mismo color, reyes adyacentes, jaque doble).
 * El dominio nunca debe invocar al motor de análisis con una posición en este estado.
 */
public class IllegalPositionException extends RuntimeException {

  private final List<String> errors;

  public IllegalPositionException(List<String> errors) {
    super("Posición ilegal: " + String.join("; ", errors));
    this.errors = List.copyOf(errors);
  }

  public List<String> errors() {
    return errors;
  }
}