package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;

import java.util.List;

public class FieldAccessExpr extends ASTNode {

    private final ASTNode target;
    private final Resolvable field;

    private FieldAccessExpr(Builder builder) {
        super(builder.getLocation());
        this.target = builder.target;
        this.field = builder.field;
    }

    public ASTNode getTarget() {
        return target;
    }

    public Resolvable getField() {
        return field;
    }

    /** 便捷方法：获取字段名 */
    public String getFieldName() {
        return field.name();
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "FieldAccess";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(target);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode target;
        private Resolvable field;

        public Builder target(ASTNode target) {
            this.target = target;
            return this;
        }

        public Builder fieldName(String fieldName) {
            this.field = Resolvable.byName(fieldName);
            return this;
        }

        public Builder field(Resolvable field) {
            this.field = field;
            return this;
        }

        public FieldAccessExpr build() {
            return new FieldAccessExpr(this);
        }
    }
}
