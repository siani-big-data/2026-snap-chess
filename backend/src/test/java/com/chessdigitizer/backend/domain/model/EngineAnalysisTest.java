package com.chessdigitizer.backend.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineAnalysisTest {

    @Test
    void formattedEval_displaysPositiveValueWithPlusSign() {
        EngineAnalysis analysis = EngineAnalysis.of(145, "e2e4");
        assertEquals("+1.45", analysis.formattedEval(),
                "145 centipeones deben formatearse como '+1.45'");
    }

    @Test
    void formattedEval_displaysNegativeValueWithoutPlusSign() {
        EngineAnalysis analysis = EngineAnalysis.of(-230, "d7d5");
        assertEquals("-2.30", analysis.formattedEval(),
                "-230 centipeones deben formatearse como '-2.30'");
    }

    @Test
    void formattedEval_displaysZeroWithPlusSign() {
        EngineAnalysis analysis = EngineAnalysis.of(0, "e2e4");
        assertEquals("+0.00", analysis.formattedEval(),
                "0 centipeones deben formatearse como '+0.00'");
    }

    @Test
    void formattedEval_handlesLargeMateScore() {
        // 30000 = valor especial de "mate" (ver StockfishEngineService)
        EngineAnalysis analysis = EngineAnalysis.of(30000, "e2e4");
        assertEquals("+300.00", analysis.formattedEval(),
                "30000 centipeones (mate) deben formatearse como '+300.00'");
    }

    @Test
    void of_factoryMethod_preservesBestMove() {
        EngineAnalysis analysis = EngineAnalysis.of(50, "g1f3");
        assertEquals("g1f3", analysis.bestMove(),
                "El mejor movimiento debe conservarse");
    }

    @Test
    void of_factoryMethod_acceptsNullBestMove_forTerminalPositions() {
        // Posición de mate: Stockfish devuelve "(none)" → bestMove=null
        EngineAnalysis analysis = EngineAnalysis.of(30000, null);
        assertNull(analysis.bestMove(),
                "bestMove puede ser null en posiciones terminales (mate/ahogado)");
    }
}
