package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

/**
 * super(...) 构造函数调用（仅可在构造函数首条语句出现）。
 *
 * @author JustNothing1021
 */
public class SuperConstructorCallExpr extends ASTNode {

    private final List<ASTNode> arguments;

    private SuperConstructorCallExpr(SourceLocation location, List<ASTNode> arguments) {
        super(location);
        this.arguments = arguments;
    }

    public List<ASTNode> getArguments() {
        return arguments;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "SuperConstructorCall";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(arguments);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private List<ASTNode> arguments = List.of();

        public Builder arguments(List<ASTNode> arguments) {
            this.arguments = arguments;
            return this;
        }

        public SuperConstructorCallExpr build() {
            return new SuperConstructorCallExpr(location, arguments);
        }
    }
}
