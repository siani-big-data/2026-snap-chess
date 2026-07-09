package com.chessdigitizer.backend.infrastructure.adapter.out.vision;

import com.chessdigitizer.backend.domain.model.BoardDetection;
import com.chessdigitizer.backend.domain.model.CellImage;
import com.chessdigitizer.backend.domain.port.out.VisionService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HttpVisionService implements VisionService {

    private final BoardDetectorClient detectorClient;
    private final BoardSegmenterClient segmenterClient;
    private final PieceClassifierClient classifierClient;

    public HttpVisionService(BoardDetectorClient detectorClient,
                             BoardSegmenterClient segmenterClient,
                             PieceClassifierClient classifierClient) {
        this.detectorClient = detectorClient;
        this.segmenterClient = segmenterClient;
        this.classifierClient = classifierClient;
    }

    @Override
    public List<BoardDetection> detectBoards(byte[] pageImage) {
        return detectorClient.detect(pageImage);
    }

    @Override
    public List<CellImage> segmentBoard(byte[] boardImage) {
        return segmenterClient.segment(boardImage);
    }

    @Override
    public String classifyCell(byte[] cellImage) {
        return classifierClient.classify(cellImage);
    }
}