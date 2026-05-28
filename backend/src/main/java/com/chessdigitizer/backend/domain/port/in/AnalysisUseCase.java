package com.chessdigitizer.backend.domain.port.in;

import com.chessdigitizer.backend.domain.model.AnalysisNode;
import java.util.List;
import java.util.UUID;

public interface AnalysisUseCase {
    AnalysisNode addMove(UUID bookId, String boardId, List<String> path, String move);
    AnalysisNode updateComment(UUID bookId, String boardId, List<String> path, String comment);
}