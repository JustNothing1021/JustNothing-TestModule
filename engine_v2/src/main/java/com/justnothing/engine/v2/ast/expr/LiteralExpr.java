package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.List;

public class LiteralExpr extends ASTNode {

    private final Object value;
    private final Class<?> type;

    private LiteralExpr(Builder builder) {
        super(builder.getLocation());
        this.value = builder.value;
        this.type = builder.type;
    }

    public Object getValue() {
        return value;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Literal";
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of();
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private Object value;
        private Class<?> type;

        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        public Builder type(Class<?> type) {
            this.type = type;
            return this;
        }

        public LiteralExpr build() {
            return new LiteralExpr(this);
        }
    }
}
