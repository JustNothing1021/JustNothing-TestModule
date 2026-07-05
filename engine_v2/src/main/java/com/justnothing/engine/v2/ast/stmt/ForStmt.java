package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class ForStmt extends ASTNode {

    private final ASTNode initializer;
    private final ASTNode condition;
    private final ASTNode update;
    private final ASTNode body;

    private ForStmt(SourceLocation location, ASTNode initializer, ASTNode condition, ASTNode update, ASTNode body) {
        super(location);
        this.initializer = initializer;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    public ASTNode getInitializer() {
        return initializer;
    }

    public ASTNode getCondition() {
        return condition;
    }

    public ASTNode getUpdate() {
        return update;
    }

    public ASTNode getBody() {
        return body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "For";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(initializer, condition, update, body);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode initializer;
        private ASTNode condition;
        private ASTNode update;
        private ASTNode body;

        public Builder initializer(ASTNode initializer) {
            this.initializer = initializer;
            return this;
        }

        public Builder condition(ASTNode condition) {
            this.condition = condition;
            return this;
        }

        public Builder update(ASTNode update) {
            this.update = update;
            return this;
        }

        public Builder body(ASTNode body) {
            this.body = body;
            return this;
        }

        public ForStmt build() {
            return new ForStmt(location, initializer, condition, update, body);
        }
    }
}
