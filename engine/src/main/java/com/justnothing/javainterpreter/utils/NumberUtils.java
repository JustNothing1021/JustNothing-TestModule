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

    public static Object computeMinMax(java.util.List<Object> args, boolean findMin) {
        if (args.isEmpty()) return findMin ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;

        boolean hasDouble = false, hasFloat = false, hasLong = false;
        for (Object arg : args) {
            if (arg instanceof Double) hasDouble = true;
            else if (arg instanceof Float) hasFloat = true;
            else if (arg instanceof Long) hasLong = true;
        }
        Number result = args.get(0) instanceof Number ? (Number) args.get(0) : 0;
        for (int i = 1; i < args.size(); i++) {
            Number val = args.get(i) instanceof Number ? (Number) args.get(i) : 0;
            double cmpA = result.doubleValue();
            double cmpB = val.doubleValue();
            if (findMin ? cmpB < cmpA : cmpB > cmpA) result = val;
        }
        if (hasDouble) return result.doubleValue();
        if (hasFloat) return result.floatValue();
        if (hasLong) return result.longValue();
        return result.intValue();
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
