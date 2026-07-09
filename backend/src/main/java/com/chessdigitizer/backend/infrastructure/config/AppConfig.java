package com.chessdigitizer.backend.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import com.chessdigitizer.backend.application.config.GlobalProperties.StorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class AppConfig {

    private final StorageProperties storageProperties;
    public AppConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void initStorageDirectories() {
        try {
            Path booksPath = Paths.get(storageProperties.getBooksPath());
            Path chessPath = Paths.get(storageProperties.getChessPath());

            Files.createDirectories(booksPath);
            Files.createDirectories(chessPath);

            log.info("Storage directories ready");

        } catch (IOException e) {
            throw new RuntimeException("Error al inicializar directorios de almacenamiento", e);
        }
    }


}
