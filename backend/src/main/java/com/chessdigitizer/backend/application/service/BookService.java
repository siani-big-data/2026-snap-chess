package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.domain.model.Book;
import com.chessdigitizer.backend.domain.model.BookCategory;
import com.chessdigitizer.backend.domain.model.ChessFile;
import com.chessdigitizer.backend.domain.port.in.LoadBookUseCase;
import com.chessdigitizer.backend.domain.port.out.BookRepository;
import com.chessdigitizer.backend.application.config.GlobalProperties.StorageProperties;
import com.chessdigitizer.backend.domain.port.out.CurrentUserPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class BookService implements LoadBookUseCase {


    BookRepository bookRepository;
    StorageProperties storageProperties;
    private final CurrentUserPort currentUserPort;


    public BookService(BookRepository bookRepository, StorageProperties storageProperties, CurrentUserPort currentUserPort) {
        this.bookRepository = bookRepository;
        this.storageProperties = storageProperties;
        this.currentUserPort = currentUserPort;

    }

    @Override
    public Book importBook(byte[] fileBytes, String originalFilename, String title) {
        UUID ownerId = currentUserPort.getCurrentUserId();
        UUID uuid = UUID.randomUUID();
        Path path = Paths.get(storageProperties.getBooksPath(), ownerId.toString(), uuid + ".pdf");
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, fileBytes, StandardOpenOption.CREATE_NEW);
            int numberOfPages;
            try (PDDocument doc = Loader.loadPDF(path.toFile())) {
                numberOfPages = doc.getNumberOfPages();
            }
            Book book = new Book(uuid, title, originalFilename, numberOfPages, BookCategory.GENERAL, ownerId);
            bookRepository.save(book);
            log.info("Libro importado: {} with id {}", title, uuid);
            return book;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Book> getBook(UUID bookId) {
       return bookRepository.findById(bookId);
    }

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public void deleteBook(UUID bookId) {
        UUID ownerId = currentUserPort.getCurrentUserId();
        Path path = Paths.get(storageProperties.getBooksPath(), ownerId.toString(), bookId + ".pdf");
        try {
            Files.deleteIfExists(path);
            bookRepository.deleteById(bookId);
            log.info("Libro eliminado: con id {}", bookId);
        } catch (IOException e) {
            throw new RuntimeException("Error eliminando Libro: " + bookId, e);
        }
    }

    @Override
    public Optional<ChessFile> getChessFile(UUID chessFileId) {
        return bookRepository.findChessFileById(chessFileId);
    }

    @Override
    public Book renameBook(UUID id, String newTitle) {
        bookRepository.updateTitle(id, newTitle);
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found: " + id));
    }

    @Override
    public Book updateCategory(UUID id, BookCategory category) {
        bookRepository.updateCategory(id, category);
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found: " + id));
    }
}
