package com.chessdigitizer.backend.infrastructure.adapter.in;

import com.chessdigitizer.backend.domain.model.AnalysisNode;
import com.chessdigitizer.backend.domain.port.in.AnalysisUseCase;
import com.chessdigitizer.backend.infrastructure.adapter.in.response.AnalysisNodeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books/{bookId}/boards/{boardId}/analysis")
public class AnalysisController {

    private final AnalysisUseCase analysisUseCase;

    public AnalysisController(AnalysisUseCase analysisUseCase) {
        this.analysisUseCase = analysisUseCase;
    }

    @PostMapping("/moves")
    public ResponseEntity<AnalysisNodeResponse> addMove(
            @PathVariable UUID bookId,
            @PathVariable String boardId,
            @RequestBody AddMoveRequest request) {

        AnalysisNode updatedTree = analysisUseCase.addMove(bookId, boardId, request.path(), request.move());
        return ResponseEntity.ok(AnalysisNodeResponse.fromDomain(updatedTree));
    }

    @PatchMapping("/comment")
    public ResponseEntity<AnalysisNodeResponse> updateComment(
            @PathVariable UUID bookId,
            @PathVariable String boardId,
            @RequestBody UpdateCommentRequest request) {

        AnalysisNode updatedTree = analysisUseCase.updateComment(
                bookId, boardId, request.path(), request.comment());
        return ResponseEntity.ok(AnalysisNodeResponse.fromDomain(updatedTree));
    }

    public record UpdateCommentRequest(List<String> path, String comment) {}
    public record AddMoveRequest(List<String> path, String move) {}


}