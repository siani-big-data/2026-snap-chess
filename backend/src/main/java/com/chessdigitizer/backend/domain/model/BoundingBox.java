package com.chessdigitizer.backend.domain.model;

public record BoundingBox(
        double x,
        double y,
        double width,
        double height
) {
    public BoundingBox{
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
    }
}
