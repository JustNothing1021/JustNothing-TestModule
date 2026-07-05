package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.Collections;
import java.util.List;

public class TryStmt extends ASTNode {

    private final ASTNode tryBlock;
    private final List<CatchClause> catchClauses;
    private final ASTNode finallyBlock;
    private final List<ASTNode> resources;

    private TryStmt(SourceLocation location, ASTNode tryBlock, List<CatchClause> catchClauses, ASTNode finallyBlock, List<ASTNode> resources) {
        super(location);
        this.tryBlock = tryBlock;
        this.catchClauses = catchClauses;
        this.finallyBlock = finallyBlock;
        this.resources = resources;
    }

    public ASTNode getTryBlock() {
        return tryBlock;
    }

    public List<CatchClause> getCatchClauses() {
        return catchClauses;
    }

    public ASTNode getFinallyBlock() {
        return finallyBlock;
    }

    public List<ASTNode> getResources() {
        return resources;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Try";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(tryBlock, catchClauses.stream().map(CatchClause::block).toList(), finallyBlock, resources);
    }

    public record CatchClause(List<String> exceptionTypes, String variableName, ASTNode block) {}

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode tryBlock;
        private List<CatchClause> catchClauses;
        private ASTNode finallyBlock;
        private List<ASTNode> resources;

        public Builder tryBlock(ASTNode tryBlock) {
            this.tryBlock = tryBlock;
            return this;
        }

        public Builder catchClauses(List<CatchClause> catchClauses) {
            this.catchClauses = catchClauses;
            return this;
        }

        public Builder finallyBlock(ASTNode finallyBlock) {
            this.finallyBlock = finallyBlock;
            return this;
        }

        public Builder resources(List<ASTNode> resources) {
            this.resources = resources;
            return this;
        }

        public TryStmt build() {
            return new TryStmt(location, tryBlock, catchClauses, finallyBlock, resources != null ? resources : Collections.emptyList());
        }
    }
}
