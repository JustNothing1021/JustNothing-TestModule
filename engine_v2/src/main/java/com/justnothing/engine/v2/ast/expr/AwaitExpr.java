package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class AwaitExpr extends ASTNode {

    private final ASTNode expression;

    private AwaitExpr(Builder builder) {
        super(builder.getLocation());
        this.expression = builder.expression;
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
        return "Await";
    }

    @Override
    public List<ASTNode> getChildren() {
        return expression != null ? List.of(expression) : List.of();
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode expression;

        public Builder expression(ASTNode expression) {
            this.expression = expression;
            return this;
        }

        public AwaitExpr build() {
            return new AwaitExpr(this);
        }
    }
}