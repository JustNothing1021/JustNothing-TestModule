package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.List;

public class AssignmentExpr extends ASTNode {

    private final ASTNode target;
    private final String operator;
    private final ASTNode value;

    private AssignmentExpr(Builder builder) {
        super(builder.getLocation());
        this.target = builder.target;
        this.operator = builder.operator;
        this.value = builder.value;
    }

    public ASTNode getTarget() {
        return target;
    }

    public String getOperator() {
        return operator;
    }

    public ASTNode getValue() {
        return value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Assignment";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(target, value);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode target;
        private String operator;
        private ASTNode value;

        public Builder target(ASTNode target) {
            this.target = target;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder value(ASTNode value) {
            this.value = value;
            return this;
        }

        public AssignmentExpr build() {
            return new AssignmentExpr(this);
        }
    }
}
