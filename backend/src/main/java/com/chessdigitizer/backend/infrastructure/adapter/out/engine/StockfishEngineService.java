package com.chessdigitizer.backend.infrastructure.adapter.out.engine;

import com.chessdigitizer.backend.application.config.GlobalProperties.EngineProperties;
import com.chessdigitizer.backend.domain.model.EngineAnalysis;
import com.chessdigitizer.backend.domain.model.Fen;
import com.chessdigitizer.backend.domain.port.out.EngineService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class StockfishEngineService implements EngineService {

    private static final Logger log = LoggerFactory.getLogger(StockfishEngineService.class);

    private final EngineProperties properties;

    private Process process;
    private BufferedWriter stdin;
    private BufferedReader stdout;
    private static final int ANALYSIS_TIMEOUT_MS = 5000;

    public StockfishEngineService(EngineProperties properties) {
        this.properties = properties;
    }

    // ─── Ciclo de vida ───────────────────────────────────────────────────────

    @PostConstruct
    public void init() throws IOException {
        String path = properties.getStockfishPath();
        log.info("Iniciando proceso Stockfish: {}", path);

        process = new ProcessBuilder(path)
                .redirectErrorStream(true)
                .start();

        stdin  = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));

        sendCommand("uci");
        waitFor("uciok");

        sendCommand("isready");
        waitFor("readyok");

        log.info("Stockfish listo y esperando comandos.");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Cerrando proceso Stockfish...");
        try {
            sendCommand("quit");
            process.waitFor();
        } catch (Exception e) {
            log.warn("Error al cerrar Stockfish, forzando destrucción.", e);
            process.destroyForcibly();
        }
    }

    // ─── Puerto ──────────────────────────────────────────────────────────────

    @Override
    public synchronized EngineAnalysis analyze(Fen fen, int moveTimeMs) {
        try {
            sendCommand("isready");
            waitFor("readyok");

            sendCommand("position fen " + fen.value());
            sendCommand("go movetime " + moveTimeMs);

            EngineAnalysis raw = parseAnalysis(readUntilBestMove());

            // UCI devuelve evalCp desde el punto de vista del jugador que mueve.
            // Normalizamos siempre desde el punto de vista de blancas.
            boolean blackToMove = fen.value().split(" ")[1].equals("b");
            int normalizedEval = blackToMove ? -raw.evalCp() : raw.evalCp();

            return EngineAnalysis.of(normalizedEval, raw.bestMove());

        } catch (IOException e) {
            throw new EngineException("Error comunicándose con Stockfish", e);
        }
    }

    // ─── UCI helpers ─────────────────────────────────────────────────────────

    private void sendCommand(String command) throws IOException {
        stdin.write(command);
        stdin.newLine();
        stdin.flush();
    }

    private void waitFor(String expected) throws IOException {
        String line;
        while ((line = stdout.readLine()) != null) {
            if (line.startsWith(expected)) return;
        }
        throw new EngineException("Stockfish no respondió con: " + expected);
    }

    private String readUntilBestMove() throws IOException {
        String line;
        String lastInfo = "";
        long deadline = System.currentTimeMillis() + ANALYSIS_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            if (!stdout.ready()) {
                try { Thread.sleep(10); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new EngineException("Análisis interrumpido");
                }
                continue;
            }
            line = stdout.readLine();
            if (line == null) break;
            if (line.startsWith("info") && line.contains("score")) {
                lastInfo = line;
            }
            if (line.startsWith("bestmove")) {
                return lastInfo + "\n" + line;
            }
        }
        // Timeout — enviar stop para limpiar el estado de Stockfish
        sendCommand("stop");
        throw new EngineException("Timeout esperando respuesta de Stockfish");
    }
    /**
     * Parsea la salida UCI. Ejemplo de entrada:
     *   info depth 20 score cp 45 pv e2e4 e7e5 ...
     *   bestmove e2e4 ponder e7e5
     */
    private EngineAnalysis parseAnalysis(String rawOutput) {
        int evalCp = 0;
        String bestMove = null;

        for (String line : rawOutput.split("\n")) {
            if (line.startsWith("info") && line.contains("score")) {
                if (line.contains("score mate")) {
                    evalCp = parseMateScore(line);
                } else if (line.contains("score cp")) {
                    evalCp = parseEvalCp(line);
                }
            }
            if (line.startsWith("bestmove")) {
                String[] tokens = line.split(" ");
                String move = tokens.length > 1 ? tokens[1] : null;
                // "(none)" significa posición terminal — mate o ahogado
                bestMove = "(none)".equals(move) ? null : move;
            }
        }
        return EngineAnalysis.of(evalCp, bestMove);
    }
    /**
     * "score mate 2"  → +30000 (blancas dan mate en 2)
     * "score mate -1" → -30000 (negras dan mate en 1)
     * Usamos ±30000 como valor especial de "mate" para la UI.
     */
    private int parseMateScore(String infoLine) {
        String[] tokens = infoLine.split(" ");
        for (int i = 0; i < tokens.length - 1; i++) {
            if ("mate".equals(tokens[i])) {
                int movesToMate = Integer.parseInt(tokens[i + 1]);
                return movesToMate > 0 ? 30000 : -30000;
            }
        }
        return 0;
    }

    private int parseEvalCp(String infoLine) {
        String[] tokens = infoLine.split(" ");
        for (int i = 0; i < tokens.length - 1; i++) {
            if ("cp".equals(tokens[i])) {
                return Integer.parseInt(tokens[i + 1]);
            }
        }
        return 0;
    }
}