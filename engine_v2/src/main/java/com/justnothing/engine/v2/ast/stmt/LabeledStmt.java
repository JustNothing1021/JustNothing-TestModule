package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class LabeledStmt extends ASTNode {

    private final String label;
    private final ASTNode body;

    private LabeledStmt(SourceLocation location, String label, ASTNode body) {
        super(location);
        this.label = label;
        this.body = body;
    }

    public String getLabel() {
        return label;
    }

    public ASTNode getBody() {
        return body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "LabeledStmt";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(body);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String label;
        private ASTNode body;

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder body(ASTNode body) {
            this.body = body;
            return this;
        }

        public LabeledStmt build() {
            return new LabeledStmt(location, label, body);
        }
    }
}
