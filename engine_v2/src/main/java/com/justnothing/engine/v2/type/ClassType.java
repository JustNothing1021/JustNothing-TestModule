package com.justnothing.engine.v2.type;

/**
 * 基于 Java Class 的具体类型。
 *
 * @author JustNothing1021
 */
public final class ClassType implements Type {

    public static final ClassType VOID = new ClassType(void.class, "void");
    public static final ClassType INT = new ClassType(int.class, "int");
    public static final ClassType LONG = new ClassType(long.class, "long");
    public static final ClassType FLOAT = new ClassType(float.class, "float");
    public static final ClassType DOUBLE = new ClassType(double.class, "double");
    public static final ClassType BOOLEAN = new ClassType(boolean.class, "boolean");
    public static final ClassType CHAR = new ClassType(char.class, "char");
    public static final ClassType BYTE = new ClassType(byte.class, "byte");
    public static final ClassType SHORT = new ClassType(short.class, "short");
    public static final ClassType STRING = new ClassType(String.class, "String");
    public static final ClassType OBJECT = new ClassType(Object.class, "Object");

    private final Class<?> clazz;
    private final String name;

    public ClassType(Class<?> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public ClassType(Class<?> clazz) {
        this.clazz = clazz;
        this.name = clazz.getSimpleName();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<?> javaClass() {
        return clazz;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ClassType other)) return false;
        return clazz.equals(other.clazz);
    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
