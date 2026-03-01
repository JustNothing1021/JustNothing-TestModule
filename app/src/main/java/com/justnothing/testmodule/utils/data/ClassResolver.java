package com.justnothing.testmodule.utils.data;

import com.justnothing.testmodule.hooks.XposedBasicHook;
import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.robv.android.xposed.XposedHelpers;


public class ClassResolver {

    private static final class ResolverLogger extends Logger {
        @Override
        public String getTag() {
            return "ClassResolver";
        }
    }

    private static final ResolverLogger logger = new ResolverLogger();

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

    public static Class<?> findClass(String className, ClassLoader preferredLoader) {
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
        Class<?> clazz = findClass(className, preferredLoader);
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
        Class<?> clazz = findClass(className, preferredLoader);
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
        Class<?> clazz = findClass(className, preferredLoader);
        if (clazz == null) {
            throw new ClassNotFoundException("未找到类: " + className);
        }
        return clazz;
    }

    public static Object getStaticField(String className, String fieldName) {
        return getStaticField(className, fieldName, null);
    }

    public static Object getStaticField(String className, String fieldName, ClassLoader preferredLoader) {
        Class<?> clazz = findClass(className, preferredLoader);
        if (clazz == null) {
            logger.debug("未找到类，无法获取静态字段: " + className + "." + fieldName);
            return null;
        }

        try {
            return XposedHelpers.getStaticObjectField(clazz, fieldName);
        } catch (Throwable e) {
            logger.debug("使用Xposed API获取静态字段失败: " + className + "." + fieldName + ", " + e.getMessage());
            
            try {
                return findStaticField(clazz, fieldName);
            } catch (Exception e2) {
                logger.debug("使用反射获取静态字段失败: " + className + "." + fieldName + ", " + e2.getMessage());
                return null;
            }
        }
    }

    private static Object findStaticField(Class<?> clazz, String fieldName) throws Exception {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                int modifiers = field.getModifiers();
                
                if (!Modifier.isStatic(modifiers)) {
                    current = current.getSuperclass();
                    continue;
                }
                
                try {
                    field.setAccessible(true);
                } catch (SecurityException e) {
                    logger.debug("无法设置字段可访问: " + current.getName() + "." + fieldName + ", " + e.getMessage());
                }
                
                try {
                    return field.get(null);
                } catch (IllegalAccessException e) {
                    logger.debug("无法访问静态字段: " + current.getName() + "." + fieldName + ", " + e.getMessage());
                    throw e;
                }
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
