package com.justnothing.testmodule.command.functions.script_new.evaluator;

public class AutoClass {
    private static final AutoClass INSTANCE = new AutoClass();
    
    private AutoClass() {}
    
    public static AutoClass getInstance() {
        return INSTANCE;
    }
    
    public static Class<?> inferType(Object value) {
        if (value == null) {
            return Object.class;
        }
        
        Class<?> actualType = value.getClass();
        
        if (value instanceof Integer) {
            return int.class;
        }
        if (value instanceof Long) {
            return long.class;
        }
        if (value instanceof Double) {
            return double.class;
        }
        if (value instanceof Float) {
            return float.class;
        }
        if (value instanceof Boolean) {
            return boolean.class;
        }
        if (value instanceof Character) {
            return char.class;
        }
        if (value instanceof Byte) {
            return byte.class;
        }
        if (value instanceof Short) {
            return short.class;
        }
        
        return actualType;
    }
    
    public static Class<?> inferStrictType(Object value) {
        if (value == null) {
            return Object.class;
        }
        
        if (value instanceof Integer) {
            return int.class;
        }
        if (value instanceof Long) {
            return long.class;
        }
        if (value instanceof Double) {
            return double.class;
        }
        if (value instanceof Float) {
            return float.class;
        }
        if (value instanceof Boolean) {
            return boolean.class;
        }
        if (value instanceof Character) {
            return char.class;
        }
        if (value instanceof Byte) {
            return byte.class;
        }
        if (value instanceof Short) {
            return short.class;
        }
        if (value instanceof String) {
            return String.class;
        }
        
        return value.getClass();
    }
    
    public static boolean isAutoType(Class<?> type) {
        return type == AutoClass.class;
    }
    
    @Override
    public String toString() {
        return "auto";
    }
}
