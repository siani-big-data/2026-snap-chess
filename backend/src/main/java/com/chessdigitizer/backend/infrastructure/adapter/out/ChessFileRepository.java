package com.chessdigitizer.backend.infrastructure.adapter.out;

import com.chessdigitizer.backend.domain.model.Book;
import com.chessdigitizer.backend.infrastructure.adapter.out.DTO.ChessFileDTO;
import com.chessdigitizer.backend.application.config.GlobalProperties.StorageProperties;
import com.chessdigitizer.backend.domain.port.out.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
@Slf4j
public class ChessFileRepository implements BookRepository {

    StorageProperties storageProperties;
    ObjectMapper objectMapper;
    public ChessFileRepository(StorageProperties storageProperties, ObjectMapper objectMapper) {
        this.storageProperties = storageProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(Book book) {
        ChessFileDTO chessFileDTO;

        Path path = Paths.get(storageProperties.getChessPath(),book.id().toString() + ".chess");
        if (!Files.exists(path)) {
            chessFileDTO = new ChessFileDTO();
        }else {
            chessFileDTO = objectMapper.readValue(path, ChessFileDTO.class);
        }

        chessFileDTO.updateFromDomain(book);
        objectMapper.writeValue(path,chessFileDTO);
        log.info("Archivo chess creado para el libro {}", book.id());
    }


    @Override
    public Optional<Book> findById(UUID id) {
        ChessFileDTO chessFileDTO;
        Path path = Paths.get(storageProperties.getChessPath(),id + ".chess");
        if (!Files.exists(path)) return Optional.empty();

        chessFileDTO = objectMapper.readValue(path, ChessFileDTO.class);
        return Optional.of(chessFileDTO.toBook());

    }

    @Override
    public List<Book> findAll() {
        // Se podrá hacer con una única Stream?
        try(Stream<Path> files = Files.list(Path.of(storageProperties.getChessPath()))){

            List<Book> books = new ArrayList<>();
            Stream<Path> chessFiles =  files.filter(file -> file.getFileName().toString().endsWith(".chess"));

            chessFiles.forEach(file -> {
                try {
                    books.add(objectMapper.readValue(file, ChessFileDTO.class).toBook());
                } catch (JacksonException e) {
                    throw new RuntimeException(e);
                }
            });

            return books;

        } catch (IOException e) {
            log.error(e.getMessage());
            return List.of();
        }
    }
    @Override
    public void deleteById(UUID id) {
        Path path = Paths.get(storageProperties.getChessPath(),id + ".chess");

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Archivo chess eliminado para el libro {}", id);
    }
}

