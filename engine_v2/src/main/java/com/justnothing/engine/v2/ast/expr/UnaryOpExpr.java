package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.List;

public class UnaryOpExpr extends ASTNode {

    private final String operator;
    private final ASTNode operand;
    private final boolean prefix;

    private UnaryOpExpr(Builder builder) {
        super(builder.getLocation());
        this.operator = builder.operator;
        this.operand = builder.operand;
        this.prefix = builder.prefix;
    }

    public String getOperator() {
        return operator;
    }

    public ASTNode getOperand() {
        return operand;
    }

    public boolean isPrefix() {
        return prefix;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "UnaryOp";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(operand);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String operator;
        private ASTNode operand;
        private boolean prefix;

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder operand(ASTNode operand) {
            this.operand = operand;
            return this;
        }

        public Builder prefix(boolean prefix) {
            this.prefix = prefix;
            return this;
        }

        public UnaryOpExpr build() {
            return new UnaryOpExpr(this);
        }
    }
}
