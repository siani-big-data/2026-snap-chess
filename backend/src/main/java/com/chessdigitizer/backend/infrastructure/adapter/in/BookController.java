package com.chessdigitizer.backend.infrastructure.adapter.in;


import com.chessdigitizer.backend.domain.model.Book;
import com.chessdigitizer.backend.domain.port.in.LoadBookUseCase;
import com.chessdigitizer.backend.infrastructure.adapter.in.response.ChessFileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@Slf4j
public class BookController {

    private final LoadBookUseCase loadBookUseCase;

    public BookController(LoadBookUseCase loadBookUseCase) {
        this.loadBookUseCase = loadBookUseCase;
    }


    @PostMapping("")
    public ResponseEntity<Book> importBook(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title) {
        try{
            byte[] bytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            Book book = loadBookUseCase.importBook(bytes, originalFilename, title);
            return ResponseEntity.status(HttpStatus.CREATED).body(book);
        } catch (IOException e) {
            log.error("Error al leer el archivo de entrada", e);
            throw new RuntimeException(e);
        }
    }

    @GetMapping("")
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(loadBookUseCase.getAllBooks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBook(@PathVariable UUID id) {
        Optional<Book> book = loadBookUseCase.getBook(id);
        return book.isPresent() ? ResponseEntity.ok(book.get()) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        loadBookUseCase.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/chess")
    public ResponseEntity<ChessFileResponse> getChessFile(@PathVariable UUID id) {
        return loadBookUseCase.getChessFile(id)
                .map(ChessFileResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/title")
    public ResponseEntity<Book> updateBookTitle(
            @PathVariable UUID id,
            @RequestBody RenameBookRequest request) {
        return ResponseEntity.ok(loadBookUseCase.renameBook(id, request.title()));
    }

    public record RenameBookRequest(String title) {}


}
