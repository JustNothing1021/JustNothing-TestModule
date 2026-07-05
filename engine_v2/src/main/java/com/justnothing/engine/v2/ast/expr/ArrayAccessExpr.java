package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.List;

public class ArrayAccessExpr extends ASTNode {

    private final ASTNode target;
    private final ASTNode index;

    private ArrayAccessExpr(Builder builder) {
        super(builder.getLocation());
        this.target = builder.target;
        this.index = builder.index;
    }

    public ASTNode getTarget() {
        return target;
    }

    public ASTNode getIndex() {
        return index;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "ArrayAccess";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(target, index);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode target;
        private ASTNode index;

        public Builder target(ASTNode target) {
            this.target = target;
            return this;
        }

        public Builder index(ASTNode index) {
            this.index = index;
            return this;
        }

        public ArrayAccessExpr build() {
            return new ArrayAccessExpr(this);
        }
    }
}
