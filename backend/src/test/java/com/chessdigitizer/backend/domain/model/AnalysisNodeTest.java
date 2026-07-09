package com.chessdigitizer.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisNodeTest {

    // ────────────────────────────────────────────────────────────
    // addOrGetChild
    // ────────────────────────────────────────────────────────────

    @Test
    void addOrGetChild_addsNewChildWhenMoveNotPresent() {
        AnalysisNode root = new AnalysisNode(null, "", null);
        AnalysisNode child = root.addOrGetChild("e4");

        assertEquals("e4", child.getMove(),
                "El hijo creado debe tener el movimiento 'e4'");
        assertEquals(1, root.getChildren().size(),
                "Debe haber exactamente 1 hijo tras añadir 'e4'");
    }

    @Test
    void addOrGetChild_returnsExistingChildWhenMoveAlreadyPresent() {
        AnalysisNode root = new AnalysisNode(null, "", null);
        AnalysisNode first = root.addOrGetChild("e4");
        first.setComment("primer comentario");

        AnalysisNode second = root.addOrGetChild("e4");

        assertSame(first, second,
                "addOrGetChild debe devolver la misma instancia si el movimiento ya existe");
        assertEquals("primer comentario", second.getComment(),
                "El comentario del nodo existente debe conservarse");
        assertEquals(1, root.getChildren().size(),
                "No debe crearse un segundo hijo duplicado");
    }

    @Test
    void addOrGetChild_createsDistinctChildrenForDifferentMoves() {
        AnalysisNode root = new AnalysisNode(null, "", null);
        root.addOrGetChild("e4");
        root.addOrGetChild("d4");

        assertEquals(2, root.getChildren().size(),
                "Deben existir 2 hijos para movimientos distintos 'e4' y 'd4'");
    }

    // ────────────────────────────────────────────────────────────
    // findChild
    // ────────────────────────────────────────────────────────────

    @Test
    void findChild_returnsEmptyWhenNoChildren() {
        AnalysisNode root = new AnalysisNode(null, "", null);
        assertTrue(root.findChild("e4").isEmpty(),
                "findChild en un nodo sin hijos debe devolver Optional.empty()");
    }

    @Test
    void findChild_returnsCorrectChild() {
        AnalysisNode root = new AnalysisNode(null, "", null);
        root.addOrGetChild("e4");
        root.addOrGetChild("d4");

        var result = root.findChild("d4");
        assertTrue(result.isPresent(), "findChild('d4') debe encontrar el hijo");
        assertEquals("d4", result.get().getMove());
    }

    @Test
    void findChild_returnsEmptyForUnknownMove() {
        AnalysisNode root = new AnalysisNode(null, "", null);
        root.addOrGetChild("e4");

        assertTrue(root.findChild("c4").isEmpty(),
                "findChild de un movimiento no presente debe devolver Optional.empty()");
    }

    // ────────────────────────────────────────────────────────────
    // getMainLine
    // ────────────────────────────────────────────────────────────

    @Test
    void getMainLine_returnsEmptyListForLeafNode() {
        AnalysisNode leaf = new AnalysisNode("e4", "", null);
        assertTrue(leaf.getMainLine().isEmpty(),
                "Un nodo hoja no tiene línea principal");
    }

    @Test
    void getMainLine_followsFirstChildAlwaysToEnd() {
        AnalysisNode root = new AnalysisNode(null, "", null);
        AnalysisNode e4 = root.addOrGetChild("e4");
        e4.addOrGetChild("e5");
        // Variante alternativa en el segundo nivel (no debe aparecer en línea principal)
        e4.addOrGetChild("c5");
        AnalysisNode e5 = e4.findChild("e5").orElseThrow();
        e5.addOrGetChild("Nf3");

        List<AnalysisNode> mainLine = root.getMainLine();

        assertEquals(3, mainLine.size(),
                "La línea principal debe tener 3 nodos: e4 → e5 → Nf3");
        assertEquals("e4",  mainLine.get(0).getMove());
        assertEquals("e5",  mainLine.get(1).getMove());
        assertEquals("Nf3", mainLine.get(2).getMove());
    }

    // ────────────────────────────────────────────────────────────
    // getChildren inmutabilidad
    // ────────────────────────────────────────────────────────────

    @Test
    void getChildren_returnsImmutableView() {
        AnalysisNode root = new AnalysisNode(null, "", null);
        root.addOrGetChild("e4");

        assertThrows(UnsupportedOperationException.class,
                () -> root.getChildren().add(new AnalysisNode("d4", "", null)),
                "getChildren() debe devolver una vista no modificable");
    }

    // ────────────────────────────────────────────────────────────
    // addChild duplicado directo (Bug conocido en AnalysisService)
    // ────────────────────────────────────────────────────────────

    @Test
    void addMove_duplicateChildAddedTwiceViaAddChild() {
        // NOTA: AnalysisService.addMove llama a target.addOrGetChild(move) dos veces
        // consecutivamente (líneas 37-39 del servicio). Este test documenta el comportamiento
        // actual: addChild añade sin verificar duplicados, por lo que un addChild directo
        // sobre el mismo movimiento crea dos hijos.
        AnalysisNode root = new AnalysisNode(null, "", null);
        AnalysisNode child = new AnalysisNode("e4", "", null);
        root.addChild(child);
        root.addChild(child); // segunda llamada directa — duplicado

        assertEquals(2, root.getChildren().size(),
                "addChild directo no deduplica: este test documenta el comportamiento actual " +
                "(posible bug en AnalysisService que llama addOrGetChild dos veces)");
    }
}
