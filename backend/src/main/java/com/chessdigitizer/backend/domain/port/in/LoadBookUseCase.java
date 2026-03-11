package com.chessdigitizer.backend.domain.port.in;

import com.chessdigitizer.backend.domain.model.Book;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadBookUseCase {
    Book importBook(byte[] fileBytes, String originalFilename, String title);
    Optional<Book> getBook(UUID bookId);
    List<Book> getAllBooks();
    void deleteBook(UUID bookId);
}
