package com.justnothing.javainterpreter.utils;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.exception.ErrorCode;
import com.justnothing.javainterpreter.exception.EvaluationException;

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

    public static Object createRange(Object start, Object end, boolean exclusive, ASTNode rangeNode) {
        if (start instanceof Number left && end instanceof Number right) {
            return createIntRange((Number) left, (Number) right, exclusive, rangeNode);
        }

        if (start instanceof Character left && end instanceof Character right) {
            return createCharRange((Character) left, (Character) right, exclusive, rangeNode);
        }

        throw new EvaluationException(
                "Range operator requires numeric or character operands",
                ErrorCode.EVAL_TYPE_MISMATCH,
                rangeNode);
    }

    public static Integer[] createIntRange(Number start, Number end, boolean exclusive, ASTNode rangeNode) {
        int startVal = start.intValue();
        int endVal = end.intValue();
        int actualEnd = exclusive ? endVal - 1 : endVal;

        if (startVal <= actualEnd) {
            int size = actualEnd - startVal + 1;
            Integer[] result = new Integer[size];
            for (int i = 0; i < size; i++) {
                result[i] = startVal + i;
            }
            return result;
        } else {
            int size = startVal - actualEnd + 1;
            Integer[] result = new Integer[size];
            for (int i = 0; i < size; i++) {
                result[i] = startVal - i;
            }
            return result;
        }
    }

    public static Character[] createCharRange(Character start, Character end, boolean exclusive, ASTNode rangeNode) {
        char startChar = (Character) start;
        char endChar = (Character) end;
        char actualEnd = exclusive ? (char) (endChar - 1) : endChar;

        if (startChar <= actualEnd) {
            int size = actualEnd - startChar + 1;
            Character[] result = new Character[size];
            for (int i = 0; i < size; i++) {
                result[i] = (char) (startChar + i);
            }
            return result;
        } else {
            int size = startChar - actualEnd + 1;
            Character[] result = new Character[size];
            for (int i = 0; i < size; i++) {
                result[i] = (char) (startChar - i);
            }
            return result;
        }
    }
}
