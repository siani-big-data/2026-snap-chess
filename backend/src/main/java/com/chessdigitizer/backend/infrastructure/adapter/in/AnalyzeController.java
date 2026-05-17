package com.chessdigitizer.backend.infrastructure.adapter.in;

import com.chessdigitizer.backend.domain.port.in.AnalyzeBoardUseCase;
import com.chessdigitizer.backend.infrastructure.adapter.in.response.ChessFileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@Slf4j
public class AnalyzeController {

    private final AnalyzeBoardUseCase analyzeBoardUseCase;

    public AnalyzeController(AnalyzeBoardUseCase analyzeBoardUseCase) {
        this.analyzeBoardUseCase = analyzeBoardUseCase;
    }

    @PostMapping("/{id}/pages/{pageNumber}/analyze")
    public ResponseEntity<ChessFileResponse> analyzePage(
            @PathVariable UUID id,
            @PathVariable int pageNumber) {
        log.info("Analizando página {} del libro {}", pageNumber, id);
        return ResponseEntity.ok(
                ChessFileResponse.fromDomain(analyzeBoardUseCase.analyzePage(id, pageNumber))
        );
    }
}