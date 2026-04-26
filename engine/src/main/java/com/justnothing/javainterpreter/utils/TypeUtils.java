package com.justnothing.javainterpreter.utils;

import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.builtins.Lambda;
import com.justnothing.javainterpreter.builtins.MethodReference;
import com.justnothing.javainterpreter.exception.ErrorCode;
import com.justnothing.javainterpreter.exception.EvaluationException;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeUtils {


    public static boolean isAssignable(@NotNull Class<?> targetType, @NotNull Class<?> sourceType) {
        if (targetType.isPrimitive()) {
            return isAssignableWrapperFor(sourceType, targetType);
        }
        if (sourceType == Lambda.class && isFunctionalInterface(targetType)) return true;
        if (sourceType == MethodReference.class && isFunctionalInterface(targetType)) return true;
        return targetType.isAssignableFrom(sourceType);
    }

    public static boolean isFunctionalInterface(Class<?> type) {
        return type.isAnnotationPresent(FunctionalInterface.class);
    }

    public static boolean isAssignableObject(Class<?> targetType, Object arg) {
        if (arg == null) {
            return !targetType.isPrimitive();
        }
        Class<?> argType = arg.getClass();
        return isAssignable(targetType, argType);
    }

    private static boolean isApplicableArgs(Class<?>[] paramTypes, Class<?>[] argTypes, boolean isVarArgs) {
        if (isVarArgs) {
            if (argTypes.length < paramTypes.length - 1) {
                return false;
            }
            for (int i = 0; i < paramTypes.length - 1; i++) {
                if (!isAssignable(paramTypes[i], argTypes[i])) {
                    return false;
                }
            }
            Class<?> varArgType = paramTypes[paramTypes.length - 1].getComponentType();
            for (int i = paramTypes.length - 1; i < argTypes.length; i++) {
                if (!isAssignable(varArgType, argTypes[i])) {
                    return false;
                }
            }
        } else {
            if (paramTypes.length != argTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!isAssignable(paramTypes[i], argTypes[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Class<?> getWrapperClass(Class<?> primitiveType) {
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == double.class) return Double.class;
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == char.class) return Character.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == short.class) return Short.class;
        if (primitiveType == void.class) return Void.class;
        return null;
    }


    public static Class<?> inferArrayType(Object[] elements) {
        if (elements.length == 0) return null;

        Class<?> result = null;
        boolean hasNull = false;
        boolean allNumbers = true;

        for (Object elem : elements) {
            if (elem == null) {
                hasNull = true;
                continue;
            }
            Class<?> elemType = elem.getClass();
            if (!(elem instanceof Number)) allNumbers = false;
            if (result == null) {
                result = elemType;
            } else if (!result.isAssignableFrom(elemType)) {
                if (elemType.isAssignableFrom(result)) {
                    result = elemType;
                } else {
                    return Object.class;
                }
            }
        }

        if (result == null) return null;

        if (allNumbers && result != Object.class) {
            return inferPrimitiveNumberType(elements);
        }

        return hasNull && result.isPrimitive() ? getWrapperClass(result) : result;
    }

    private static Class<?> inferPrimitiveNumberType(Object[] elements) {
        boolean hasDouble = false, hasFloat = false, hasLong = false;
        for (Object elem : elements) {
            if (elem == null) continue;
            if (elem instanceof Double) hasDouble = true;
            else if (elem instanceof Float) hasFloat = true;
            else if (elem instanceof Long) hasLong = true;
        }
        if (hasDouble) return double.class;
        if (hasFloat) return float.class;
        if (hasLong) return long.class;
        return int.class;
    }

    public static Object convertToPrimitive(Object value, Class<?> targetType) throws EvaluationException {
        if (value == null) {
            return getDefaultValue(targetType);
        }

        if (targetType == int.class) {
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof Character) return (int) (Character) value;
        } else if (targetType == long.class) {
            if (value instanceof Number) return ((Number) value).longValue();
            if (value instanceof Character) return (long) (Character) value;
        } else if (targetType == double.class) {
            if (value instanceof Number) return ((Number) value).doubleValue();
            if (value instanceof Character) return (double) (Character) value;
        } else if (targetType == float.class) {
            if (value instanceof Number) return ((Number) value).floatValue();
            if (value instanceof Character) return (float) (Character) value;
        } else if (targetType == short.class) {
            if (value instanceof Number) return ((Number) value).shortValue();
            if (value instanceof Character) return (short) ((Character) value).charValue();
        } else if (targetType == byte.class) {
            if (value instanceof Number) return ((Number) value).byteValue();
            if (value instanceof Character) return (byte) ((Character) value).charValue();
        } else if (targetType == char.class) {
            if (value instanceof Number) return (char) ((Number) value).intValue();
            if (value instanceof Character) return value;
            if (value instanceof String && ((String) value).length() == 1) return ((String) value).charAt(0);
        } else if (targetType == boolean.class) {
            if (value instanceof Boolean) return value;
        }

        throw new EvaluationException(
                "Cannot convert " + value.getClass().getSimpleName() + " to " + targetType.getSimpleName(),
                -1,
                -1,
                ErrorCode.EVAL_TYPE_MISMATCH
        );
    }

    public static Object getDefaultValue(Class<?> type) {
        if (type == null) return null;

        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;

        return null;
    }

    private static boolean isLambdaFunctionalInterfaceMatch(Class<?>[] paramTypes, Class<?>[] runtimeArgTypes) {
        if (paramTypes.length != runtimeArgTypes.length) return false;
        for (int i = 0; i < paramTypes.length; i++) {
            if (runtimeArgTypes[i] == Lambda.class) {
                if (!paramTypes[i].isInterface()) return false;
            } else if (!isAssignable(paramTypes[i], runtimeArgTypes[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAssignableWrapperFor(Class<?> wrapperType, Class<?> primitiveType) {
        if (primitiveType == int.class) return wrapperType == Integer.class;
        if (primitiveType == long.class) return wrapperType == Long.class || wrapperType == Integer.class;
        if (primitiveType == double.class) return wrapperType == Double.class || wrapperType == Float.class || wrapperType == Long.class || wrapperType == Integer.class;
        if (primitiveType == float.class) return wrapperType == Float.class || wrapperType == Integer.class;
        if (primitiveType == boolean.class) return wrapperType == Boolean.class;
        if (primitiveType == char.class) return wrapperType == Character.class;
        if (primitiveType == byte.class) return wrapperType == Byte.class;
        if (primitiveType == short.class) return wrapperType == Short.class || wrapperType == Byte.class;
        return false;
    }

    private static boolean isExactWrapperFor(Class<?> wrapperType, Class<?> primitiveType) {
        if (primitiveType == int.class) return wrapperType == Integer.class;
        if (primitiveType == long.class) return wrapperType == Long.class;
        if (primitiveType == double.class) return wrapperType == Double.class;
        if (primitiveType == float.class) return wrapperType == Float.class;
        if (primitiveType == boolean.class) return wrapperType == Boolean.class;
        if (primitiveType == char.class) return wrapperType == Character.class;
        if (primitiveType == byte.class) return wrapperType == Byte.class;
        if (primitiveType == short.class) return wrapperType == Short.class;
        return false;
    }

    public static Class<?> getWrapperType(Class<?> primitiveType) {
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == double.class) return Double.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == char.class) return Character.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == short.class) return Short.class;
        return primitiveType;
    }

    public static Class<?> getPrimitiveType(String typeName) {
        return switch (typeName) {
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "boolean" -> boolean.class;
            case "char" -> char.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "void" -> void.class;
            default -> Object.class;
        };
    }



    private static int computeMatchScore(Class<?>[] paramTypes, Object[] args) {
        int score = 0;
        for (int i = 0; i < paramTypes.length; i++) {
            if (args == null || i >= args.length) {
                return 0;
            }
            if (args[i] == null) {
                if (paramTypes[i].isPrimitive()) {
                    return 0;
                }
                score += 1;
            } else {
                Class<?> argType = args[i].getClass();

                if (paramTypes[i].equals(argType)) {
                    score += 3;
                } else if (paramTypes[i].isPrimitive() && isExactWrapperFor(argType, paramTypes[i])) {
                    score += 2;
                } else if (isAssignable(paramTypes[i], argType)) {
                    score += 1;
                } else if (paramTypes[i].isPrimitive() && isAssignableWrapperFor(argType, paramTypes[i])) {
                    score += 1;
                } else {
                    return 0;
                }
            }
        }
        return score;
    }

    public static Method findMethod(Class<?> clazz, String name, Object[] args) {
        Method method = findMethod(clazz, name, args, false);
        if (method != null) return method;
        return findMethod(clazz, name, args, true);
    }

    public static Method findMethod(Class<?> clazz, String name, Object[] args, boolean isStatic) {
        List<Method> candidates = new ArrayList<>();

        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
                candidates.add(method);
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name) && !candidates.contains(method)) {
                candidates.add(method);
            }
        }

        Method bestMatch = null;
        int bestScore = -1;
        Method varArgsCandidate = null;

        for (Method method : candidates) {
            if (isStatic != Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Class<?>[] paramTypes = method.getParameterTypes();
            boolean isVarArgs = method.isVarArgs();

            if (isApplicableArgs(paramTypes, args, isVarArgs)) {
                if (!isVarArgs && paramTypes.length == (args != null ? args.length : 0)) {
                    int matchScore = computeMatchScore(paramTypes, args);

                    if (matchScore > bestScore) {
                        bestScore = matchScore;
                        bestMatch = method;
                    }
                }

                if (isVarArgs && varArgsCandidate == null) {
                    varArgsCandidate = method;
                }
            }
        }

        if (bestMatch != null) {
            return bestMatch;
        }

        return varArgsCandidate;
    }




    public static Constructor<?> findConstructor(Class<?> clazz, Class<?>[] argTypes) throws NoSuchMethodException {
        // 首先尝试精确匹配
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(argTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignored) {
        }

        // 然后尝试公共构造函数
        try {
            return clazz.getConstructor(argTypes);
        } catch (NoSuchMethodException ignored) {
        }

        // 最后尝试找到最匹配的构造函数
        Constructor<?> bestMatch = null;
        int bestScore = -1;

        // 检查所有声明的构造函数
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (isApplicableArgs(paramTypes, argTypes, constructor.isVarArgs())) {
                int score = computeMatchScore(paramTypes, argTypes);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = constructor;
                }
            }
        }

        // 如果没有找到，检查公共构造函数
        if (bestMatch == null) {
            for (Constructor<?> constructor : clazz.getConstructors()) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (isApplicableArgs(paramTypes, argTypes, constructor.isVarArgs())) {
                    int score = computeMatchScore(paramTypes, argTypes);
                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = constructor;
                    }
                }
            }
        }

        if (bestMatch != null) {
            bestMatch.setAccessible(true);
            return bestMatch;
        }

        throw new NoSuchMethodException("No suitable constructor found for arguments: " + Arrays.toString(argTypes));
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        try {
            // 首先尝试在当前类中查找字段
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // 如果当前类没有该字段，尝试在父类中查找
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return findField(superClass, fieldName);
            }
            return null;
        }
    }

    public static Object resolveField(String fieldName, Object instance) {
        if (instance == null) {
            return null;
        }
        try {
            // 尝试使用反射直接获取字段，包括从父类继承的字段
            Field field = TypeUtils.findField(instance.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(instance);
            }
        } catch (Exception e) {
            // 字段访问失败，返回 null 让调用者尝试其他方法
            return null;
        }
        // 字段未找到，返回 null 让调用者尝试其他方法
        return null;
    }


    public static Class<?>[] resolveSuperConstructorArgTypes(Class<?> superClass, Class<?>[] runtimeArgTypes) {
        try {
            Constructor<?> bestMatch = null;
            int bestScore = -1;

            for (Constructor<?> constructor : superClass.getDeclaredConstructors()) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (isApplicableArgs(paramTypes, runtimeArgTypes, constructor.isVarArgs())) {
                    int score = computeMatchScore(paramTypes, runtimeArgTypes);
                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = constructor;
                    }
                } else if (isLambdaFunctionalInterfaceMatch(paramTypes, runtimeArgTypes)) {
                    int score = computeMatchScore(paramTypes, runtimeArgTypes);
                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = constructor;
                    }
                }
            }

            if (bestMatch == null) {
                for (Constructor<?> constructor : superClass.getConstructors()) {
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    if (isApplicableArgs(paramTypes, runtimeArgTypes, constructor.isVarArgs())) {
                        int score = computeMatchScore(paramTypes, runtimeArgTypes);
                        if (score > bestScore) {
                            bestScore = score;
                            bestMatch = constructor;
                        }
                    } else if (isLambdaFunctionalInterfaceMatch(paramTypes, runtimeArgTypes)) {
                        int score = computeMatchScore(paramTypes, runtimeArgTypes);
                        if (score > bestScore) {
                            bestScore = score;
                            bestMatch = constructor;
                        }
                    }
                }
            }

            if (bestMatch != null) {
                return bestMatch.getParameterTypes();
            }
        } catch (Exception ignored) {}
        return null;
    }


    public static Object[] prepareInvokeArguments(Method method, Object[] args) {
        Class<?>[] paramTypes = method.getParameterTypes();

        if (!method.isVarArgs() || paramTypes.length == 0) {
            for (int i = 0; i < paramTypes.length; i++) {
                if (args[i] instanceof Lambda lambda && isFunctionalInterface(paramTypes[i])) {
                    args[i] = lambda.asInterface(paramTypes[i]);
                } else if (args[i] instanceof MethodReference methodReference) {
                    args[i] = methodReference.asInterface(paramTypes[i]);
                }
            }
            return args;
        }

        int fixedParamCount = paramTypes.length - 1;
        Class<?> varArgsType = paramTypes[fixedParamCount];
        Class<?> varArgsComponentType = varArgsType.getComponentType();

        if (args.length < fixedParamCount) {
            return args;
        }

        Object[] invokeArgs = new Object[paramTypes.length];

        System.arraycopy(args, 0, invokeArgs, 0, fixedParamCount);

        for (int i = 0; i < fixedParamCount; i++) {
            if (invokeArgs[i] instanceof Lambda lambda && isFunctionalInterface(paramTypes[i])) {
                invokeArgs[i] = lambda.asInterface(paramTypes[i]);
            } else if (invokeArgs[i] instanceof MethodReference methodReference) {
                invokeArgs[i] = methodReference.asInterface(paramTypes[i]);
            }
        }

        int varArgCount = args.length - fixedParamCount;

        if (varArgCount == 1) {
            Object lastArg = args[fixedParamCount];

            if (lastArg != null && lastArg.getClass().isArray() &&
                    TypeUtils.isAssignable(varArgsComponentType, lastArg.getClass().getComponentType())) {
                invokeArgs[fixedParamCount] = lastArg;
            } else {
                assert varArgsComponentType != null;
                Object varArgArray = Array.newInstance(varArgsComponentType, 1);
                Array.set(varArgArray, 0, lastArg);
                invokeArgs[fixedParamCount] = varArgArray;
            }
        } else if (varArgCount == 0) {
            assert varArgsComponentType != null;
            invokeArgs[fixedParamCount] = Array.newInstance(varArgsComponentType, 0);
        } else {
            assert varArgsComponentType != null;
            Object varArgArray = Array.newInstance(varArgsComponentType, varArgCount);
            for (int i = 0; i < varArgCount; i++) {
                Array.set(varArgArray, i, args[fixedParamCount + i]);
            }
            invokeArgs[fixedParamCount] = varArgArray;
        }

        return invokeArgs;
    }

    private static boolean isApplicableArgs(Class<?>[] paramTypes, Object[] args, boolean isVarArgs) {
        int paramCount = paramTypes.length;
        int argCount = args != null ? args.length : 0;

        if (isVarArgs) {
            if (argCount < paramCount - 1) {
                return false;
            }

            for (int i = 0; i < paramCount - 1; i++) {
                if (!isAssignableObject(paramTypes[i], args[i])) {
                    return false;
                }
            }

            if (argCount >= paramCount) {
                Class<?> varArgType = paramTypes[paramCount - 1].getComponentType();
                for (int i = paramCount - 1; i < argCount; i++) {
                    if (!isAssignableObject(varArgType, args[i])) {
                        return false;
                    }
                }
            }

        } else {
            if (paramCount != argCount) {
                return false;
            }

            for (int i = 0; i < paramCount; i++) {
                if (!isAssignableObject(paramTypes[i], args[i])) {
                    return false;
                }
            }

        }
        return true;
    }


    public static Object convertValue(Object value, Class<?> targetType, SourceLocation location) throws EvaluationException {
        if (value == null) {
            if (targetType.isPrimitive()) {
                throw new EvaluationException(
                        "Cannot assign null to primitive type: " + targetType.getName(),
                        location,
                        ErrorCode.EVAL_TYPE_MISMATCH
                );
            }
            return null;
        }

        if (targetType == null || targetType == Object.class) {
            return value;
        }

        Class<?> sourceType = value.getClass();

        if (targetType.isAssignableFrom(sourceType)) {
            return value;
        }

        if (targetType.isArray() && sourceType.isArray()) {
            return convertArray(value, targetType, location);
        }

        if (targetType.isPrimitive() && sourceType.isAssignableFrom(TypeUtils.getWrapperType(targetType))) {
            return value;
        }

        if (value instanceof Number num) {

            if (targetType == int.class || targetType == Integer.class) {
                return num.intValue();
            }
            if (targetType == long.class || targetType == Long.class) {
                return num.longValue();
            }
            if (targetType == float.class || targetType == Float.class) {
                return num.floatValue();
            }
            if (targetType == double.class || targetType == Double.class) {
                return num.doubleValue();
            }
            if (targetType == byte.class || targetType == Byte.class) {
                return num.byteValue();
            }
            if (targetType == short.class || targetType == Short.class) {
                return num.shortValue();
            }
        }

        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
        }

        if (targetType == char.class || targetType == Character.class) {
            if (value instanceof Character) {
                return value;
            }
            if (value instanceof Number) {
                return (char) ((Number) value).intValue();
            }
        }

        throw new EvaluationException(
                "Cannot convert " + sourceType.getName() + " to " + targetType.getName(),
                location,
                ErrorCode.EVAL_TYPE_MISMATCH
        );
    }


    public static Object convertArray(Object value, Class<?> targetType, SourceLocation location) throws EvaluationException {
        int length = Array.getLength(value);
        Class<?> componentType = targetType.getComponentType();
        Object result = Array.newInstance(componentType, length);

        for (int i = 0; i < length; i++) {
            Object element = Array.get(value, i);
            try {
                Object converted = convertValue(element, componentType, location);
                Array.set(result, i, converted);
            } catch (EvaluationException e) {
                throw new EvaluationException(
                        "Cannot convert element at index " + i + ": " + e.getMessage(),
                        location,
                        ErrorCode.EVAL_TYPE_MISMATCH
                );
            }
        }

        return result;
    }

    public static Class<?> closetCommonSuperClass(Class<?> clazz1, Class<?> clazz2) {
        if (clazz1.isPrimitive() || clazz2.isPrimitive()) {
            return clazz1 == clazz2 ? clazz1 :Object.class;
        }
        while (clazz1 != Object.class && clazz2 != Object.class) {
            if (clazz1.isAssignableFrom(clazz2)) {
                return clazz1;
            }
            if (clazz2.isAssignableFrom(clazz1)) {
                return clazz2;
            }
            clazz1 = clazz1.getSuperclass();
            clazz2 = clazz2.getSuperclass();
        }
        return Object.class;
    }




    public static boolean isFloatingType(Number n) {
        return n instanceof Double || n instanceof Float;
    }


    public static boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        return true;
    }

    public static int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        if (value instanceof Character) return (Character) value;
        return 0;
    }

    public static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    public static float toFloat(Object value) {
        if (value == null) return 0.0f;
        if (value instanceof Number) return ((Number) value).floatValue();
        return 0.0f;
    }

    public static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }

    public static char toChar(Object value) {
        if (value == null) return '\0';
        if (value instanceof Character) return (Character) value;
        if (value instanceof Number) return (char) ((Number) value).intValue();
        return '\0';
    }

    public static byte toByte(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).byteValue();
        return 0;
    }

    public static short toShort(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).shortValue();
        return 0;
    }

}
