package com.chessdigitizer.backend.domain.port.in;

import java.util.UUID;

public interface UpdateBoardFenUseCase {
    void updateFen(UUID bookId, String boardId, String fen);
}