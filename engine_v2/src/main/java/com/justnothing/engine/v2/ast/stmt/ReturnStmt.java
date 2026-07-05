package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class ReturnStmt extends ASTNode {

    private final ASTNode value;

    private ReturnStmt(SourceLocation location, ASTNode value) {
        super(location);
        this.value = value;
    }

    public ASTNode getValue() {
        return value;
    }

    public boolean hasValue() {
        return value != null;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Return";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(value);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode value;

        public Builder value(ASTNode value) {
            this.value = value;
            return this;
        }

        public ReturnStmt build() {
            return new ReturnStmt(location, value);
        }
    }
}
