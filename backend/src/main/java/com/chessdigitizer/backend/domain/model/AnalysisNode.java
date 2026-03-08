package com.chessdigitizer.backend.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnalysisNode {

    private final String move;
    private final String comment;
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
}