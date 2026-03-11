package com.chessdigitizer.backend.infrastructure.adapter.in;


import com.chessdigitizer.backend.domain.model.Book;
import com.chessdigitizer.backend.domain.port.in.LoadBookUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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


}
