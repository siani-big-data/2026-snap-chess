package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.application.config.GlobalProperties;
import com.chessdigitizer.backend.domain.model.*;
import com.chessdigitizer.backend.domain.port.in.AnalyzeBoardUseCase;
import com.chessdigitizer.backend.domain.port.out.BookRepository;
import com.chessdigitizer.backend.domain.port.out.PdfRenderer;
import com.chessdigitizer.backend.domain.port.out.VisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalyzeBoardService implements AnalyzeBoardUseCase {

    private static final Map<String, Character> PIECE_TO_FEN = Map.ofEntries(
            Map.entry("wp", 'P'), Map.entry("wr", 'R'), Map.entry("wn", 'N'),
            Map.entry("wb", 'B'), Map.entry("wq", 'Q'), Map.entry("wk", 'K'),
            Map.entry("bp", 'p'), Map.entry("br", 'r'), Map.entry("bn", 'n'),
            Map.entry("bb", 'b'), Map.entry("bq", 'q'), Map.entry("bk", 'k')
    );

    private final PdfRenderer pdfRenderer;
    private final GlobalProperties.RenderProperties renderProperties;
    private final VisionService visionService;
    private final BookRepository bookRepository;

    public AnalyzeBoardService(PdfRenderer pdfRenderer, GlobalProperties.RenderProperties renderProperties, VisionService visionService, BookRepository bookRepository) {
        this.pdfRenderer = pdfRenderer;
        this.renderProperties = renderProperties;
        this.visionService = visionService;
        this.bookRepository = bookRepository;
    }

    @Override
    public ChessFile analyzePage(UUID bookId, int pageNumber) {
        byte[] pageImage = pdfRenderer.renderPage(bookId, pageNumber, renderProperties.getDefaultDpi());
        List<BoardDetection> detectedBoards = visionService.detectBoards(pageImage);
        List<ChessBoard> newBoards = new ArrayList<>();

        for (BoardDetection board : detectedBoards) {
            List<CellImage> cells = visionService.segmentBoard(board.image());
            List<String> classifications = new ArrayList<>();
            for (CellImage cell : cells) {
                classifications.add(visionService.classifyCell(cell.image()));
            }
            String fen = buildFen(classifications);
            log.info("FEN generado para tablero {} página {}: {}", newBoards.size() + 1, pageNumber, fen);
            String boardId = "board-p%d-%d".formatted(pageNumber, newBoards.size() + 1);
            newBoards.add(new ChessBoard(
                    boardId,
                    pageNumber,
                    board.boundingBox(),
                    new Fen(fen),
                    new AnalysisNode(null, "", null)));
        }
        return updateAndSaveChessFile(bookId, pageNumber, newBoards);
    }

    private ChessFile updateAndSaveChessFile(UUID bookId, int pageNumber, List<ChessBoard> newBoards) {
        ChessFile chessFile = bookRepository.findChessFileById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("ChessFile not found for bookId: " + bookId));

        List<ChessBoard> updatedBoards = chessFile.boards().stream()
                .filter(b -> b.page() != pageNumber)
                .collect(Collectors.toCollection(ArrayList::new));
        updatedBoards.addAll(newBoards);

        ChessFile updatedChessFile = new ChessFile(
                chessFile.id(),
                chessFile.title(),
                chessFile.originalFilename(),
                chessFile.totalPages(),
                updatedBoards
        );
        bookRepository.saveChessFile(updatedChessFile);
        return updatedChessFile;
    }

    private String buildFen(List<String> classifications) {
        StringBuilder fen = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int emptyCount = 0;
            for (int j = 0; j < 8; j++) {
                String classification = classifications.get(i * 8 + j);

                if (Objects.equals(classification, "empty")) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(PIECE_TO_FEN.get(classification));
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (i != 7) {
                fen.append("/");
            }
        }
        return fen.toString() + " w - - 0 1";
    }
}