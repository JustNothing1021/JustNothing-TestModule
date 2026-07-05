package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class IfStmt extends ASTNode {

    private final ASTNode condition;
    private final ASTNode thenBlock;
    private final ASTNode elseBlock;

    private IfStmt(SourceLocation location, ASTNode condition, ASTNode thenBlock, ASTNode elseBlock) {
        super(location);
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    public ASTNode getCondition() {
        return condition;
    }

    public ASTNode getThenBlock() {
        return thenBlock;
    }

    public ASTNode getElseBlock() {
        return elseBlock;
    }

    public boolean hasElse() {
        return elseBlock != null;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "If";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(condition, thenBlock, elseBlock);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode condition;
        private ASTNode thenBlock;
        private ASTNode elseBlock;

        public Builder condition(ASTNode condition) {
            this.condition = condition;
            return this;
        }

        public Builder thenBlock(ASTNode thenBlock) {
            this.thenBlock = thenBlock;
            return this;
        }

        public Builder elseBlock(ASTNode elseBlock) {
            this.elseBlock = elseBlock;
            return this;
        }

        public IfStmt build() {
            return new IfStmt(location, condition, thenBlock, elseBlock);
        }
    }
}
