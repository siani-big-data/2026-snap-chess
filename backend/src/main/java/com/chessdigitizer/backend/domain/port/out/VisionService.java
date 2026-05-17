package com.chessdigitizer.backend.domain.port.out;

import com.chessdigitizer.backend.domain.model.BoardDetection;
import com.chessdigitizer.backend.domain.model.CellImage;

import java.util.List;

public interface VisionService {
    List<BoardDetection> detectBoards(byte[] pageImage);
    List<CellImage> segmentBoard(byte[] boardImage);
    String classifyCell(byte[] cellImage);
}
