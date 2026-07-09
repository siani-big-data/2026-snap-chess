package com.chessdigitizer.backend.infrastructure.adapter.out.DTO;

import lombok.Data;

@Data
public class BoundingBoxDTO {

    double x;
    double y;
    double width;
    double height;
}