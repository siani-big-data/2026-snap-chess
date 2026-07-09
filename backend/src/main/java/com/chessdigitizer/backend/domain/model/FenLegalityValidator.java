package com.chessdigitizer.backend.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Valida la legalidad ajedrecística de una posición FEN, más allá de su corrección
 * estructural (que ya garantiza el propio Value Object Fen).
 *
 * Reglas aplicadas (deliberadamente sin verificar el turno real, ver decisión de diseño
 * documentada en la memoria: el campo de turno del FEN generado por el pipeline de
 * detección es actualmente siempre 'w', por lo que cualquier regla dependiente del turno
 * produciría falsos positivos sistemáticos en posiciones de libro con negras a mover).
 */
public class FenLegalityValidator {

    private static final int MAX_PIECES_PER_SIDE = 16;
    private static final int MAX_PAWNS_PER_SIDE = 8;

    public FenValidationResult validate(Fen fen) {
        String placement = extractPiecePlacement(fen);
        char[][] board = parseBoard(placement);

        List<String> errors = new ArrayList<>();
        errors.addAll(checkKingCounts(board));
        errors.addAll(checkPieceCounts(board));
        errors.addAll(checkPawnsOnBackRanks(placement));
        errors.addAll(checkKingsNotAdjacent(board));
        errors.addAll(checkNoDoubleCheck(board));

        return errors.isEmpty() ? FenValidationResult.ok() : FenValidationResult.invalid(errors);
    }

    private String extractPiecePlacement(Fen fen) {
        return fen.value().split(" ")[0];
    }

    private char[][] parseBoard(String placement) {
        String[] rows = placement.split("/");
        char[][] board = new char[8][8];
        for (int r = 0; r < 8; r++) {
            int col = 0;
            for (char c : rows[r].toCharArray()) {
                if (Character.isDigit(c)) {
                    int empties = c - '0';
                    for (int k = 0; k < empties; k++) {
                        board[r][col++] = '.';
                    }
                } else {
                    board[r][col++] = c;
                }
            }
        }
        return board;
    }

    private List<String> checkKingCounts(char[][] board) {
        long whiteKings = countOccurrences(board, 'K');
        long blackKings = countOccurrences(board, 'k');

        List<String> errors = new ArrayList<>();
        if (whiteKings != 1) {
            errors.add("Se esperaba exactamente 1 rey blanco, se detectaron " + whiteKings);
        }
        if (blackKings != 1) {
            errors.add("Se esperaba exactamente 1 rey negro, se detectaron " + blackKings);
        }
        return errors;
    }

    private List<String> checkPieceCounts(char[][] board) {
        List<String> errors = new ArrayList<>();
        for (boolean white : new boolean[]{true, false}) {
            long total = countSide(board, white);
            long pawns = countOccurrences(board, white ? 'P' : 'p');
            String color = white ? "blancas" : "negras";

            if (total > MAX_PIECES_PER_SIDE) {
                errors.add("Las " + color + " tienen " + total + " piezas, el máximo legal es " + MAX_PIECES_PER_SIDE);
            }
            if (pawns > MAX_PAWNS_PER_SIDE) {
                errors.add("Las " + color + " tienen " + pawns + " peones, el máximo legal es " + MAX_PAWNS_PER_SIDE);
            }
        }
        return errors;
    }

    private List<String> checkPawnsOnBackRanks(String placement) {
        String[] rows = placement.split("/");
        List<String> errors = new ArrayList<>();
        // rows[0] = fila 8, rows[7] = fila 1
        if (rows[0].toLowerCase().contains("p") || rows[7].toLowerCase().contains("p")) {
            errors.add("Hay peones detectados en la primera o última fila, lo cual es imposible");
        }
        return errors;
    }

    private List<String> checkKingsNotAdjacent(char[][] board) {
        int[] whiteKing = findPiece(board, 'K');
        int[] blackKing = findPiece(board, 'k');
        if (whiteKing == null || blackKing == null) {
            return List.of(); // ya reportado por checkKingCounts
        }
        int rowDiff = Math.abs(whiteKing[0] - blackKing[0]);
        int colDiff = Math.abs(whiteKing[1] - blackKing[1]);
        if (rowDiff <= 1 && colDiff <= 1) {
            return List.of("Los dos reyes están en casillas adyacentes, posición imposible");
        }
        return List.of();
    }

    private List<String> checkNoDoubleCheck(char[][] board) {
        int[] whiteKing = findPiece(board, 'K');
        int[] blackKing = findPiece(board, 'k');
        if (whiteKing == null || blackKing == null) {
            return List.of(); // ya reportado por checkKingCounts
        }
        boolean whiteInCheck = isAttacked(board, whiteKing, false);
        boolean blackInCheck = isAttacked(board, blackKing, true);
        if (whiteInCheck && blackInCheck) {
            return List.of("Ambos reyes están en jaque simultáneamente, posición imposible");
        }
        return List.of();
    }

    // -- utilidades de tablero --

    private long countOccurrences(char[][] board, char piece) {
        long count = 0;
        for (char[] row : board) {
            for (char c : row) {
                if (c == piece) count++;
            }
        }
        return count;
    }

    private long countSide(char[][] board, boolean white) {
        long count = 0;
        for (char[] row : board) {
            for (char c : row) {
                if (c == '.') continue;
                boolean isWhitePiece = Character.isUpperCase(c);
                if (isWhitePiece == white) count++;
            }
        }
        return count;
    }

    private int[] findPiece(char[][] board, char piece) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] == piece) return new int[]{r, c};
            }
        }
        return null;
    }

    /**
     * Comprueba si la casilla 'target' está atacada por alguna pieza del bando
     * indicado por 'byWhite'. Implementación geométrica directa (sin generación
     * de movimientos completa); suficiente para detectar jaque doble imposible.
     */
    private boolean isAttacked(char[][] board, int[] target, boolean byWhite) {
        int tr = target[0], tc = target[1];

        // Peones
        char pawn = byWhite ? 'P' : 'p';
        int pawnDir = byWhite ? 1 : -1; // un peón blanco ataca hacia fila menor (hacia r-1)... ver nota
        int[][] pawnAttacks = {{tr + pawnDir, tc - 1}, {tr + pawnDir, tc + 1}};
        for (int[] sq : pawnAttacks) {
            if (inBounds(sq) && board[sq[0]][sq[1]] == pawn) return true;
        }

        // Caballo
        char knight = byWhite ? 'N' : 'n';
        int[][] knightDeltas = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] d : knightDeltas) {
            int[] sq = {tr + d[0], tc + d[1]};
            if (inBounds(sq) && board[sq[0]][sq[1]] == knight) return true;
        }

        // Rey contrario (para detectar adyacencia como amenaza, ya cubierto en checkKingsNotAdjacent,
        // pero se incluye por completitud de la función de ataque)
        char king = byWhite ? 'K' : 'k';
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int[] sq = {tr + dr, tc + dc};
                if (inBounds(sq) && board[sq[0]][sq[1]] == king) return true;
            }
        }

        // Piezas deslizantes: torre/dama en líneas, alfil/dama en diagonales
        char rook = byWhite ? 'R' : 'r';
        char bishop = byWhite ? 'B' : 'b';
        char queen = byWhite ? 'Q' : 'q';

        int[][] straightDirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : straightDirs) {
            if (slidingAttackHits(board, tr, tc, d[0], d[1], rook, queen)) return true;
        }
        int[][] diagonalDirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : diagonalDirs) {
            if (slidingAttackHits(board, tr, tc, d[0], d[1], bishop, queen)) return true;
        }

        return false;
    }

    private boolean slidingAttackHits(char[][] board, int r, int c, int dr, int dc, char piece1, char piece2) {
        int row = r + dr, col = c + dc;
        while (inBounds(new int[]{row, col})) {
            char cell = board[row][col];
            if (cell != '.') {
                return cell == piece1 || cell == piece2;
            }
            row += dr;
            col += dc;
        }
        return false;
    }

    private boolean inBounds(int[] sq) {
        return sq[0] >= 0 && sq[0] < 8 && sq[1] >= 0 && sq[1] < 8;
    }
}