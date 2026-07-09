package com.chessdigitizer.backend.infrastructure.adapter.out;

import com.chessdigitizer.backend.domain.model.Book;
import com.chessdigitizer.backend.application.config.GlobalProperties;
import com.chessdigitizer.backend.domain.model.BookCategory;
import com.chessdigitizer.backend.infrastructure.adapter.out.security.CurrentUserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest
class ChessFileRepositoryTest {

    @Autowired
    private ChessFileRepository repository;

    @Autowired
    private GlobalProperties.StorageProperties storageProperties;

    private UUID testOwnerId;

    @BeforeEach
    void setUp() {
        testOwnerId = UUID.randomUUID();
        CurrentUserContextHolder.set(testOwnerId);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContextHolder.clear();
    }

    @Test
    void save_shouldCreatePhysicalFile() throws IOException {
        UUID bookId = UUID.randomUUID();
        Book book = new Book(bookId, "Test Book", "test.pdf", 100, BookCategory.GENERAL, testOwnerId);

        repository.save(book);

        Path expectedPath = Paths.get(storageProperties.getChessPath(), testOwnerId.toString(), bookId + ".chess");
        assertTrue(Files.exists(expectedPath), "El archivo .chess debería existir en el disco");

        repository.deleteById(bookId);
    }

    @Test
    void findAll_shouldReturnListOfBooks() throws IOException {
        UUID bookId = UUID.randomUUID();
        Book book = new Book(bookId, "Test Book", "test.pdf", 100, BookCategory.GENERAL, testOwnerId);
        repository.save(book);

        List<Book> books = repository.findAll();

        assertNotNull(books);
        assertTrue(books.stream().anyMatch(b -> b.id().equals(bookId)),
                "findAll debe devolver el libro guardado para el usuario actual");

        repository.deleteById(bookId);
    }
}