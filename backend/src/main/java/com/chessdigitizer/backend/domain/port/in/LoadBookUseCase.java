package com.chessdigitizer.backend.domain.port.in;

import com.chessdigitizer.backend.domain.model.Book;
import com.chessdigitizer.backend.domain.model.BookCategory;
import com.chessdigitizer.backend.domain.model.ChessFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadBookUseCase {
    Book importBook(byte[] fileBytes, String originalFilename, String title);
    Optional<Book> getBook(UUID bookId);
    List<Book> getAllBooks();
    void deleteBook(UUID bookId);
    Optional<ChessFile> getChessFile(UUID chessFileId);
    Book renameBook(UUID id, String newTitle);
    Book updateCategory(UUID id, BookCategory category);
}
