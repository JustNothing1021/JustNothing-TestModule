package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class WhileStmt extends ASTNode {

    private final ASTNode condition;
    private final ASTNode body;

    private WhileStmt(SourceLocation location, ASTNode condition, ASTNode body) {
        super(location);
        this.condition = condition;
        this.body = body;
    }

    public ASTNode getCondition() {
        return condition;
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
        return "While";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(condition, body);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode condition;
        private ASTNode body;

        public Builder condition(ASTNode condition) {
            this.condition = condition;
            return this;
        }

        public Builder body(ASTNode body) {
            this.body = body;
            return this;
        }

        public WhileStmt build() {
            return new WhileStmt(location, condition, body);
        }
    }
}
