package com.chessdigitizer.backend.application.service;

import com.chessdigitizer.backend.application.config.GlobalProperties;
import com.chessdigitizer.backend.domain.model.*;
import com.chessdigitizer.backend.domain.port.out.BookRepository;
import com.chessdigitizer.backend.domain.port.out.PdfRenderer;
import com.chessdigitizer.backend.domain.port.out.VisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyzeBoardServiceTest {

    @Mock
    private PdfRenderer pdfRenderer;
    @Mock
    private VisionService visionService;
    @Mock
    private BookRepository bookRepository;

    private GlobalProperties.RenderProperties renderProperties;
    private AnalyzeBoardService analyzeBoardService;

    @BeforeEach
    void setUp() {
        renderProperties = new GlobalProperties.RenderProperties();
        renderProperties.setDefaultDpi(150);
        analyzeBoardService = new AnalyzeBoardService(pdfRenderer, renderProperties, visionService, bookRepository);
    }

    @Test
    void analyzePage_replacesBoardsOnSamePage_andBuildsEmptyFen() {
        UUID bookId = UUID.randomUUID();
        int page = 2;
        ChessFile existing = new ChessFile(
                bookId,
                "Título",
                "f.pdf",
                10,
                BookCategory.GENERAL,
                new ArrayList<>(List.of(
                        new ChessBoard("old-1", page, new BoundingBox(1, 1, 5, 5), Fen.STARTING_POSITION,
                                new AnalysisNode(null, "", null)),
                        new ChessBoard("keep", 3, new BoundingBox(1, 1, 5, 5), Fen.STARTING_POSITION,
                                new AnalysisNode(null, "", null))
                ))
        );
        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.of(existing));
        when(pdfRenderer.renderPage(bookId, page, 150)).thenReturn(new byte[]{1});

        BoundingBox detBox = new BoundingBox(0, 0, 10, 10);
        List<CellImage> cells = IntStream.range(0, 64)
                .mapToObj(i -> new CellImage("sq" + i, new byte[]{2}))
                .toList();
        when(visionService.detectBoards(any(byte[].class)))
                .thenReturn(List.of(new BoardDetection(detBox, new byte[]{3})));
        when(visionService.segmentBoard(any(byte[].class))).thenReturn(cells);
        when(visionService.classifyCell(any(byte[].class))).thenReturn("empty");

        ChessFile result = analyzeBoardService.analyzePage(bookId, page);

        assertEquals(2, result.boards().size());
        assertTrue(result.boards().stream().anyMatch(b -> "keep".equals(b.id())));
        ChessBoard detected = result.boards().stream()
                .filter(b -> b.page() == page)
                .findFirst()
                .orElseThrow();
        assertEquals("board-p2-1", detected.id());
        assertEquals("8/8/8/8/8/8/8/8 w - - 0 1", detected.fen().value());

        ArgumentCaptor<ChessFile> saved = ArgumentCaptor.forClass(ChessFile.class);
        verify(bookRepository).saveChessFile(saved.capture());
        assertEquals("8/8/8/8/8/8/8/8 w - - 0 1",
                saved.getValue().boards().stream()
                        .filter(b -> b.page() == page)
                        .findFirst()
                        .orElseThrow()
                        .fen()
                        .value());
    }

    @Test
    void analyzePage_throwsWhenChessFileMissing() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findChessFileById(bookId)).thenReturn(Optional.empty());
        when(pdfRenderer.renderPage(bookId, 1, 150)).thenReturn(new byte[]{1});
        when(visionService.detectBoards(any())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> analyzeBoardService.analyzePage(bookId, 1));
    }
}
