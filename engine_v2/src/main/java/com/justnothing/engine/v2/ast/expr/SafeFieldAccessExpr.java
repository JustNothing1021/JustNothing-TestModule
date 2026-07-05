package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;

import java.util.List;

/**
 * 安全字段访问表达式：{@code target?.fieldName}
 * <p>
 * 当 target 为 null 时短路返回 null，不抛 NPE。
 * </p>
 *
 * @author JustNothing1021
 */
public class SafeFieldAccessExpr extends ASTNode {

    private final ASTNode target;
    private final Resolvable field;

    private SafeFieldAccessExpr(Builder builder) {
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

    public String getFieldName() {
        return field.name();
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "SafeFieldAccess";
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

        public SafeFieldAccessExpr build() {
            return new SafeFieldAccessExpr(this);
        }
    }
}
