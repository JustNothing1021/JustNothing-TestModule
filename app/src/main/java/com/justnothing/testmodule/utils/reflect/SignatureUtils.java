package com.justnothing.testmodule.utils.reflect;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SignatureUtils {

    public static Class<?>[] parseSignature(String signature) throws ClassNotFoundException {
        if (signature == null || signature.isEmpty()) {
            return new Class<?>[0];
        }
        String[] parts = signature.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        Class<?>[] types = new Class<?>[parts.length];
        for (int i = 0; i < parts.length; i++) {
            types[i] = ClassResolver.findClassOrFail(parts[i].trim());
        }
        return types;
    }

    public static Class<?>[] parseSignature(String signature, ClassLoader classLoader) throws ClassNotFoundException {
        if (signature == null || signature.isEmpty()) {
            return new Class<?>[0];
        }
        String[] parts = signature.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        Class<?>[] types = new Class<?>[parts.length];
        for (int i = 0; i < parts.length; i++) {
            types[i] = ClassResolver.findClassOrFail(parts[i].trim(), classLoader);
        }
        return types;
    }

    public static Class<?>[] parseSignature(String signature, ClassLoader classLoader, List<String> imports)
            throws ClassNotFoundException {
        if (signature == null || signature.isEmpty()) {
            return new Class<?>[0];
        }
        String[] parts = signature.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        Class<?>[] types = new Class<?>[parts.length];
        for (int i = 0; i < parts.length; i++) {
            types[i] = ClassResolver.findClassWithImportsOrFail(parts[i].trim(), classLoader, imports);
        }
        return types;
    }

    public static String[] splitParams(String paramStr) {
        List<String> params = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inQuotes = false;

        for (char c : paramStr.toCharArray()) {
            if (c == '"' && (current.length() == 0 || current.charAt(current.length() - 1) != '\\')) {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == '(' && !inQuotes) {
                parenDepth++;
                current.append(c);
            } else if (c == ')' && !inQuotes) {
                parenDepth--;
                current.append(c);
            } else if (c == ',' && parenDepth == 0 && !inQuotes) {
                params.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            params.add(current.toString());
        }

        return params.toArray(new String[0]);
    }

    public static String formatSignature(Class<?>[] paramTypes) {
        if (paramTypes == null || paramTypes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramTypes.length; i++) {
            sb.append(paramTypes[i].getName());
            if (i < paramTypes.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public static String formatSignature(Member member) {
        if (!(member instanceof Method method)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(paramTypes[i].getName());
        }
        return sb.toString();
    }

    public static String formatSimpleSignature(Class<?>[] paramTypes) {
        if (paramTypes == null || paramTypes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramTypes.length; i++) {
            sb.append(paramTypes[i].getSimpleName());
            if (i < paramTypes.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public static String formatSimpleSignature(Member member) {
        if (!(member instanceof Method method)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(paramTypes[i].getSimpleName());
        }
        return sb.toString();
    }
}