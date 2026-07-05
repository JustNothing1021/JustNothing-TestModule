package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodCallExpr extends ASTNode {

    private final ASTNode target;
    private final Resolvable method;
    private final List<ASTNode> arguments;
    private final List<String> typeArguments;

    private MethodCallExpr(Builder builder) {
        super(builder.getLocation());
        this.target = builder.target;
        this.method = builder.method;
        this.arguments = Collections.unmodifiableList(new ArrayList<>(builder.arguments));
        this.typeArguments = builder.typeArguments;
    }

    public ASTNode getTarget() {
        return target;
    }

    public Resolvable getMethod() {
        return method;
    }

    /** 便捷方法：获取方法名 */
    public String getMethodName() {
        return method.name();
    }

    public List<ASTNode> getArguments() {
        return arguments;
    }

    public List<String> getTypeArguments() { return typeArguments; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "MethodCall";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(ASTNode.singleton(target), arguments);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode target;
        private Resolvable method;
        private List<ASTNode> arguments = Collections.emptyList();
        private List<String> typeArguments = Collections.emptyList();

        public Builder target(ASTNode target) {
            this.target = target;
            return this;
        }

        public Builder methodName(String methodName) {
            this.method = Resolvable.byName(methodName);
            return this;
        }

        public Builder method(Resolvable method) {
            this.method = method;
            return this;
        }

        public Builder arguments(List<ASTNode> arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder typeArguments(List<String> typeArguments) {
            this.typeArguments = typeArguments;
            return this;
        }

        public MethodCallExpr build() {
            return new MethodCallExpr(this);
        }
    }
}
