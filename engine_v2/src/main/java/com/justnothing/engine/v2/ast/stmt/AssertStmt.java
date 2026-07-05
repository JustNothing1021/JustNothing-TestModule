package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class AssertStmt extends ASTNode {

    private final ASTNode condition;
    private final ASTNode message;

    private AssertStmt(SourceLocation location, ASTNode condition, ASTNode message) {
        super(location);
        this.condition = condition;
        this.message = message;
    }

    public ASTNode getCondition() {
        return condition;
    }

    public ASTNode getMessage() {
        return message;
    }

    public boolean hasMessage() {
        return message != null;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Assert";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(condition, message);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode condition;
        private ASTNode message;

        public Builder condition(ASTNode condition) {
            this.condition = condition;
            return this;
        }

        public Builder message(ASTNode message) {
            this.message = message;
            return this;
        }

        public AssertStmt build() {
            return new AssertStmt(location, condition, message);
        }
    }
}
