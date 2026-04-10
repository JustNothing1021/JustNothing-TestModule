package com.justnothing.testmodule.utils.reflect;

import com.justnothing.javainterpreter.api.IClassFinder;

import java.util.List;

public class AppClassFinder implements IClassFinder {
    
    @Override
    public Class<?> findClass(String className) {
        return ClassResolver.findClass(className);
    }
    
    @Override
    public Class<?> findClass(String className, ClassLoader classLoader) {
        return ClassResolver.findClass(className, classLoader);
    }
    
    @Override
    public Class<?> findClassWithImports(String className, ClassLoader classLoader, List<String> imports) {
        return ClassResolver.findClassWithImports(className, classLoader, imports);
    }
    
    @Override
    public Class<?> findClassOrFail(String className) throws ClassNotFoundException {
        return ClassResolver.findClassOrFail(className);
    }
    
    @Override
    public Class<?> findClassOrFail(String className, ClassLoader classLoader) throws ClassNotFoundException {
        return ClassResolver.findClassOrFail(className, classLoader);
    }
}
