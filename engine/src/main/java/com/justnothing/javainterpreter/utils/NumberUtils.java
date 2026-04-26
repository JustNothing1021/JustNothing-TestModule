package com.justnothing.javainterpreter.utils;

public class NumberUtils {
    public static Number addNumbers(Number a, Number b) {
        if (TypeUtils.isFloatingType(a) || TypeUtils.isFloatingType(b)) {
            return a.doubleValue() + b.doubleValue();
        }
        if (a instanceof Long || b instanceof Long) {
            return a.longValue() + b.longValue();
        }
        return a.intValue() + b.intValue();
    }

    public static Number subtractNumbers(Number a, Number b) {
        if (TypeUtils.isFloatingType(a) || TypeUtils.isFloatingType(b)) {
            return a.doubleValue() - b.doubleValue();
        }
        if (a instanceof Long || b instanceof Long) {
            return a.longValue() - b.longValue();
        }
        return a.intValue() - b.intValue();
    }

    public static Number multiplyNumbers(Number a, Number b) {
        if (TypeUtils.isFloatingType(a) || TypeUtils.isFloatingType(b)) {
            return a.doubleValue() * b.doubleValue();
        }
        if (a instanceof Long || b instanceof Long) {
            return a.longValue() * b.longValue();
        }
        return a.intValue() * b.intValue();
    }

    public static Number divideNumbers(Number a, Number b) {
        if (TypeUtils.isFloatingType(a) || TypeUtils.isFloatingType(b)) {
            return a.doubleValue() / b.doubleValue();
        }
        if (a instanceof Long || b instanceof Long) {
            return a.longValue() / b.longValue();
        }
        return a.intValue() / b.intValue();
    }

    public static Number moduloNumbers(Number a, Number b) {
        if (TypeUtils.isFloatingType(a) || TypeUtils.isFloatingType(b)) {
            return a.doubleValue() % b.doubleValue();
        }
        if (a instanceof Long || b instanceof Long) {
            return a.longValue() % b.longValue();
        }
        return a.intValue() % b.intValue();
    }

    public static int compareNumbers(Number a, Number b) {
        return Double.compare(a.doubleValue(), b.doubleValue());
    }
}
