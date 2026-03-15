package com.justnothing.testmodule.command.functions.script_new.evaluator;

/**
 * 执行上下文
 * <p>
 * 在求值AST时维护的上下文信息，包括：
 * - ClassLoader：用于加载类和反射
 * - ScopeManager：管理变量作用域
 * - 其他执行时需要的状态
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class ExecutionContext {
    
    private final ClassLoader classLoader;
    private final ScopeManager scopeManager;
    private int loopDepth;
    private static final int MAX_LOOP_DEPTH = 1000;
    
    public ExecutionContext(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.scopeManager = new ScopeManager();
        this.loopDepth = 0;
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    public ScopeManager getScopeManager() {
        return scopeManager;
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
