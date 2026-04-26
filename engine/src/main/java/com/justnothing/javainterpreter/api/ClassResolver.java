package com.justnothing.javainterpreter.api;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClassResolver {

    private static final List<ClassLoader> registeredLoaders = new CopyOnWriteArrayList<>();
    private static ClassLoader primaryClassLoader = null;
    private static final Object loaderLock = new Object();

    public static void registerClassLoader(ClassLoader loader) {
        if (loader == null) return;
        if (!registeredLoaders.contains(loader)) {
            registeredLoaders.add(loader);
        }
    }

    public static void setPrimaryClassLoader(ClassLoader loader) {
        synchronized (loaderLock) {
            primaryClassLoader = loader;
            registerClassLoader(loader);
        }
    }

    public static ClassLoader getPrimaryClassLoader() {
        return primaryClassLoader;
    }

    public static boolean isTypeCompatible(Class<?> expected, Class<?> actual) {
        if (expected == Void.class && actual == Void.class) return true;
        if (actual == Void.class) return !expected.isPrimitive();
        if (expected.isPrimitive()) return isPrimitiveWrapperMatch(expected, actual);
        return expected.isAssignableFrom(actual);
    }

    private static boolean isPrimitiveWrapperMatch(Class<?> primitive, Class<?> wrapper) {
        if (primitive == int.class) return wrapper == Integer.class;
        if (primitive == long.class) return wrapper == Long.class;
        if (primitive == float.class) return wrapper == Float.class;
        if (primitive == double.class) return wrapper == Double.class;
        if (primitive == boolean.class) return wrapper == Boolean.class;
        if (primitive == char.class) return wrapper == Character.class;
        if (primitive == byte.class) return wrapper == Byte.class;
        if (primitive == short.class) return wrapper == Short.class;
        return false;
    }

    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, List<Class<?>> usingArgTypes, boolean isVarArgs) {
        if (isVarArgs) {
            if (methodArgsTypes.length == 0) return false;
            Class<?> varArgsType = methodArgsTypes[methodArgsTypes.length - 1];
            if (varArgsType.isArray()) {
                Class<?> varArgsComponentType = varArgsType.getComponentType();
                int fixedParamCount = methodArgsTypes.length - 1;
                if (usingArgTypes.size() < fixedParamCount) return false;
                for (int i = 0; i < fixedParamCount; i++) {
                    if (!isTypeCompatible(methodArgsTypes[i], usingArgTypes.get(i))) return false;
                }
                for (int i = fixedParamCount; i < usingArgTypes.size(); i++) {
                    if (!isTypeCompatible(varArgsComponentType, usingArgTypes.get(i))) return false;
                }
                return true;
            }
        }
        if (methodArgsTypes.length != usingArgTypes.size()) return false;
        for (int i = 0; i < methodArgsTypes.length; i++) {
            if (!isTypeCompatible(methodArgsTypes[i], usingArgTypes.get(i))) return false;
        }
        return true;
    }

    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, Class<?>[] usingArgTypes, boolean isVarArgs) {
        return isApplicableArgs(methodArgsTypes, Arrays.asList(usingArgTypes), isVarArgs);
    }

    public static Class<?> findClass(String className) {
        return findClass(className, null);
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        switch (className) {
            case "int" -> { return int.class; }
            case "long" -> { return long.class; }
            case "float" -> { return float.class; }
            case "double" -> { return double.class; }
            case "boolean" -> { return boolean.class; }
            case "char" -> { return char.class; }
            case "byte" -> { return byte.class; }
            case "short" -> { return short.class; }
            case "void" -> { return void.class; }
        }
        
        Class<?> clazz = findClassInternal(className, classLoader);
        if (clazz != null) {
            return clazz;
        }
        
        if (className.contains(".")) {
            String[] parts = className.split("\\.");
            if (parts.length >= 2) {
                for (int i = parts.length - 1; i >= 1; i--) {
                    StringBuilder nestedClassName = new StringBuilder(parts[0]);
                    for (int j = 1; j < parts.length; j++) {
                        if (j < i) {
                            nestedClassName.append('.').append(parts[j]);
                        } else {
                            nestedClassName.append('$').append(parts[j]);
                        }
                    }
                    clazz = findClassInternal(nestedClassName.toString(), classLoader);
                    if (clazz != null) {
                        return clazz;
                    }
                }
            }
        }
        
        return null;
    }

    public static Class<?> findClassWithImports(String className, ClassLoader classLoader, List<String> imports) {

        switch (className) {
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "void":
                return void.class;
        }

        Class<?> clazz;
        if (className.contains(".")) {
            clazz = findClassInternal(className, classLoader);

            if (clazz != null) {
                return clazz;
            }

            String[] parts = className.split("\\.");
            if (parts.length >= 2) {
                for (int i = parts.length - 1; i >= 1; i--) {
                    StringBuilder nestedClassName = new StringBuilder(parts[0]);
                    for (int j = 1; j < parts.length; j++) {
                        if (j < i) {
                            nestedClassName.append('.').append(parts[j]);
                        } else {
                            nestedClassName.append('$').append(parts[j]);
                        }
                    }
                    clazz = findClassInternal(nestedClassName.toString(), classLoader);

                    if (clazz != null) {
                        return clazz;
                    }
                }
            }
        }

        for (String importStmt : imports) {
            String fullClassName;
            if (importStmt.endsWith(".*")) {
                String packageName = importStmt.substring(0, importStmt.length() - 2);
                fullClassName = packageName + "." + className;
            } else {
                fullClassName = importStmt;
                String lastName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
                if (!lastName.equals(className)) {
                    continue;
                }
            }
            clazz = findClassInternal(fullClassName, classLoader);
            if (clazz != null) {
                return clazz;
            }

            String[] parts = fullClassName.split("\\.");
            if (parts.length >= 2) {
                for (int i = parts.length - 1; i >= 1; i--) {
                    StringBuilder nestedClassName = new StringBuilder(parts[0]);
                    for (int j = 1; j < parts.length; j++) {
                        if (j < i) {
                            nestedClassName.append('.').append(parts[j]);
                        } else {
                            nestedClassName.append('$').append(parts[j]);
                        }
                    }
                    clazz = findClassInternal(nestedClassName.toString(), classLoader);

                    if (clazz != null) {
                        return clazz;
                    }
                }
            }
        }
        return null;
    }

    protected static Class<?> findClassInternal(String className, ClassLoader preferredLoader) {
        if (preferredLoader != null) {
            Class<?> clazz = findInLoader(className, preferredLoader);
            if (clazz != null) return clazz;
        }
        if (primaryClassLoader != null) {
            Class<?> clazz = findInLoader(className, primaryClassLoader);
            if (clazz != null) return clazz;
        }
        for (ClassLoader loader : registeredLoaders) {
            if (loader == preferredLoader || loader == primaryClassLoader) continue;
            Class<?> clazz = findInLoader(className, loader);
            if (clazz != null) return clazz;
        }
        return findInLoader(className, null);
    }

    private static Class<?> findInLoader(String className, ClassLoader loader) {
        try {
            if (loader != null) return Class.forName(className, false, loader);
            else return Class.forName(className);
        } catch (Exception e) {
            return null;
        }
    }

    public static Method findMethod(String className, String methodName, Object... paramTypes) {
        return findMethod(className, methodName, null, paramTypes);
    }

    public static Method findMethod(String className, String methodName, ClassLoader preferredLoader, Object... paramTypes) {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) return null;
        Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName) && isApplicableArgs(m.getParameterTypes(), (Class<?>[]) paramTypes, m.isVarArgs()))
                return m;
        }
        return null;
    }

    public static List<Method> findAllMethods(String className, String methodName) {
        return findAllMethods(className, methodName, null);
    }

    public static List<Method> findAllMethods(String className, String methodName, ClassLoader preferredLoader) {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) return new ArrayList<>();
        Method[] methods = clazz.getDeclaredMethods();
        List<Method> result = new ArrayList<>();
        for (Method m : methods) {
            if (m.getName().equals(methodName)) result.add(m);
        }
        return result;
    }

    public static Class<?> findClassOrFail(String className) throws ClassNotFoundException {
        return findClassOrFail(className, null);
    }

    public static Class<?> findClassOrFail(String className, ClassLoader preferredLoader) throws ClassNotFoundException {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) throw new ClassNotFoundException("Class not found: " + className);
        return clazz;
    }

    public static Class<?> findClassWithImportsOrFail(String className, ClassLoader classLoader, List<String> imports) throws ClassNotFoundException {
        Class<?> clazz = findClassWithImports(className, classLoader, imports);
        if (clazz == null) throw new ClassNotFoundException("Class not found: " + className);
        return clazz;
    }

    public static Field findStaticField(String className, String fieldName, ClassLoader classLoader) {
        return findStaticField(className, fieldName, classLoader, true, true);
    }

    public static Field findStaticField(String className, String fieldName, ClassLoader preferredLoader, boolean accessSuper, boolean accessInterfaces) {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) return null;
        return findStaticField(clazz, fieldName, accessSuper, accessInterfaces);
    }

    public static Field findStaticField(Class<?> clazz, String fieldName, boolean accessSuper, boolean accessInterfaces) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (Modifier.isStatic(field.getModifiers())) return field;
                current = current.getSuperclass();
            } catch (NoSuchFieldException e) {
                if (!accessSuper) break;
                current = current.getSuperclass();
            }
        }
        if (accessInterfaces) {
            for (Class<?> _interface : clazz.getInterfaces()) {
                try {
                    return _interface.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {}
            }
        }
        return null;
    }
}
