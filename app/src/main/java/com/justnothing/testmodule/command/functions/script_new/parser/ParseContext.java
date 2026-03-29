package com.justnothing.testmodule.command.functions.script_new.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParseContext {
    
    private int currentLine;
    private int currentColumn;
    private Class<?> expectedParamType;
    private final Deque<NestingLevel> nestingStack;
    private final List<String> imports;
    private final Set<String> declaredClassNames;
    private ClassLoader classLoader;
    
    public ParseContext() {
        this(Thread.currentThread().getContextClassLoader());
    }
    
    public ParseContext(ClassLoader classLoader) {
        this.currentLine = 1;
        this.currentColumn = 1;
        this.expectedParamType = null;
        this.nestingStack = new ArrayDeque<>();
        this.imports = new ArrayList<>();
        this.declaredClassNames = new HashSet<>();
        this.classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
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
    
    public int getCurrentLine() {
        return currentLine;
    }
    
    public void setCurrentLine(int currentLine) {
        this.currentLine = currentLine;
    }
    
    public int getCurrentColumn() {
        return currentColumn;
    }
    
    public void setCurrentColumn(int currentColumn) {
        this.currentColumn = currentColumn;
    }
    
    public Class<?> getExpectedParamType() {
        return expectedParamType;
    }
    
    public void setExpectedParamType(Class<?> expectedParamType) {
        this.expectedParamType = expectedParamType;
    }
    
    public void clearExpectedParamType() {
        this.expectedParamType = null;
    }
    
    public void pushNestingLevel(NestingLevel level) {
        nestingStack.push(level);
    }
    
    public NestingLevel popNestingLevel() {
        return nestingStack.pop();
    }
    
    public NestingLevel peekNestingLevel() {
        return nestingStack.peek();
    }
    
    public boolean isNestingStackEmpty() {
        return nestingStack.isEmpty();
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
    
    public Set<String> getDeclaredClassNames() {
        return declaredClassNames;
    }
    
    public enum NestingLevel {
        PARENTHESIS,  // 圆括号 ()
        BRACE,        // 花括号 {}
        BRACKET,       // 方括号 []
        LAMBDA,        // Lambda表达式
        METHOD_CALL    // 方法调用
    }
}
