package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;

import java.util.List;

public class CastExpr extends ASTNode {

    private final Resolvable targetType;
    private final ASTNode expr;

    private CastExpr(Builder builder) {
        super(builder.getLocation());
        this.targetType = builder.targetType;
        this.expr = builder.expr;
    }

    public Resolvable getTargetTypeRef() {
        return targetType;
    }

    /** 便捷方法：获取目标类型名 */
    public String getTargetType() {
        return targetType.name();
    }

    public ASTNode getExpr() {
        return expr;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Cast";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(expr);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private Resolvable targetType;
        private ASTNode expr;

        public Builder targetType(String targetType) {
            this.targetType = Resolvable.byName(targetType);
            return this;
        }

        public Builder targetTypeRef(Resolvable targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder expr(ASTNode expr) {
            this.expr = expr;
            return this;
        }

        public CastExpr build() {
            return new CastExpr(this);
        }
    }
}
