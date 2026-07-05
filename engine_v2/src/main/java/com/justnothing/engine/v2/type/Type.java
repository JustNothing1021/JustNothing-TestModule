package com.justnothing.engine.v2.type;

/**
 * 类型抽象。
 * <p>
 * v2 的类型系统设计目标：
 * <ul>
 *   <li>与 Java 类型系统无缝衔接（{@link #javaClass()} 返回对应的 Java Class）</li>
 *   <li>支持类型变量（用于类型推断）</li>
 *   <li>支持泛型（{@link GenericType}）</li>
 *   <li>支持联合类型（用于控制流分析）</li>
 * </ul>
 * </p>
 *
 * @author JustNothing1021
 */
public sealed interface Type permits ClassType, TypeVar, GenericType, UnionType {

    /**
     * 类型名称
     */
    String name();

    /**
     * 对应的 Java Class（如果存在）
     */
    Class<?> javaClass();

    /**
     * 是否为基本类型
     */
    default boolean isPrimitive() {
        Class<?> cls = javaClass();
        return cls != null && cls.isPrimitive();
    }

    /**
     * 是否为 void 类型
     */
    default boolean isVoid() {
        return this == ClassType.VOID;
    }

    /**
     * 是否为 Object 类型
     */
    default boolean isObject() {
        return this == ClassType.OBJECT;
    }

    /**
     * 此类型是否可以赋值给 target 类型
     */
    default boolean isAssignableTo(Type target) {
        if (this.equals(target)) return true;
        if (target.isObject()) return true;
        Class<?> from = this.javaClass();
        Class<?> to = target.javaClass();
        return from != null && to != null && to.isAssignableFrom(from);
    }
}
