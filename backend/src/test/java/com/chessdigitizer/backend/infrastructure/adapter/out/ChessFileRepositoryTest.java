package com.chessdigitizer.backend.infrastructure.adapter.out;

import com.chessdigitizer.backend.domain.model.Book;
import com.chessdigitizer.backend.infrastructure.config.GlobalProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@SpringBootTest
class ChessFileRepositoryTest {

    @Autowired
    private ChessFileRepository repository;

    @Autowired
    private GlobalProperties.StorageProperties storageProperties;

    @Test
    void save_shouldCreatePhysicalFile() throws IOException {
        // 1. Preparar datos de prueba
        UUID bookId = UUID.randomUUID();
        Book book = new Book(bookId, "Test Book", "test.pdf", 100);

        // 2. Ejecutar
        repository.save(book);

        // 3. Verificar que el archivo existe físicamente
        Path expectedPath = Paths.get(storageProperties.getChessPath(), bookId + ".chess");
        assertTrue("El archivo .chess debería existir en el disco", Files.exists(expectedPath));

        // 4. Limpieza (opcional)
        // repository.deleteById(bookId);
    }

    @Test
    void findAll_shouldReturnListOfBooks() {
        // Al tener el @PostConstruct en AppConfig, las carpetas ya existen.
        // Podrías guardar 2 libros y verificar que findAll() devuelve una lista de tamaño 2.
        List<Book> books = repository.findAll();
        assertNotNull(books);
    }
}