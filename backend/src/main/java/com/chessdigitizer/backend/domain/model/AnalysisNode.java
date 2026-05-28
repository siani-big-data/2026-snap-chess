package com.chessdigitizer.backend.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class AnalysisNode {

    private final String move;
    private String comment;
    private final Integer evalCp;
    private final List<AnalysisNode> children;

    public AnalysisNode(String move, String comment, Integer evalCp) {
        this.move = move;
        this.comment = comment;
        this.evalCp = evalCp;
        this.children = new ArrayList<>();
    }

    public void addChild(AnalysisNode child) {
        this.children.add(child);
    }

    public String getMove(){ return move; }
    public String getComment(){ return comment; }
    public Integer getEvalCp(){ return evalCp; }

    public List<AnalysisNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public Optional<AnalysisNode> findChild(String move) {
        return children.stream()
                .filter(child -> child.move.equals(move))
                .findFirst();
    }

    public AnalysisNode addOrGetChild(String move) {
        return findChild(move).orElseGet(() -> {
            AnalysisNode newChild = new AnalysisNode(move, "", null);
            children.add(newChild);
            return newChild;
        });
    }

    public List<AnalysisNode> getMainLine() {
        List<AnalysisNode> line = new ArrayList<>();
        AnalysisNode current = this;
        while (!current.children.isEmpty()) {
            current = current.children.get(0);
            line.add(current);
        }
        return Collections.unmodifiableList(line);
    }

    public void setComment(String comment) { this.comment = comment; }
}