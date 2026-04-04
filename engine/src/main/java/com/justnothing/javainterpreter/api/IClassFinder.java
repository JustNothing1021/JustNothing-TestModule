package com.justnothing.javainterpreter.api;

import java.util.List;

public interface IClassFinder {
    
    Class<?> findClass(String className);
    
    Class<?> findClass(String className, ClassLoader classLoader);
    
    Class<?> findClassWithImports(String className, ClassLoader classLoader, List<String> imports);
    
    Class<?> findClassOrFail(String className) throws ClassNotFoundException;
    
    Class<?> findClassOrFail(String className, ClassLoader classLoader) throws ClassNotFoundException;
    
}
