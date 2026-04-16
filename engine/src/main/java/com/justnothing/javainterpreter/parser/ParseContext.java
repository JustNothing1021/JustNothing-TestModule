package com.justnothing.javainterpreter.parser;

import com.justnothing.javainterpreter.api.DefaultClassFinder;
import com.justnothing.javainterpreter.api.IClassFinder;
import com.justnothing.javainterpreter.api.ClassResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParseContext {
    
    private final List<String> imports;
    private final Set<String> declaredClassNames;
    private ClassLoader classLoader;
    private IClassFinder classFinder;
    
    private String currentClassName;
    private Set<String> currentClassFields;
    private Set<String> currentMethodParams;
    
    public ParseContext() {
        this(Thread.currentThread().getContextClassLoader());
    }
    
    public ParseContext(ClassLoader classLoader) {
        this.imports = new ArrayList<>();
        this.declaredClassNames = new HashSet<>();
        this.classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        this.classFinder = new DefaultClassFinder();
        addDefaultImports();
    }
    
    private void addDefaultImports() {
        imports.add("java.lang.*");
        imports.add("java.util.*");
        imports.add("java.lang.reflect.*");
        imports.add("java.util.function.*");
        imports.add("android.os.*");
        imports.add("android.util.*");
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    public IClassFinder getClassFinder() {
        return classFinder;
    }
    
    public void setClassFinder(IClassFinder classFinder) {
        this.classFinder = classFinder;
    }


    public List<String> getImports() {
        return imports;
    }
    
    public void addImport(String importStmt) {
        if (!imports.contains(importStmt)) {
            imports.add(importStmt);
        }
    }
    
    public void declareClass(String className) {
        declaredClassNames.add(className);
    }
    
    public boolean isClassDeclared(String className) {
        return declaredClassNames.contains(className);
    }
    
    public void enterClass(String className) {
        this.currentClassName = className;
        this.currentClassFields = new HashSet<>();
    }
    
    public void exitClass() {
        this.currentClassName = null;
        this.currentClassFields = null;
    }
    
    public void addField(String fieldName) {
        if (currentClassFields != null) {
            currentClassFields.add(fieldName);
        }
    }
    
    public boolean isFieldOfCurrentClass(String name) {
        return currentClassFields != null && currentClassFields.contains(name);
    }
    
    public void enterMethod(Set<String> paramNames) {
        this.currentMethodParams = paramNames;
    }
    
    public void exitMethod() {
        this.currentMethodParams = null;
    }
    
    public boolean isLocalVariable(String name) {
        return currentMethodParams != null && currentMethodParams.contains(name);
    }
    
    public boolean shouldResolveAsField(String name) {
        return (isFieldOfCurrentClass(name) || isFieldOfParentClass(name)) && !isLocalVariable(name);
    }
    
    private boolean isFieldOfParentClass(String name) {
        // 暂时简化处理，假设所有未在当前类中定义的字段都是父类的字段
        // 后续可以根据实际的类继承关系来实现
        return !isFieldOfCurrentClass(name) && !isLocalVariable(name);
    }
    
    public Class<?> resolveClass(String className) {
        return ClassResolver.findClassWithImports(className, classLoader, imports);
    }
    
    public Class<?> resolveClassOrFail(String className) throws ClassNotFoundException {
        return ClassResolver.findClassWithImportsOrFail(className, classLoader, imports);
    }
}
