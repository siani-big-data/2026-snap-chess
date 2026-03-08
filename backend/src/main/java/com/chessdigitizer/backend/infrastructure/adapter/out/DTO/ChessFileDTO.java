package com.chessdigitizer.backend.infrastructure.adapter.out.DTO;

import com.chessdigitizer.backend.domain.model.Book;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChessFileDTO {

    private UUID id;
    private String title;
    private String originalFilename;
    private int totalPages;
    private List<ChessBoardDTO> boards = new ArrayList<>();

    public void updateFromDomain(Book book) {
        this.id = book.id();
        this.title = book.title();
        this.originalFilename = book.originalFilename();
        this.totalPages = book.totalPages();
    }

    public Book toBook() {
        return new Book(id, title, originalFilename, totalPages);
    }
}
