package com.chessdigitizer.backend.infrastructure.adapter.out;

import com.chessdigitizer.backend.domain.model.*;
import com.chessdigitizer.backend.infrastructure.adapter.out.DTO.AnalysisNodeDTO;
import com.chessdigitizer.backend.infrastructure.adapter.out.DTO.BoundingBoxDTO;
import com.chessdigitizer.backend.infrastructure.adapter.out.DTO.ChessBoardDTO;
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

    @Override
    public Optional<ChessFile> findChessFileById(UUID chessFileId) {
        Path path = Paths.get(storageProperties.getChessPath(),chessFileId + ".chess");
        if (!Files.exists(path)) return Optional.empty();

        ChessFile chessFile = toChessFile(objectMapper.readValue(path, ChessFileDTO.class));
        return Optional.of(chessFile);
    }

    @Override
    public void updateTitle(UUID id, String newTitle) {
        Path path = Paths.get(storageProperties.getChessPath(), id + ".chess");
        if (!Files.exists(path)) return;
        ChessFileDTO chessFileDTO = objectMapper.readValue(path, ChessFileDTO.class);
        chessFileDTO.setTitle(newTitle);
        objectMapper.writeValue(path,chessFileDTO);

    }

    private ChessFile toChessFile(ChessFileDTO dto) {
        List<ChessBoard> boards = dto.getBoards().stream().map(this::toChessBoard).toList();

        return new ChessFile(
                dto.getId(),
                dto.getTitle(),
                dto.getOriginalFilename(),
                dto.getTotalPages(),
                boards
        );
    }

    private ChessBoard toChessBoard(ChessBoardDTO dto) {

        Fen fen = (dto.getFen() != null && !dto.getFen().isBlank())
                ? new Fen(dto.getFen())
                : Fen.STARTING_POSITION;

        BoundingBox bbox = dto.getBoundingBox() != null
                ? toBoundingBox(dto.getBoundingBox())
                : null;

        return new ChessBoard(
                dto.getId(),
                dto.getPage(),
                bbox,
                fen,
                toAnalysisNode(dto.getAnalysis())
        );
    }

    private AnalysisNode toAnalysisNode(AnalysisNodeDTO dto) {return null; }//Todo todavía no implementado

    private BoundingBox toBoundingBox(BoundingBoxDTO dto) {
        return new BoundingBox(
                dto.getX(),
                dto.getY(),
                dto.getWidth(),
                dto.getHeight()
        );
    }
}

