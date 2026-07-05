package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class ThrowStmt extends ASTNode {

    private final ASTNode expression;

    private ThrowStmt(SourceLocation location, ASTNode expression) {
        super(location);
        this.expression = expression;
    }

    public ASTNode getExpression() {
        return expression;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Throw";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(expression);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode expression;

        public Builder expression(ASTNode expression) {
            this.expression = expression;
            return this;
        }

        public ThrowStmt build() {
            return new ThrowStmt(location, expression);
        }
    }
}
