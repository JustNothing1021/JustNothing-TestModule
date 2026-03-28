package com.justnothing.testmodule.command.functions.script_new.evaluator;

import com.justnothing.testmodule.utils.reflect.ClassResolver;

/**
 * 类查找工具
 * <p>
 * 提供类查找功能，封装 ClassResolver
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class ClassFinder {
    
    /**
     * 通过API查找类
     * 
     * @param className 类名
     * @return Class对象，如果找不到返回null
     */
    public static Class<?> findClassThroughApi(String className) {
        return ClassResolver.findClass(className);
    }
    
    /**
     * 通过API查找类（指定ClassLoader）
     * 
     * @param className 类名
     * @param classLoader 类加载器
     * @return Class对象，如果找不到返回null
     */
    public static Class<?> findClassThroughApi(String className, ClassLoader classLoader) {
        return ClassResolver.findClass(className, classLoader);
    }
    
    /**
     * 通过API查找类（带导入）
     * 
     * @param className 类名
     * @param classLoader 类加载器
     * @param imports 导入列表
     * @return Class对象，如果找不到返回null
     */
    public static Class<?> findClassWithImports(String className, ClassLoader classLoader, java.util.List<String> imports) {
        return ClassResolver.findClassWithImports(className, classLoader, imports);
    }
    
    /**
     * 通过API查找类，找不到抛出异常
     * 
     * @param className 类名
     * @return Class对象
     * @throws ClassNotFoundException 如果找不到类
     */
    public static Class<?> findClassOrFail(String className) throws ClassNotFoundException {
        return ClassResolver.findClassOrFail(className);
    }
    
    /**
     * 通过API查找类（指定ClassLoader），找不到抛出异常
     * 
     * @param className 类名
     * @param classLoader 类加载器
     * @return Class对象
     * @throws ClassNotFoundException 如果找不到类
     */
    public static Class<?> findClassOrFail(String className, ClassLoader classLoader) throws ClassNotFoundException {
        return ClassResolver.findClassOrFail(className, classLoader);
    }
}
