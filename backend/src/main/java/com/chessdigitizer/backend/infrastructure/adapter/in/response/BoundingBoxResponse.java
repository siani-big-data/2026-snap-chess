package com.chessdigitizer.backend.infrastructure.adapter.in.response;

import lombok.Data;

@Data
public class BoundingBoxResponse {

    double x;
    double y;
    double width;
    double height;

}
