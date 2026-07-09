package com.chessdigitizer.backend.infrastructure.adapter.out.DTO;

import lombok.Data;

@Data
public class ChessBoardDTO {

    private String id;
    private int page;
    private BoundingBoxDTO boundingBox;
    private String fen;
    AnalysisNodeDTO analysis;
}
