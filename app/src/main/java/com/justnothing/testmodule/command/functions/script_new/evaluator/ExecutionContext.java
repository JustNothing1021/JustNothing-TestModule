package com.justnothing.testmodule.command.functions.script_new.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutionContext {
    
    private final ClassLoader classLoader;
    private final ScopeManager scopeManager;
    private final List<String> imports;
    private final Map<String, Class<?>> customClasses;
    private final Builtins builtins;
    private int loopDepth;
    private static final int MAX_LOOP_DEPTH = 1000;
    
    public ExecutionContext(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.scopeManager = new ScopeManager();
        this.imports = new ArrayList<>();
        this.customClasses = new HashMap<>();
        this.builtins = new Builtins();
        this.loopDepth = 0;
        addDefaultImports();
    }
    
    public ExecutionContext(ClassLoader classLoader, List<String> imports) {
        this.classLoader = classLoader;
        this.scopeManager = new ScopeManager();
        this.imports = imports != null ? new ArrayList<>(imports) : new ArrayList<>();
        this.customClasses = new HashMap<>();
        this.builtins = new Builtins();
        this.loopDepth = 0;
        if (this.imports.isEmpty()) {
            addDefaultImports();
        }
    }
    
    public ExecutionContext(ExecutionContext parent) {
        this.classLoader = parent.classLoader;
        this.scopeManager = new ScopeManager(parent.scopeManager);
        this.imports = new ArrayList<>(parent.imports);
        this.customClasses = new HashMap<>(parent.customClasses);
        this.builtins = parent.builtins;
        this.loopDepth = parent.loopDepth;
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
    
    public void registerCustomClass(String className, Class<?> clazz) {
        customClasses.put(className, clazz);
    }
    
    public Class<?> getCustomClass(String className) {
        return customClasses.get(className);
    }
    
    public boolean hasCustomClass(String className) {
        return customClasses.containsKey(className);
    }
    
    public Map<String, Class<?>> getCustomClasses() {
        return Collections.unmodifiableMap(customClasses);
    }
    
    public Builtins getBuiltins() {
        return builtins;
    }
    
    public boolean hasBuiltin(String name) {
        return builtins.hasFunction(name);
    }
    
    public Builtins.BuiltinFunction getBuiltin(String name) {
        return builtins.getFunction(name);
    }
    
    public Object callBuiltin(String name, List<Object> args) {
        Builtins.BuiltinFunction func = builtins.getFunction(name);
        if (func == null) {
            throw new RuntimeException("Unknown builtin function: " + name);
        }
        return func.call(args);
    }
}
