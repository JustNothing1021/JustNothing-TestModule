package com.justnothing.testmodule.hooks;


import com.justnothing.testmodule.utils.logging.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;


public class HookAPI {
    public static final String TAG = "HookAPI";
    private static final Logger logger = Logger.getLoggerForName(TAG);

    public static XC_MethodHook.Unhook findAndHookMethod(
            Class<?> clazz,
            String methodName,
            Object... parameterTypesAndHook
    ) {
        logger.info("尝试Hook方法: " + clazz.getName() + "." + methodName);
        return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndHook);
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            String className,
            ClassLoader cl,
            String methodName,
            Object... parameterTypesAndHook
    ) {
        logger.info("尝试Hook方法: " + className + "." + methodName + ", 类加载器: " + cl);
        return XposedHelpers.findAndHookMethod(className, cl, methodName, parameterTypesAndHook);
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(
            Class<?> clazz,
            Object... parameterTypesAndHook
    ) {
        logger.info("尝试Hook构造函数: " + clazz.getName());
        return XposedHelpers.findAndHookConstructor(clazz, parameterTypesAndHook);
    }

    public static Class<?> findClassIfExists(String className, ClassLoader cl) {
        logger.info("尝试查找类: " + className + ", 类加载器: " + cl);
        Class<?> result = XposedHelpers.findClassIfExists(className, cl);
        if (result == null) {
            logger.warn("类未找到: " + className + ", 类加载器: " + cl);
        }
        return result;
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            logger.info("尝试查找方法: " + clazz.getName() + "." + methodName);
            return XposedHelpers.findMethodExact(clazz, methodName, parameterTypes);
        } catch (NoSuchMethodError e) {
            logger.warn("方法未找到: " + clazz.getName() + "." + methodName);
            throw new NoSuchMethodException(clazz.getName() + "." + methodName);
        }
    }

    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            logger.info("尝试查找方法: " + clazz.getName() + "." + methodName);
            return XposedHelpers.findMethodExact(clazz, methodName, parameterTypes);
        } catch (NoSuchMethodError e) {
            logger.warn("方法未找到: " + clazz.getName() + "." + methodName);
            return null;
        }
    }


    public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
        try {
            logger.info("尝试查找字段: " + clazz.getName() + "." + fieldName);
            return XposedHelpers.findFieldIfExists(clazz, fieldName);
        } catch (NoSuchFieldError e) {
            logger.warn("字段未找到: " + clazz.getName() + "." + fieldName);
            return null;
        }
    }


    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) throws NoSuchFieldException {
        try {
            logger.info("尝试设置静态字段: " + clazz.getName() + "." + fieldName);
            XposedHelpers.setStaticObjectField(clazz, fieldName, value);
        } catch (NoSuchFieldError e) {
            logger.warn("字段未找到: " + clazz.getName() + "." + fieldName);
            throw new NoSuchFieldException(clazz.getName() + "." + fieldName);
        }
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            logger.info("尝试获取静态字段: " + clazz.getName() + "." + fieldName);
            return XposedHelpers.getStaticObjectField(clazz, fieldName);
        } catch (NoSuchFieldError e) {
            logger.warn("字段未找到: " + clazz.getName() + "." + fieldName);
            throw new NoSuchFieldException(clazz.getName() + "." + fieldName);
        }
    }

    public static void setObjectField(Object object, String fieldName, Object value) throws NoSuchFieldException {
        try {
            logger.info("尝试设置字段: " + object.getClass().getName() + "." + fieldName);
            XposedHelpers.setObjectField(object, fieldName, value);
        } catch (NoSuchFieldError e) {
            logger.warn("字段未找到: " + object.getClass().getName() + "." + fieldName);
            throw new NoSuchFieldException(object.getClass().getName() + "." + fieldName);
        }
    }

    public static Object getObjectField(Object object, String fieldName) throws NoSuchFieldException {
        try {
            logger.info("尝试获取字段: " + object.getClass().getName() + "." + fieldName);
            return XposedHelpers.getObjectField(object, fieldName);
        } catch (NoSuchFieldError e) {
            logger.warn("字段未找到: " + object.getClass().getName() + "." + fieldName);
            throw new NoSuchFieldException(object.getClass().getName() + "." + fieldName);
        }
    }
}
