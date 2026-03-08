package com.chessdigitizer.backend.domain.port.out;

import com.chessdigitizer.backend.domain.model.Book;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository {
    void save(Book book);
    Optional<Book> findById(UUID id);
    List<Book> findAll();
    void deleteById(UUID id);

}
