package com.chessdigitizer.backend.infrastructure.config;

import com.chessdigitizer.backend.domain.model.FenLegalityValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra como beans de Spring aquellos servicios de dominio que son POJOs puros
 * (sin @Component, sin dependencias de infraestructura), para que el dominio
 * permanezca ajeno al framework y la inyección se resuelva aquí.
 */
@Configuration
public class DomainConfig {

    @Bean
    public FenLegalityValidator fenLegalityValidator() {
        return new FenLegalityValidator();
    }
}