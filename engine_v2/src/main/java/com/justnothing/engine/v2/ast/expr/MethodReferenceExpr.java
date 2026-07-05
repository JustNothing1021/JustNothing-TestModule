package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodReferenceExpr extends ASTNode {

    private final ASTNode target;
    private final String methodName;
    private final List<String> typeArguments;

    private MethodReferenceExpr(Builder builder) {
        super(builder.getLocation());
        this.target = builder.target;
        this.methodName = builder.methodName;
        this.typeArguments = Collections.unmodifiableList(new ArrayList<>(builder.typeArguments));
    }

    public ASTNode getTarget() {
        return target;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getTypeArguments() {
        return typeArguments;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "MethodReference";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(target);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode target;
        private String methodName;
        private List<String> typeArguments = Collections.emptyList();

        public Builder target(ASTNode target) {
            this.target = target;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder typeArguments(List<String> typeArguments) {
            this.typeArguments = typeArguments;
            return this;
        }

        public MethodReferenceExpr build() {
            return new MethodReferenceExpr(this);
        }
    }
}
