package com.justnothing.testmodule.utils.reflect;

import com.justnothing.testmodule.hooks.XposedBasicHook;
import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class ClassResolver {

    public static final String TAG = "ClassResolver";
    private static final Logger logger = Logger.getLoggerForName(TAG);

    private static final List<ClassLoader> registeredLoaders = new CopyOnWriteArrayList<>();
    private static ClassLoader apkClassLoader = null;
    private static final Object loaderLock = new Object();

    public static void registerClassLoader(ClassLoader loader) {
        if (loader == null) {
            logger.warn("尝试注册null ClassLoader");
            return;
        }

        if (!registeredLoaders.contains(loader)) {
            registeredLoaders.add(loader);
            logger.debug("注册ClassLoader: " + loader);
        }
    }

    public static void registerApkClassLoader(ClassLoader loader) {
        synchronized (loaderLock) {
            apkClassLoader = loader;
            registerClassLoader(loader);
            logger.info("注册APK ClassLoader");
        }
    }

    public static ClassLoader getApkClassLoader() {
        return apkClassLoader;
    }

    public static Class<?> findClass(String className) {
        return findClass(className, null);
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        switch (className) {
            case "int":
                logger.debug("基本类型: int");
                return int.class;
            case "long":
                logger.debug("基本类型: long");
                return long.class;
            case "float":
                logger.debug("基本类型: float");
                return float.class;
            case "double":
                logger.debug("基本类型: double");
                return double.class;
            case "boolean":
                logger.debug("基本类型: boolean");
                return boolean.class;
            case "char":
                logger.debug("基本类型: char");
                return char.class;
            case "byte":
                logger.debug("基本类型: byte");
                return byte.class;
            case "short":
                logger.debug("基本类型: short");
                return short.class;
            case "void":
                logger.debug("基本类型: void");
                return void.class;
        }
        return findClassInternal(className, classLoader);
    }
    

    
    public static Class<?> findClassWithImports(String className, ClassLoader classLoader, List<String> imports) {

        switch (className) {
            case "int":
                logger.debug("基本类型: int");
                return int.class;
            case "long":
                logger.debug("基本类型: long");
                return long.class;
            case "float":
                logger.debug("基本类型: float");
                return float.class;
            case "double":
                logger.debug("基本类型: double");
                return double.class;
            case "boolean":
                logger.debug("基本类型: boolean");
                return boolean.class;
            case "char":
                logger.debug("基本类型: char");
                return char.class;
            case "byte":
                logger.debug("基本类型: byte");
                return byte.class;
            case "short":
                logger.debug("基本类型: short");
                return short.class;
            case "void":
                logger.debug("基本类型: void");
                return void.class;
        }

        Class<?> clazz;
        if (className.contains(".")) {
            logger.debug("尝试完整类名: " + className);
            clazz = findClassInternal(className, classLoader);

            if (clazz != null) {
                logger.debug("通过完整类名找到类: " + clazz.getName());
                return clazz;
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
                logger.debug("通过导入找到类: " + clazz.getName());
                return clazz;
            }
        }
        return null;
    }

    protected static Class<?> findClassInternal(String className, ClassLoader preferredLoader) {
        if (preferredLoader != null) {
            Class<?> clazz = findInLoader(className, preferredLoader);
            if (clazz != null) {
                logger.debug("在首选ClassLoader中找到类: " + className);
                return clazz;
            }
        }

        if (apkClassLoader != null) {
            Class<?> clazz = findInLoader(className, apkClassLoader);
            if (clazz != null) {
                logger.debug("在APK ClassLoader中找到类: " + className);
                return clazz;
            }
        }

        for (ClassLoader loader : registeredLoaders) {
            if (loader == preferredLoader || loader == apkClassLoader) {
                continue;
            }

            Class<?> clazz = findInLoader(className, loader);
            if (clazz != null) {
                logger.debug("在注册的ClassLoader中找到类: " + className);
                return clazz;
            }
        }

        Class<?> clazz = findInLoader(className, null);
        if (clazz != null) {
            logger.debug("在系统ClassLoader中找到类: " + className);
            return clazz;
        }

        logger.debug("未找到类: " + className);
        return null;
    }

    private static Class<?> findInLoader(String className, ClassLoader loader) {
        try {
            return XposedBasicHook.ClassFinder.withCl(loader).find(className);
        } catch (Exception e) {
            logger.debug("在ClassLoader中查找类失败: " + className + ", " + e.getMessage());
            return null;
        }
    }

    public static Method findMethod(String className, String methodName, Object... paramTypes) {
        return findMethod(className, methodName, null, paramTypes);
    }

    public static Method findMethod(String className, String methodName, ClassLoader preferredLoader, Object... paramTypes) {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) {
            logger.debug("未找到类，无法查找方法: " + className + "." + methodName);
            return null;
        }

        try {
            return XposedBasicHook.MethodFinder.withCl(preferredLoader)
                .find(className, methodName, paramTypes);
        } catch (Exception e) {
            logger.debug("查找方法失败: " + className + "." + methodName + ", " + e.getMessage());
            return null;
        }
    }

    public static List<Method> findAllMethods(String className, String methodName) {
        return findAllMethods(className, methodName, null);
    }

    public static List<Method> findAllMethods(String className, String methodName, ClassLoader preferredLoader) {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) {
            logger.debug("未找到类，无法查找方法: " + className + "." + methodName);
            return new ArrayList<>();
        }

        try {
            return XposedBasicHook.MethodFinder.withCl(preferredLoader)
                .findAll(className, methodName);
        } catch (Exception e) {
            logger.debug("查找所有方法失败: " + className + "." + methodName + ", " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static Class<?> findClassOrFail(String className) throws ClassNotFoundException {
        return findClassOrFail(className, null);
    }

    public static Class<?> findClassOrFail(String className, ClassLoader preferredLoader) throws ClassNotFoundException {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) {
            throw new ClassNotFoundException("未找到类: " + className);
        }
        return clazz;
    }

    public static Class<?> findClassWithImportsOrFail(String className, List<String> imports)
                                throws ClassNotFoundException {
        return findClassWithImportsOrFail(className, null, imports);
    }

    public static Class<?> findClassWithImportsOrFail(String className, ClassLoader classLoader, List<String> imports) 
                                throws ClassNotFoundException {
        Class<?> clazz = findClassWithImports(className, classLoader, imports);
        if (clazz == null) {
            throw new ClassNotFoundException("未找到类: " + className);
        }
        return clazz;
    }


    public static Field findStaticField(String className, String fieldName, boolean accessSuper, boolean accessInterfaces) {
        return findStaticField(className, fieldName, null, true, true);
    }

    public static Field findStaticField(String className, String fieldName, ClassLoader classLoader) {
        return findStaticField(className, fieldName, classLoader, true, true);
    }

    public static Field findStaticField(String className, String fieldName,
                                         ClassLoader preferredLoader,
                                        boolean accessSuper, boolean accessInterfaces) {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) {
            logger.debug("未找到类，无法获取静态字段: " + className + "." + fieldName);
            return null;
        }

        try {
            return findStaticField(clazz, fieldName, accessSuper, accessInterfaces);
        } catch (Throwable e) {
            logger.debug("使用反射获取静态字段失败: " + className + "." + fieldName + ", " + e.getMessage());
            return null;
        }
    }

    public static Field findStaticField(Class<?> clazz, String fieldName,
                                        boolean accessSuper, boolean accessInterfaces) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                int modifiers = field.getModifiers();
                
                if (!Modifier.isStatic(modifiers)) {
                    current = current.getSuperclass();
                    continue;
                }
                
                return field;
            } catch (NoSuchFieldException e) {
                if (!accessSuper) break;
                current = current.getSuperclass();
            }
        }
        if (accessInterfaces) {
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> _interface : interfaces) {
                try {
                    return _interface.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        return null;
    }

    public static Field findStaticFieldOrFail(Class<?> clazz, String fieldName,
                    boolean accessSuper, boolean accessInterfaces) throws NoSuchFieldException {
        Field field = findStaticField(clazz, fieldName, accessSuper, accessInterfaces);
        if (field == null) {
            throw new NoSuchFieldException("找不到字段: " + clazz.getName() + "." + fieldName);
        }
        return field;
    }

    public static Object getStaticFieldInternal(Class<?> clazz, String fieldName,
             boolean accessSuper, boolean accessInterfaces) throws IllegalAccessException {
        Field field = findStaticField(clazz, fieldName, accessSuper, accessInterfaces);
        field.setAccessible(true);
        return field.get(null);
    }
}
