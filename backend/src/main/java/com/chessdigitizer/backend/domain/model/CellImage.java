package com.chessdigitizer.backend.domain.model;

import java.util.Objects;

public record CellImage(String notation, byte[] image){
    public CellImage{
        Objects.requireNonNull(notation);
        Objects.requireNonNull(image);
    }
}
