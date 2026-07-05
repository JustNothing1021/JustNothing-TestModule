package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.List;

public class TernaryExpr extends ASTNode {

    private final ASTNode condition;
    private final ASTNode trueExpr;
    private final ASTNode falseExpr;

    private TernaryExpr(Builder builder) {
        super(builder.getLocation());
        this.condition = builder.condition;
        this.trueExpr = builder.trueExpr;
        this.falseExpr = builder.falseExpr;
    }

    public ASTNode getCondition() {
        return condition;
    }

    public ASTNode getTrueExpr() {
        return trueExpr;
    }

    public ASTNode getFalseExpr() {
        return falseExpr;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Ternary";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(condition, trueExpr, falseExpr);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode condition;
        private ASTNode trueExpr;
        private ASTNode falseExpr;

        public Builder condition(ASTNode condition) {
            this.condition = condition;
            return this;
        }

        public Builder trueExpr(ASTNode trueExpr) {
            this.trueExpr = trueExpr;
            return this;
        }

        public Builder falseExpr(ASTNode falseExpr) {
            this.falseExpr = falseExpr;
            return this;
        }

        public TernaryExpr build() {
            return new TernaryExpr(this);
        }
    }
}
