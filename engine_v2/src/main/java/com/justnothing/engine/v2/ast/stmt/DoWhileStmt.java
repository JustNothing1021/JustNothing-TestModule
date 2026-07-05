package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class DoWhileStmt extends ASTNode {

    private final ASTNode body;
    private final ASTNode condition;

    private DoWhileStmt(SourceLocation location, ASTNode body, ASTNode condition) {
        super(location);
        this.body = body;
        this.condition = condition;
    }

    public ASTNode getBody() {
        return body;
    }

    public ASTNode getCondition() {
        return condition;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "DoWhile";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(body, condition);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode body;
        private ASTNode condition;

        public Builder body(ASTNode body) {
            this.body = body;
            return this;
        }

        public Builder condition(ASTNode condition) {
            this.condition = condition;
            return this;
        }

        public DoWhileStmt build() {
            return new DoWhileStmt(location, body, condition);
        }
    }
}
