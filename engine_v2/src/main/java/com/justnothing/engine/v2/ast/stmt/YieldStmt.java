package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.List;

/**
 * yield expr——switch 表达式的值产出语句。
 */
public class YieldStmt extends ASTNode {

    private final ASTNode value;

    private YieldStmt(Builder builder) {
        super(builder.getLocation());
        this.value = builder.value;
    }

    public ASTNode getValue() {
        return value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Yield";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(value);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode value;

        public Builder value(ASTNode value) { this.value = value; return this; }

        public YieldStmt build() { return new YieldStmt(this); }
    }
}
