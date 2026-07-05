package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArrayLiteralExpr extends ASTNode {

    private final List<ASTNode> elements;

    private ArrayLiteralExpr(Builder builder) {
        super(builder.getLocation());
        this.elements = Collections.unmodifiableList(new ArrayList<>(builder.elements));
    }

    public List<ASTNode> getElements() {
        return elements;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "ArrayLiteral";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(elements);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private List<ASTNode> elements = Collections.emptyList();

        public Builder elements(List<ASTNode> elements) {
            this.elements = elements;
            return this;
        }

        public ArrayLiteralExpr build() {
            return new ArrayLiteralExpr(this);
        }
    }
}
