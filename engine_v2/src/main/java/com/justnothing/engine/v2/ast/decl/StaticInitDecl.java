package com.justnothing.engine.v2.ast.decl;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;
import com.justnothing.engine.v2.ast.stmt.BlockStmt;

import java.util.List;

public class StaticInitDecl extends ASTNode {
    private final BlockStmt statements;

    private StaticInitDecl(SourceLocation location, BlockStmt statements) {
        super(location);
        this.statements = statements;
    }

    public BlockStmt getStatements() {
        return statements;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "StaticInitDecl";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(statements);
    }

    public static class Builder extends ASTNode.Builder<StaticInitDecl.Builder> {
        private BlockStmt statements;

        public StaticInitDecl.Builder statements(BlockStmt statements) { 
            this.statements = statements;
            return this;
        }

        public StaticInitDecl build() {
            return new StaticInitDecl(location, statements);
        }
    }
}
