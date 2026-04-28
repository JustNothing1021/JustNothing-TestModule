package com.justnothing.javainterpreter.utils;

import com.justnothing.javainterpreter.ast.ASTNode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ArrayUtils {


    private static Object[] castToObjectArray(Object obj) {
        if (obj == null) {
            return new Object[0];
        }
        if (obj.getClass().isArray()) {
            Object[] arr = new Object[Array.getLength(obj)];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = Array.get(obj, i);
            }
            return arr;
        }
        return new Object[] {obj};
    }


    public static Object arrayDifference(Object left, Object right, ASTNode sourceNode) {
        if (left == null || right == null) {
            if (left == null && right == null) {
                return new Object[0];
            }
            return left == null ? right : left;
        }
        if (left.getClass().isArray() && right.getClass().isArray()) {
            Class<?> leftComponentType = left.getClass().getComponentType();
            Class<?> rightComponentType = right.getClass().getComponentType();
            Class<?> resultComponentType = TypeUtils.closetCommonSuperClass(leftComponentType, rightComponentType);
            Object[] leftArr = castToObjectArray(left);
            Object[] rightArr = castToObjectArray(right);
            Object[] result = arrayDifference(leftArr, rightArr);
            return TypeUtils.convertArray(result, resultComponentType, sourceNode);
        }
        return new Object[0];
    }

    private static Object[] arrayDifference(Object[] left, Object[] right) {
        Object[] leftArr = castToObjectArray(left);
        Object[] rightArr = castToObjectArray(right);
        Set<Object> rightSet = new HashSet<>(Arrays.asList(rightArr));
        List<Object> result = new ArrayList<>();
        for (Object obj : leftArr) {
            if (!rightSet.contains(obj)) {
                result.add(obj);
            }
        }
        return result.toArray();
    }



    public static Object arrayConcat(Object left, Object right, ASTNode sourceNode) {
        if (left == null || right == null) {
            if (left == null && right == null) {
                return new Object[0];
            }
            return left == null ? right : left;
        }
        if (left.getClass().isArray() && right.getClass().isArray()) {
            Class<?> leftComponentType = left.getClass().getComponentType();
            Class<?> rightComponentType = right.getClass().getComponentType();
            Class<?> resultComponentType = TypeUtils.closetCommonSuperClass(leftComponentType, rightComponentType);
            Object[] leftArr = castToObjectArray(left);
            Object[] rightArr = castToObjectArray(right);
            Object[] result = arrayConcat(leftArr, rightArr);
            return TypeUtils.convertArray(result, resultComponentType, sourceNode);
        }
        return new Object[0];

    }


    private static Object[] arrayConcat(Object[] left, Object[] right) {
        Object[] result = new Object[Array.getLength(left) + Array.getLength(right)];
        System.arraycopy(left, 0, result, 0, Array.getLength(left));
        System.arraycopy(right, 0, result, Array.getLength(left), Array.getLength(right));
        return result;
    }

    public static Object arrayRepeat(Object arr, int count, ASTNode sourceNode) {
        if (arr == null) {
            return new Object[0];
        }
        if (count <= 0) {
            return new Object[0];
        }
        return TypeUtils.convertArray(arrayRepeat(castToObjectArray(arr), count), arr.getClass().getComponentType(), sourceNode);
    }

    private static Object[] arrayRepeat(Object[] arr, int count) {
        Object[] newArr = new Object[(int) (Array.getLength(arr) * count)];
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < Array.getLength(arr); j++) {
                newArr[i * Array.getLength(arr) + j] = Array.get(arr, j);
            }
        }
        return newArr;
    }

    public static Object arrayIntersection(Object left, Object right, ASTNode sourceNode) {
        if (left == null || right == null) {
            if (left == null && right == null) {
                return new Object[0];
            }
            return left == null ? right : left;
        }
        if (left.getClass().isArray() && right.getClass().isArray()) {
            Class<?> leftComponentType = left.getClass().getComponentType();
            Class<?> rightComponentType = right.getClass().getComponentType();
            Class<?> resultComponentType = TypeUtils.closetCommonSuperClass(leftComponentType, rightComponentType);
            Object[] leftArr = castToObjectArray(left);
            Object[] rightArr = castToObjectArray(right);
            Object[] result = arrayIntersection(leftArr, rightArr);
            return TypeUtils.convertArray(result, resultComponentType, sourceNode);
        }
        return new Object[0];
    }

    private static Object[] arrayIntersection(Object[] left, Object[] right) {
        Set<Object> rightSet = new HashSet<>(Arrays.asList(right));
        List<Object> result = new ArrayList<>();
        Set<Object> seen = new HashSet<>();
        for (Object obj : left) {
            if (rightSet.contains(obj) && !seen.contains(obj)) {
                result.add(obj);
                seen.add(obj);
            }
        }
        return result.toArray();
    }

    public static Object arrayUnion(Object left, Object right, ASTNode sourceNode) {
        if (left == null || right == null) {
            if (left == null && right == null) {
                return new Object[0];
            }
            return left == null ? right : left;
        }
        if (left.getClass().isArray() && right.getClass().isArray()) {
            Class<?> leftComponentType = left.getClass().getComponentType();
            Class<?> rightComponentType = right.getClass().getComponentType();
            Class<?> resultComponentType = TypeUtils.closetCommonSuperClass(leftComponentType, rightComponentType);
            Object[] leftArr = castToObjectArray(left);
            Object[] rightArr = castToObjectArray(right);
            Object[] result = arrayUnion(leftArr, rightArr);
            return TypeUtils.convertArray(result, resultComponentType, sourceNode);
        }
        return new Object[0];
    }

    private static Object[] arrayUnion(Object[] left, Object[] right) {
        Set<Object> resultSet = new LinkedHashSet<>();
        resultSet.addAll(Arrays.asList(left));
        resultSet.addAll(Arrays.asList(right));
        return resultSet.toArray();
    }

    public static Object arrayCartesianProduct(Object left, Object right, ASTNode sourceNode) {
        if (left == null || right == null) {
            if (left == null && right == null) {
                return new Object[0];
            }
            return left == null ? right : left;
        }
        if (left.getClass().isArray() && right.getClass().isArray()) {
            Class<?> leftComponentType = left.getClass().getComponentType();
            Class<?> rightComponentType = right.getClass().getComponentType();
            Class<?> resultComponentType = TypeUtils.closetCommonSuperClass(leftComponentType, rightComponentType);
            Object[] leftArr = castToObjectArray(left);
            Object[] rightArr = castToObjectArray(right);
            Object[] result = arrayCartesianProduct(leftArr, rightArr);
            return TypeUtils.convertArray(result, resultComponentType, sourceNode);
        }
        return new Object[0];
    }

    private static Object[] arrayCartesianProduct(Object[] left, Object[] right) {
        int len1 = left.length;
        int len2 = right.length;
        Object[] result = new Object[len1 * len2];

        int index = 0;
        for (Object o : left) {
            for (Object object : right) {
                Object[] pair = new Object[2];
                pair[0] = o;
                pair[1] = object;
                result[index++] = pair;
            }
        }

        return result;
    }

    public static Object arraySymmetricDifference(Object left, Object right, ASTNode sourceNode) {
        if (left == null || right == null) {
            if (left == null && right == null) {
                return new Object[0];
            }
            return left == null ? right : left;
        }
        if (left.getClass().isArray() && right.getClass().isArray()) {
            Class<?> leftComponentType = left.getClass().getComponentType();
            Class<?> rightComponentType = right.getClass().getComponentType();
            Class<?> resultComponentType = TypeUtils.closetCommonSuperClass(leftComponentType, rightComponentType);
            Object[] leftArr = castToObjectArray(left);
            Object[] rightArr = castToObjectArray(right);
            Object[] result = arraySymmetricDifference(leftArr, rightArr);
            return TypeUtils.convertArray(result, resultComponentType, sourceNode);
        }
        return new Object[0];
    }

    private static Object[] arraySymmetricDifference(Object[] left, Object[] right) {
        Set<Object> leftSet = new HashSet<>(Arrays.asList(left));
        Set<Object> rightSet = new HashSet<>(Arrays.asList(right));
        List<Object> result = new ArrayList<>();
        for (Object obj : left) {
            if (!rightSet.contains(obj)) {
                result.add(obj);
            }
        }
        for (Object obj : right) {
            if (!leftSet.contains(obj)) {
                result.add(obj);
            }
        }
        return result.toArray();
    }


    public static Object arrayReverse(Object arr, ASTNode sourceNode) {
        Class<?> arrComponentType = arr.getClass().getComponentType();
        Object[] result = arrayReverse(castToObjectArray(arr));
        return TypeUtils.convertArray(result, arrComponentType, sourceNode);
    }


    private static Object[] arrayReverse(Object[] arr) {
        Object[] result = new Object[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[arr.length - 1 - i];
        }
        return result;
    }
}
