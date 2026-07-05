package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InterpolatedStringExpr extends ASTNode {

    private final List<Object> parts;

    private InterpolatedStringExpr(Builder builder) {
        super(builder.getLocation());
        this.parts = Collections.unmodifiableList(new ArrayList<>(builder.parts));
    }

    public List<Object> getParts() {
        return parts;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "InterpolatedString";
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of();
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private List<Object> parts = Collections.emptyList();

        public Builder parts(List<Object> parts) {
            this.parts = parts;
            return this;
        }

        public InterpolatedStringExpr build() {
            return new InterpolatedStringExpr(this);
        }
    }
}
