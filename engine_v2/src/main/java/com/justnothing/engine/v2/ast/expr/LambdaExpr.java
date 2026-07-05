package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LambdaExpr extends ASTNode {

    private final List<String> parameters;
    private final ASTNode body;

    private LambdaExpr(Builder builder) {
        super(builder.getLocation());
        this.parameters = Collections.unmodifiableList(new ArrayList<>(builder.parameters));
        this.body = builder.body;
    }

    public List<String> getParameters() {
        return parameters;
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
        return "Lambda";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(body);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private List<String> parameters = Collections.emptyList();
        private ASTNode body;

        public Builder parameters(List<String> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder body(ASTNode body) {
            this.body = body;
            return this;
        }

        public LambdaExpr build() {
            return new LambdaExpr(this);
        }
    }
}
