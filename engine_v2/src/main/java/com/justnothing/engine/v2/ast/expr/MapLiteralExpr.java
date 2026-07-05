package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapLiteralExpr extends ASTNode {

    private final List<ASTNode> keys;
    private final List<ASTNode> values;

    private MapLiteralExpr(Builder builder) {
        super(builder.getLocation());
        this.keys = Collections.unmodifiableList(new ArrayList<>(builder.keys));
        this.values = Collections.unmodifiableList(new ArrayList<>(builder.values));
    }

    public List<ASTNode> getKeys() {
        return keys;
    }

    public List<ASTNode> getValues() {
        return values;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "MapLiteral";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(keys, values);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private List<ASTNode> keys = Collections.emptyList();
        private List<ASTNode> values = Collections.emptyList();

        public Builder keys(List<ASTNode> keys) {
            this.keys = keys;
            return this;
        }

        public Builder values(List<ASTNode> values) {
            this.values = values;
            return this;
        }

        public MapLiteralExpr build() {
            return new MapLiteralExpr(this);
        }
    }
}
