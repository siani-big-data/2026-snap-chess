package com.chessdigitizer.backend.infrastructure.adapter.in.response;

import com.chessdigitizer.backend.domain.model.BoundingBox;
import com.chessdigitizer.backend.domain.model.ChessBoard;
import com.chessdigitizer.backend.domain.model.ChessFile;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ChessFileResponse {
    private UUID id;
    private String title;
    private String originalFilename;
    private int totalPages;
    private List<ChessBoardResponse> boards = new ArrayList<>();

    public static ChessFileResponse fromDomain(ChessFile chessFile) {
        ChessFileResponse response = new ChessFileResponse();

        // 1. Mapeo de la lista de tableros
        for(ChessBoard board : chessFile.boards()){
            BoundingBox bbox = board.boundingBox();

            BoundingBoxResponse bboxRes = new BoundingBoxResponse();
            bboxRes.setX(bbox.x());
            bboxRes.setY(bbox.y());
            bboxRes.setWidth(bbox.width());
            bboxRes.setHeight(bbox.height());

            ChessBoardResponse boardRes = new ChessBoardResponse();
            boardRes.setId(board.id());
            boardRes.setPage(board.page());
            boardRes.setBbox(bboxRes);
            boardRes.setFen(board.fen().value());
            boardRes.setAnalysis(AnalysisNodeResponse.fromDomain(board.analysis()));

            response.getBoards().add(boardRes);
        }

        response.setId(chessFile.id());
        response.setTitle(chessFile.title());
        response.setOriginalFilename(chessFile.originalFilename());
        response.setTotalPages(chessFile.totalPages());

        return response;
    }
}
