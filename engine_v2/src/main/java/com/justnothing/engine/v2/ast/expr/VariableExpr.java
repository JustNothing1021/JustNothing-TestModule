package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;

import java.util.List;

public class VariableExpr extends ASTNode {

    private final Resolvable reference;

    private VariableExpr(Builder builder) {
        super(builder.getLocation());
        this.reference = builder.reference;
    }

    public Resolvable getReference() {
        return reference;
    }

    /** 便捷方法：获取变量名 */
    public String getName() {
        return reference.name();
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Variable";
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of();
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private Resolvable reference;

        public Builder name(String name) {
            this.reference = Resolvable.byName(name);
            return this;
        }

        public Builder reference(Resolvable reference) {
            this.reference = reference;
            return this;
        }

        public VariableExpr build() {
            return new VariableExpr(this);
        }
    }
}
