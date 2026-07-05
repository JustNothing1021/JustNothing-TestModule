package com.justnothing.engine.v2.type;

import java.util.Set;

/**
 * 联合类型（用于控制流分析，如 if 分支后变量的类型可能是 A | B）。
 *
 * @author JustNothing1021
 */
public final class UnionType implements Type {

    private final Set<Type> alternatives;

    public UnionType(Set<Type> alternatives) {
        this.alternatives = alternatives;
    }

    @Override
    public String name() {
        StringBuilder sb = new StringBuilder();
        for (Type t : alternatives) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(t.name());
        }
        return sb.toString();
    }

    @Override
    public Class<?> javaClass() {
        // 联合类型的 javaClass 是所有备选类型的最近公共父类
        Class<?> result = null;
        for (Type alt : alternatives) {
            Class<?> cls = alt.javaClass();
            if (cls == null) return null;
            if (result == null) {
                result = cls;
            } else {
                result = commonSuperclass(result, cls);
            }
        }
        return result;
    }

    public Set<Type> alternatives() {
        return alternatives;
    }

    private static Class<?> commonSuperclass(Class<?> a, Class<?> b) {
        if (a.isAssignableFrom(b)) return a;
        if (b.isAssignableFrom(a)) return b;
        Class<?> sup = a.getSuperclass();
        while (sup != null && !sup.isAssignableFrom(b)) {
            sup = sup.getSuperclass();
        }
        return sup != null ? sup : Object.class;
    }

    @Override
    public String toString() {
        return name();
    }
}
