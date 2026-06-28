package com.chessdigitizer.backend.infrastructure.adapter.out;

import com.chessdigitizer.backend.domain.model.*;
import com.chessdigitizer.backend.domain.port.out.CurrentUserPort;
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
    CurrentUserPort currentUserPort;

    public ChessFileRepository(StorageProperties storageProperties, ObjectMapper objectMapper, CurrentUserPort currentUserPort) {
        this.storageProperties = storageProperties;
        this.objectMapper = objectMapper;
        this.currentUserPort = currentUserPort;
    }

    @Override
    public void save(Book book) {
        Path path = resolveChessPath(book.ownerId(), book.id());
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ChessFileDTO chessFileDTO;
        if (!Files.exists(path)) {
            chessFileDTO = new ChessFileDTO();
        } else {
            chessFileDTO = objectMapper.readValue(path, ChessFileDTO.class);
        }

        chessFileDTO.updateFromDomain(book);
        objectMapper.writeValue(path, chessFileDTO);
        log.info("Archivo chess creado para el libro {}", book.id());
    }


    @Override
    public Optional<Book> findById(UUID id) {
        Path path = resolveChessPath(currentUserPort.getCurrentUserId(), id);
        if (!Files.exists(path)) return Optional.empty();
        return Optional.of(objectMapper.readValue(path, ChessFileDTO.class).toBook());
    }

    @Override
    public List<Book> findAll() {
        Path ownerDir = Paths.get(storageProperties.getChessPath(), currentUserPort.getCurrentUserId().toString());
        if (!Files.exists(ownerDir)) return List.of();

        try (Stream<Path> files = Files.list(ownerDir)) {
            List<Book> books = new ArrayList<>();
            files.filter(file -> file.getFileName().toString().endsWith(".chess"))
                    .forEach(file -> {
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
        Path path = resolveChessPath(currentUserPort.getCurrentUserId(), id);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Archivo chess eliminado para el libro {}", id);
    }

    @Override
    public Optional<ChessFile> findChessFileById(UUID chessFileId) {
        Path path = resolveChessPath(currentUserPort.getCurrentUserId(), chessFileId);
        if (!Files.exists(path)) return Optional.empty();
        return Optional.of(toChessFile(objectMapper.readValue(path, ChessFileDTO.class)));
    }

    @Override
    public void updateTitle(UUID id, String newTitle) {
        Path path = resolveChessPath(currentUserPort.getCurrentUserId(), id);
        if (!Files.exists(path)) return;
        ChessFileDTO chessFileDTO = objectMapper.readValue(path, ChessFileDTO.class);
        chessFileDTO.setTitle(newTitle);
        objectMapper.writeValue(path, chessFileDTO);
    }

    @Override
    public void updateCategory(UUID id, BookCategory category) {
        Path path = resolveChessPath(currentUserPort.getCurrentUserId(), id);
        if (!Files.exists(path)) return;
        ChessFileDTO chessFileDTO = objectMapper.readValue(path, ChessFileDTO.class);
        chessFileDTO.setCategory(category);
        objectMapper.writeValue(path, chessFileDTO);
    }

    @Override
    public void saveChessFile(ChessFile chessFile) {
        Path path = resolveChessPath(chessFile.ownerId(), chessFile.id());
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ChessFileDTO dto = toChessFileDTO(chessFile);
        objectMapper.writeValue(path, dto);
        log.info("ChessFile guardado para el libro {}", chessFile.id());
    }

    @Override
    public void updateBoardAnalysis(UUID bookId, String boardId, AnalysisNode analysis) {
        Path path = resolveChessPath(currentUserPort.getCurrentUserId(), bookId);
        ChessFileDTO dto = objectMapper.readValue(path, ChessFileDTO.class);
        dto.getBoards().stream()
                .filter(b -> b.getId().equals(boardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId))
                .setAnalysis(toAnalysisNodeDTO(analysis));
        objectMapper.writeValue(path, dto);
    }

    @Override
    public void updateBoardEval(UUID bookId, String boardId, int evalCp) {
        Path path = resolveChessPath(currentUserPort.getCurrentUserId(), bookId);
        ChessFileDTO dto = objectMapper.readValue(path, ChessFileDTO.class);
        ChessBoardDTO board = dto.getBoards().stream()
                .filter(b -> b.getId().equals(boardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId));
        if (board.getAnalysis() == null) board.setAnalysis(new AnalysisNodeDTO());
        board.getAnalysis().setEvalCp(evalCp);
        objectMapper.writeValue(path, dto);
    }

    @Override
    public void updateBoardFen(UUID bookId, String boardId, Fen fen) {
        Path path = resolveChessPath(currentUserPort.getCurrentUserId(), bookId);
        ChessFileDTO dto = objectMapper.readValue(path, ChessFileDTO.class);
        ChessBoardDTO board = dto.getBoards().stream()
                .filter(b -> b.getId().equals(boardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId));
        board.setFen(fen.value());
        objectMapper.writeValue(path, dto);
    }

    private ChessFile toChessFile(ChessFileDTO dto) {
        List<ChessBoard> boards = dto.getBoards().stream().map(this::toChessBoard).toList();

        return new ChessFile(
                dto.getId(),
                dto.getTitle(),
                dto.getOriginalFilename(),
                dto.getTotalPages(),
                dto.getCategory(),
                dto.getOwnerId(),
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
        dto.setCategory(chessFile.category());
        dto.setOwnerId(chessFile.ownerId());
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

    private Path resolveChessPath(UUID ownerId, UUID bookId) {
        return Paths.get(storageProperties.getChessPath(), ownerId.toString(), bookId + ".chess");
    }

}

