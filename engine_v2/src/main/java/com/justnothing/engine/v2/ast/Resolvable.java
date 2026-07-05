package com.justnothing.engine.v2.ast;

import com.justnothing.engine.v2.type.Type;

import java.util.Objects;

/**
 * 可解析的名称-类型引用。
 * <p>
 * 设计原则：name 和 resolved 二选一必填。
 * <ul>
 *   <li>从名字创建（未解析）：{@code Resolvable.byName("x")} → 后续通过 setResolved 填充类型</li>
 *   <li>从已解析类型创建：{@code Resolvable.resolved(type)} → 名字从 type.name() 推导</li>
 * </ul>
 * </p>
 *
 * @author JustNothing1021
 */
public final class Resolvable {

    private final String name;
    private Type resolved;

    private Resolvable(String name, Type resolved) {
        // 至少一个非 null
        if (name == null && resolved == null) {
            throw new IllegalArgumentException("name 和 resolved 不能同时为 null");
        }
        this.name = name;
        this.resolved = resolved;
    }

    // ========== 工厂方法 ==========

    /**
     * 从名字创建（未解析状态）
     */
    public static Resolvable byName(String name) {
        return new Resolvable(Objects.requireNonNull(name), null);
    }

    /**
     * 从已解析类型创建（名字从类型推导）
     */
    public static Resolvable resolved(Type type) {
        return new Resolvable(null, Objects.requireNonNull(type));
    }

    /**
     * 同时指定名字和已解析类型
     */
    public static Resolvable of(String name, Type type) {
        return new Resolvable(
                Objects.requireNonNull(name),
                Objects.requireNonNull(type)
        );
    }

    // ========== 访问 ==========

    /**
     * 获取名称。优先用显式名字，否则从已解析类型推导。
     */
    public String name() {
        if (name != null) return name;
        return resolved.name();
    }

    /**
     * 获取已解析的类型，null 表示尚未解析。
     */
    public Type resolvedType() {
        return resolved;
    }

    /**
     * 是否已完成类型解析
     */
    public boolean isResolved() {
        return resolved != null;
    }

    /**
     * 设置解析结果。包级私有，只有 TypeChecker 可调用。
     */
    void setResolved(Type type) {
        this.resolved = Objects.requireNonNull(type);
    }

    @Override
    public String toString() {
        if (resolved != null) {
            return name != null ? name + ": " + resolved : resolved.toString();
        }
        return name + ": ?";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resolvable other)) return false;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
