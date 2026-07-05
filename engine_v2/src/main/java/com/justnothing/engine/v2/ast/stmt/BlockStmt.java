package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class BlockStmt extends ASTNode {

    private final List<ASTNode> statements;

    private BlockStmt(SourceLocation location, List<ASTNode> statements) {
        super(location);
        this.statements = statements;
    }

    public List<ASTNode> getStatements() {
        return statements;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Block";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(statements);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private List<ASTNode> statements;

        public Builder statements(List<ASTNode> statements) {
            this.statements = statements;
            return this;
        }

        public BlockStmt build() {
            return new BlockStmt(location, statements);
        }
    }
}
