package com.chessdigitizer.backend.domain.port.out;

import com.chessdigitizer.backend.domain.model.AnalysisNode;
import com.chessdigitizer.backend.domain.model.Book;
import com.chessdigitizer.backend.domain.model.ChessFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository {
    void save(Book book);
    Optional<Book> findById(UUID id);
    List<Book> findAll();
    void deleteById(UUID id);
    Optional<ChessFile> findChessFileById(UUID chessFileId);
    void updateTitle(UUID id, String newTitle);

    void saveChessFile(ChessFile chessFile);

    void updateBoardAnalysis(UUID bookId, String boardId, AnalysisNode analysis);

    void updateBoardEval(UUID bookId, String boardId, int evalCp);
}
