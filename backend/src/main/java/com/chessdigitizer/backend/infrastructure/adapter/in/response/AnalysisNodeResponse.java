package com.chessdigitizer.backend.infrastructure.adapter.in.response;

import com.chessdigitizer.backend.domain.model.AnalysisNode;
import java.util.List;

public record AnalysisNodeResponse(
        String move,
        String comment,
        Integer evalCp,
        List<AnalysisNodeResponse> children
) {
    public static AnalysisNodeResponse fromDomain(AnalysisNode node) {
        if (node == null) return null;

        List<AnalysisNodeResponse> childrenRes = node.getChildren().stream()
                .map(AnalysisNodeResponse::fromDomain)
                .toList();

        return new AnalysisNodeResponse(
                node.getMove(),
                node.getComment(),
                node.getEvalCp(),
                childrenRes.isEmpty() ? null : childrenRes
        );
    }
}