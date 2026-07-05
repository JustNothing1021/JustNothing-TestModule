package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;

import java.util.List;

public class InstanceofExpr extends ASTNode {

    private final ASTNode expr;
    private final Resolvable typeRef;
    private final String bindingVar;

    private InstanceofExpr(Builder builder) {
        super(builder.getLocation());
        this.expr = builder.expr;
        this.typeRef = builder.typeRef;
        this.bindingVar = builder.bindingVar;
    }

    public ASTNode getExpr() { return expr; }
    public Resolvable getTypeRef() { return typeRef; }
    public String getBindingVar() { return bindingVar; }

    /** 便捷方法：获取类型名 */
    public String getTypeName() { return typeRef.name(); }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Instanceof";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(expr);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode expr;
        private Resolvable typeRef;
        private String bindingVar;

        public Builder expr(ASTNode expr) { this.expr = expr; return this; }
        public Builder typeName(String typeName) { this.typeRef = Resolvable.byName(typeName); return this; }
        public Builder typeRef(Resolvable typeRef) { this.typeRef = typeRef; return this; }
        public Builder bindingVar(String bindingVar) { this.bindingVar = bindingVar; return this; }

        public InstanceofExpr build() { return new InstanceofExpr(this); }
    }
}
