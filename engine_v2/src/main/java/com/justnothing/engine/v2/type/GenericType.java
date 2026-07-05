package com.justnothing.engine.v2.type;

import java.util.List;

/**
 * 泛型类型（如 {@code List<String>}）。
 *
 * @author JustNothing1021
 */
public final class GenericType implements Type {

    private final Class<?> rawType;
    private final List<Type> typeArguments;
    private final String name;

    public GenericType(Class<?> rawType, List<Type> typeArguments) {
        this.rawType = rawType;
        this.typeArguments = typeArguments;
        this.name = rawType.getSimpleName() + typeArguments;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<?> javaClass() {
        return rawType;
    }

    public List<Type> typeArguments() {
        return typeArguments;
    }

    @Override
    public String toString() {
        return name;
    }
}
