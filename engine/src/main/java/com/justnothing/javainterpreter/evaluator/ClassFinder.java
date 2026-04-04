package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.api.DefaultClassFinder;
import com.justnothing.javainterpreter.api.IClassFinder;

import java.util.List;

/**
 * 类查找工具
 * <p>
 * 提供类查找功能，封装 ClassResolver
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 * @deprecated 使用 {@link IClassFinder} 接口和 {@link DefaultClassFinder} 实现代替
 */
@Deprecated
public class ClassFinder {
    
    private static final DefaultClassFinder INSTANCE = new DefaultClassFinder();
    
    @Deprecated
    public static IClassFinder getDefault() {
        return INSTANCE;
    }
    
    @Deprecated
    public static Class<?> findClassThroughApi(String className) {
        return INSTANCE.findClass(className);
    }
    
    @Deprecated
    public static Class<?> findClassThroughApi(String className, ClassLoader classLoader) {
        return INSTANCE.findClass(className, classLoader);
    }
    
    @Deprecated
    public static Class<?> findClassWithImports(String className, ClassLoader classLoader, List<String> imports) {
        return INSTANCE.findClassWithImports(className, classLoader, imports);
    }
    
    @Deprecated 
    public static Class<?> findClassOrFail(String className) throws ClassNotFoundException {
        return INSTANCE.findClassOrFail(className);
    }
    
    @Deprecated 
    public static Class<?> findClassOrFail(String className, ClassLoader classLoader) throws ClassNotFoundException {
        return INSTANCE.findClassOrFail(className, classLoader);
    }
}
