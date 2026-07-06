package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.domain.model.Book;
import com.chessdigitizer.backend.application.config.GlobalProperties.StorageProperties;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookServiceIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private StorageProperties storageProperties;

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
    void importBook_shouldCreatePhysicalPdfAndSaveToRepository() throws IOException {
        // 1. Preparar datos (PDF mínimo para que PDFBox lo lea)
        byte[] dummyPdf = "%PDF-1.0\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj 3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj\ntrailer<</Root 1 0 R/Size 4>>\n%%EOF".getBytes();
        String title = "Libro Real";
        String originalName = "test.pdf";

        // 2. Ejecutar lógica real
        Book savedBook = bookService.importBook(dummyPdf, originalName, title);

        // 3. Verificar persistencia física (ruta: {booksPath}/{ownerId}/{bookId}.pdf)
        assertEquals(testOwnerId, savedBook.ownerId());
        Path expectedPath = Paths.get(
                storageProperties.getBooksPath(), testOwnerId.toString(), savedBook.id() + ".pdf");
        assertTrue(Files.exists(expectedPath), "El archivo PDF real debería existir en el disco");

        // 4. Verificar que se puede recuperar del repositorio real
        assertTrue(bookService.getBook(savedBook.id()).isPresent());

        // Limpieza: borrar el archivo creado para no llenar la carpeta de pruebas
        bookService.deleteBook(savedBook.id());
    }

    @Test
    void deleteBook_shouldRemoveFileFromDisk() throws IOException {
        // 1. Importar uno primero
        byte[] dummyPdf = "%PDF-1.0\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj 3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj\ntrailer<</Root 1 0 R/Size 4>>\n%%EOF".getBytes();
        Book book = bookService.importBook(dummyPdf, "delete-me.pdf", "Temp");
        Path path = Paths.get(storageProperties.getBooksPath(), testOwnerId.toString(), book.id() + ".pdf");

        // 2. Ejecutar borrado
        bookService.deleteBook(book.id());

        // 3. Verificar que ya no existe
        assertFalse(Files.exists(path), "El archivo debería haber sido borrado del disco");
        assertTrue(bookService.getBook(book.id()).isEmpty());
    }
}