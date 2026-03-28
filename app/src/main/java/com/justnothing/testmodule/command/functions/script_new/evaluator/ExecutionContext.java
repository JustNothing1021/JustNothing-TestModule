package com.justnothing.testmodule.command.functions.script_new.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 执行上下文
 * <p>
 * 在求值AST时维护的上下文信息，包括：
 * - ClassLoader：用于加载类和反射
 * - ScopeManager：管理变量作用域
 * - Imports：导入列表，用于类名解析
 * - 其他执行时需要的状态
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class ExecutionContext {
    
    private final ClassLoader classLoader;
    private final ScopeManager scopeManager;
    private final List<String> imports;
    private int loopDepth;
    private static final int MAX_LOOP_DEPTH = 1000;
    
    public ExecutionContext(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.scopeManager = new ScopeManager();
        this.imports = new ArrayList<>();
        this.loopDepth = 0;
        addDefaultImports();
    }
    
    public ExecutionContext(ClassLoader classLoader, List<String> imports) {
        this.classLoader = classLoader;
        this.scopeManager = new ScopeManager();
        this.imports = imports != null ? new ArrayList<>(imports) : new ArrayList<>();
        this.loopDepth = 0;
        if (this.imports.isEmpty()) {
            addDefaultImports();
        }
    }
    
    private void addDefaultImports() {
        imports.add("java.lang.*");
        imports.add("java.util.*");
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    public ScopeManager getScopeManager() {
        return scopeManager;
    }
    
    public List<String> getImports() {
        return Collections.unmodifiableList(imports);
    }
    
    public int getLoopDepth() {
        return loopDepth;
    }
    
    public void incrementLoopDepth() {
        loopDepth++;
        if (loopDepth > MAX_LOOP_DEPTH) {
            throw new RuntimeException(
                "Maximum loop depth exceeded: " + MAX_LOOP_DEPTH);
        }
    }
    
    public void decrementLoopDepth() {
        if (loopDepth > 0) {
            loopDepth--;
        }
    }
}
