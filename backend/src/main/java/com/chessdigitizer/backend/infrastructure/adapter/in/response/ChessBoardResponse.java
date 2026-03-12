package com.chessdigitizer.backend.infrastructure.adapter.in.response;

import lombok.Data;

@Data
public class ChessBoardResponse {
    String id;
    int page;
    BoundingBoxResponse bbox;
    String fen;
    Object analysis;


}
