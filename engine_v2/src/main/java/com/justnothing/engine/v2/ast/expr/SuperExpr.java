package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.List;

/**
 * super 引用表达式。
 * <p>
 * 与 VariableExpr("super") 不同，这是语义明确的 super 引用，
 * TypeChecker 可以据此正确解析超类方法/字段。
 * </p>
 *
 * @author JustNothing1021
 */
public class SuperExpr extends ASTNode {

    private SuperExpr(Builder builder) {
        super(builder.getLocation());
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Super";
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of();
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        public SuperExpr build() {
            return new SuperExpr(this);
        }
    }
}
