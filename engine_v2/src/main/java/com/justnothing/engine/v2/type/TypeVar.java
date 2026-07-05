package com.justnothing.engine.v2.type;

/**
 * 类型变量（用于类型推断）。
 * <p>
 * 在 Phase 2 (Type Inference) 中，类型变量会被统一求解为具体类型。
 * </p>
 *
 * @author JustNothing1021
 */
public final class TypeVar implements Type {

    private static int counter = 0;

    private final String name;
    private Type bound;

    public TypeVar() {
        this("T" + counter++);
    }

    public TypeVar(String name) {
        this.name = name;
        this.bound = ClassType.OBJECT;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<?> javaClass() {
        return bound.javaClass();
    }

    /**
     * 获取当前上界
     */
    public Type bound() {
        return bound;
    }

    /**
     * 统一：将此类型变量绑定到具体类型
     */
    public void unify(Type type) {
        this.bound = type;
    }

    /**
     * 是否已被求解
     */
    public boolean isResolved() {
        return !(bound instanceof TypeVar);
    }

    @Override
    public String toString() {
        return isResolved() ? bound.toString() : name + " <: " + bound;
    }
}
