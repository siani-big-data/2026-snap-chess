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
import java.util.stream.Collectors;
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

    @Override
    public void saveChessFile(ChessFile chessFile) {
        Path path = Paths.get(storageProperties.getChessPath(), chessFile.id() + ".chess");
        ChessFileDTO dto = toChessFileDTO(chessFile);
        objectMapper.writeValue(path, dto);
        log.info("ChessFile guardado para el libro {}", chessFile.id());
    }

    @Override
    public void updateBoardAnalysis(UUID bookId, String boardId, AnalysisNode analysis) {
        Path path = Paths.get(storageProperties.getChessPath(), bookId + ".chess");
        ChessFileDTO dto = objectMapper.readValue(path, ChessFileDTO.class);

        dto.getBoards().stream()
                .filter(b -> b.getId().equals(boardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId))
                .setAnalysis(toAnalysisNodeDTO(analysis));

        objectMapper.writeValue(path, dto);
        log.info("Análisis actualizado para tablero '{}' del libro '{}'", boardId, bookId);
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

    private AnalysisNode toAnalysisNode(AnalysisNodeDTO dto) {
        if (dto == null) return new AnalysisNode(null, "", null);

        AnalysisNode node = new AnalysisNode(dto.getMove(), dto.getComment(), dto.getEvalCp());

        if (dto.getChildren() != null) {
            dto.getChildren().stream()
                    .map(this::toAnalysisNode)
                    .forEach(node::addChild);
        }

        return node;
    }

    private BoundingBox toBoundingBox(BoundingBoxDTO dto) {
        return new BoundingBox(
                dto.getX(),
                dto.getY(),
                dto.getWidth(),
                dto.getHeight()
        );
    }
    private ChessFileDTO toChessFileDTO(ChessFile chessFile) {
        ChessFileDTO dto = new ChessFileDTO();
        dto.setId(chessFile.id());
        dto.setTitle(chessFile.title());
        dto.setOriginalFilename(chessFile.originalFilename());
        dto.setTotalPages(chessFile.totalPages());
        dto.setBoards(
                chessFile.boards().stream()
                        .map(this::toChessBoardDTO)
                        .collect(Collectors.toCollection(ArrayList::new))
        );
        return dto;
    }

    private ChessBoardDTO toChessBoardDTO(ChessBoard board) {
        ChessBoardDTO dto = new ChessBoardDTO();
        dto.setId(board.id());
        dto.setPage(board.page());
        dto.setFen(board.fen().value());
        dto.setBoundingBox(toBoundingBoxDTO(board.boundingBox()));
        dto.setAnalysis(toAnalysisNodeDTO(board.analysis()));
        return dto;
    }

    private BoundingBoxDTO toBoundingBoxDTO(BoundingBox bbox) {
        BoundingBoxDTO dto = new BoundingBoxDTO();
        dto.setX(bbox.x());
        dto.setY(bbox.y());
        dto.setWidth(bbox.width());
        dto.setHeight(bbox.height());
        return dto;
    }

    private AnalysisNodeDTO toAnalysisNodeDTO(AnalysisNode node) {
        if (node == null) return null;

        AnalysisNodeDTO dto = new AnalysisNodeDTO();
        dto.setMove(node.getMove());
        dto.setComment(node.getComment());
        dto.setEvalCp(node.getEvalCp());

        List<AnalysisNodeDTO> childrenDTOs = node.getChildren().stream()
                .map(this::toAnalysisNodeDTO)  // recursión
                .collect(Collectors.toCollection(ArrayList::new));

        dto.setChildren(childrenDTOs);
        return dto;
    }


}

