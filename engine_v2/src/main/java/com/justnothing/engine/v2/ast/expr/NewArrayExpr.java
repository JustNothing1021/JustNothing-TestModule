package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数组创建表达式：{@code new int[5]}、{@code new String[]{"a","b"}}
 *
 * @author JustNothing1021
 */
public class NewArrayExpr extends ASTNode {

    private final Resolvable elementType;
    private final List<ASTNode> dimensions;
    private final List<ASTNode> initializer;

    private NewArrayExpr(Builder builder) {
        super(builder.getLocation());
        this.elementType = builder.elementType;
        this.dimensions = Collections.unmodifiableList(new ArrayList<>(builder.dimensions));
        this.initializer = builder.initializer != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.initializer))
                : null;
    }

    public Resolvable getElementType() {
        return elementType;
    }

    public String getElementTypeName() {
        return elementType.name();
    }

    public List<ASTNode> getDimensions() {
        return dimensions;
    }

    /** 花括号初始化器，null 表示没有初始化器 */
    public List<ASTNode> getInitializer() {
        return initializer;
    }

    public boolean hasInitializer() {
        return initializer != null;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "NewArray";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(initializer);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private Resolvable elementType;
        private List<ASTNode> dimensions = Collections.emptyList();
        private List<ASTNode> initializer;

        public Builder elementType(String typeName) {
            this.elementType = Resolvable.byName(typeName);
            return this;
        }

        public Builder elementType(Resolvable elementType) {
            this.elementType = elementType;
            return this;
        }

        public Builder dimensions(List<ASTNode> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder initializer(List<ASTNode> initializer) {
            this.initializer = initializer;
            return this;
        }

        public NewArrayExpr build() {
            return new NewArrayExpr(this);
        }
    }
}
