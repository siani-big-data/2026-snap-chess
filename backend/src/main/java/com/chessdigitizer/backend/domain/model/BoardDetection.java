package com.chessdigitizer.backend.domain.model;

import java.util.Objects;

public record BoardDetection(BoundingBox boundingBox, byte[] image){

    public BoardDetection{
        Objects.requireNonNull(boundingBox);
        Objects.requireNonNull(image);
    }

}
