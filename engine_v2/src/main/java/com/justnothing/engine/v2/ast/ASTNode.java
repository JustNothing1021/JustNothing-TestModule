package com.justnothing.engine.v2.ast;

import com.justnothing.engine.v2.type.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * AST 节点基类。
 * <p>
 * v2 设计原则：
 * <ul>
 *   <li>结构不可变：子节点、运算符等字段 final，创建后不再修改</li>
 *   <li>类型可标注：resolvedType 由 Phase 2 (TypeChecker) 填充，null = 未解析</li>
 *   <li>Builder 构建：统一通过 Builder 创建，支持常量折叠等构建期优化</li>
 *   <li>Visitor 支持：通过 accept/visit 实现双分派</li>
 * </ul>
 * </p>
 *
 * @author JustNothing1021
 */
public abstract class ASTNode {

    private final SourceLocation location;
    private Type resolvedType;
    private ASTNode binding;

    protected ASTNode(SourceLocation location) {
        this.location = location;
    }

    public SourceLocation getLocation() {
        return location;
    }

    /**
     * 获取已解析的类型（由 TypeChecker 填充），null 表示未解析。
     * 对于有名字的引用（变量、方法等），类型信息也可通过 Resolvable 获取。
     */
    public Type getResolvedType() {
        return resolvedType;
    }

    /**
     * 设置解析后的类型。包级私有，只有同包的 TypeChecker 可调用。
     */
    void setResolvedType(Type type) {
        this.resolvedType = type;
    }

    /**
     * 获取此引用指向的声明（由 Resolver 填充），null = 未绑定。
     */
    public ASTNode getBinding() {
        return binding;
    }

    /**
     * 设置绑定。包级私有，只有同包的 Resolver 可调用。
     */
    void setBinding(ASTNode declaration) {
        this.binding = declaration;
    }

    /**
     * 接受访问者（双分派）
     */
    public abstract <T> T accept(ASTVisitor<T> visitor);

    /**
     * 节点类型名称（用于调试和可视化）
     */
    public abstract String nodeName();

    /**
     * 返回子节点列表（用于 Resolver/遍历），默认空。
     */
    public List<ASTNode> getChildren() {
        return List.of();
    }

    @SafeVarargs
    public static List<ASTNode> children(ASTNode... nodes) {
        List<ASTNode> result = new ArrayList<>();
        for (ASTNode node : nodes) {
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    public static List<ASTNode> children(List<ASTNode> list) {
        return list != null ? new ArrayList<>(list) : List.of();
    }

    public static List<ASTNode> children(List<ASTNode> first, List<ASTNode> second) {
        List<ASTNode> result = new ArrayList<>();
        if (first != null) result.addAll(first);
        if (second != null) result.addAll(second);
        return result;
    }

    public static List<ASTNode> singleton(ASTNode node) {
        return node != null ? List.of(node) : List.of();
    }

    @SafeVarargs
    public static List<ASTNode> children(Object... children) {
        List<ASTNode> result = new ArrayList<>();
        for (Object child : children) {
            if (child instanceof ASTNode node) {
                result.add(node);
            } else if (child instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof ASTNode node) {
                        result.add(node);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return nodeName();
    }

    /**
     * AST 节点 Builder 基类。
     * <p>
     * 使用自泛型（self-referential generic）实现链式调用。
     * 所有子类 Builder 继承此基类，自动获得 location() 方法。
     * </p>
     *
     * @param <B> Builder 自身类型
     */
    protected abstract static class Builder<B extends Builder<B>> {
        protected SourceLocation location;

        @SuppressWarnings("unchecked")
        public B location(SourceLocation location) {
            this.location = location;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B location(int line, int column, String source) {
            this.location = new SourceLocation(line, column, source);
            return (B) this;
        }

        public SourceLocation getLocation() {
            return location;
        }

    }
}
