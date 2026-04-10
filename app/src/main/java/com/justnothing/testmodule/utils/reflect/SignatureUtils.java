package com.justnothing.testmodule.utils.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignatureUtils {

    private static final Map<String, Class<?>> PRIMITIVE_TYPES = new HashMap<>();
    
    static {
        PRIMITIVE_TYPES.put("B", byte.class);
        PRIMITIVE_TYPES.put("C", char.class);
        PRIMITIVE_TYPES.put("D", double.class);
        PRIMITIVE_TYPES.put("F", float.class);
        PRIMITIVE_TYPES.put("I", int.class);
        PRIMITIVE_TYPES.put("J", long.class);
        PRIMITIVE_TYPES.put("S", short.class);
        PRIMITIVE_TYPES.put("Z", boolean.class);
        PRIMITIVE_TYPES.put("V", void.class);
    }


    public static Class<?>[] parseParamList(String signature) throws ClassNotFoundException {
        if (signature == null || signature.isEmpty()) {
            return new Class<?>[0];
        }
        if (signature.startsWith("(") && !signature.endsWith(")")) return parseJVMMethodParamList(signature);
        else return parseReadableParamList(signature);
    }

    public static Class<?>[] parseParamList(String signature, ClassLoader cl) throws ClassNotFoundException {
        if (signature == null || signature.isEmpty()) {
            return new Class<?>[0];
        }
        if (signature.endsWith(")V")) return parseJVMMethodParamList(signature);
        else return parseReadableParamList(signature, cl);
    }

    public static Class<?>[] parseReadableParamList(String signature) throws ClassNotFoundException {
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

    private static Class<?>[] parseReadableParamList(String signature, ClassLoader classLoader) throws ClassNotFoundException {
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

    private static Class<?>[] parseReadableParamList(String signature, ClassLoader classLoader, List<String> imports)
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

    public static String formatReadableParamList(Member member) {
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

    public static Class<?>[] parseJVMMethodParamList(String jvmSignature) throws ClassNotFoundException {
        if (jvmSignature == null || jvmSignature.isEmpty()) {
            return new Class<?>[0];
        }

        int paramStart = jvmSignature.indexOf('(');
        int paramEnd = jvmSignature.indexOf(')');

        if (paramStart == -1 || paramEnd == -1 || paramEnd <= paramStart + 1) {
            return new Class<?>[0];
        }

        String paramPart = jvmSignature.substring(paramStart + 1, paramEnd);
        if (paramPart.isEmpty()) {
            return new Class<?>[0];
        }

        List<Class<?>> paramTypes = new ArrayList<>();
        int i = 0;
        while (i < paramPart.length()) {
            int arrayDepth = 0;
            while (i < paramPart.length() && paramPart.charAt(i) == '[') {
                arrayDepth++;
                i++;
            }

            String typeCode;
            if (i >= paramPart.length()) {
                break;
            }

            char c = paramPart.charAt(i);
            if (c == 'L') {
                int semiColonIndex = paramPart.indexOf(';', i);
                if (semiColonIndex == -1) {
                    break;
                }
                typeCode = paramPart.substring(i + 1, semiColonIndex);
                i = semiColonIndex + 1;
            } else {
                typeCode = String.valueOf(c);
                i++;
            }

            Class<?> clazz = PRIMITIVE_TYPES.get(typeCode);
            if (clazz == null && typeCode.startsWith("L")) {
                String className = typeCode.replace('/', '.');
                try {
                    clazz = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    try {
                        clazz = ClassResolver.findClassOrFail(className);
                    } catch (Exception ex) {
                        throw new ClassNotFoundException("无法找到类: " + className);
                    }
                }
            }

            if (clazz != null) {
                for (int d = 0; d < arrayDepth; d++) {
                    clazz = Array.newInstance(clazz, 0).getClass();
                }
                paramTypes.add(clazz);
            }
        }

        return paramTypes.toArray(new Class<?>[0]);
    }
}