package com.chessdigitizer.backend.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundingBoxTest {

    @Test
    void rejectsNonPositiveWidth() {
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(0, 0, 0, 10));
    }

    @Test
    void rejectsNonPositiveHeight() {
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(0, 0, 10, 0));
    }

    @Test
    void acceptsPositiveDimensions() {
        BoundingBox box = new BoundingBox(1, 2, 3, 4);
        assertEquals(1, box.x());
        assertEquals(4, box.height());
    }
}
