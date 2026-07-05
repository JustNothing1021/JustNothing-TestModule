package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class SynchronizedStmt extends ASTNode {

    private final ASTNode body;
    private final ASTNode lock;

    private SynchronizedStmt(SourceLocation location, ASTNode body, ASTNode lock) {
        super(location);
        this.body = body;
        this.lock = lock;
    }

    public ASTNode getBody() {
        return body;
    }

    public ASTNode getLock() { return lock; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Synchronized";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(body, lock);
    }

    public static class Builder extends ASTNode.Builder<SynchronizedStmt.Builder> {
        private ASTNode body;
        private ASTNode lock;

        public Builder statements(ASTNode statements) {
            this.body = statements;
            return this;
        }

        public Builder lock(ASTNode lock) {
            this.lock = lock;
            return this;
        }

        public SynchronizedStmt build() {
            return new SynchronizedStmt(location, body, lock);
        }
    }

}
