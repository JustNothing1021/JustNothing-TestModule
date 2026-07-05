package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

public class ContinueStmt extends ASTNode {

    private final String label;

    private ContinueStmt(SourceLocation location, String label) {
        super(location);
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Continue";
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String label;

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public ContinueStmt build() {
            return new ContinueStmt(location, label);
        }
    }
}
