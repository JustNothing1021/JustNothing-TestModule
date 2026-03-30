package com.justnothing.testmodule.command.functions.script_new.evaluator;

import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.command.output.SystemOutputCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutionContext {
    
    public static class Variable {
        public Object value;
        public Class<?> type;
        
        public Variable(Object value) {
            this.value = value;
            this.type = value != null ? value.getClass() : Void.class;
        }
        
        public Variable(Object value, Class<?> type) {
            this.value = value;
            this.type = type;
        }
    }
    
    private final ClassLoader classLoader;
    private final ScopeManager scopeManager;
    private final List<String> imports;
    private final Map<String, Class<?>> customClasses;
    private final Builtins builtins;
    private int loopDepth;
    private static final int MAX_LOOP_DEPTH = 1000;
    
    private IOutputHandler outputBuffer;
    private IOutputHandler warnMsgBuffer;
    private boolean printAST = false;

    public ExecutionContext() {
        this(Thread.currentThread().getContextClassLoader());
    }
    
    public ExecutionContext(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.scopeManager = new ScopeManager();
        this.imports = new ArrayList<>();
        this.customClasses = new HashMap<>();
        this.builtins = new Builtins();
        this.loopDepth = 0;
        this.outputBuffer = new SystemOutputCollector(System.out, System.in);
        this.warnMsgBuffer = new SystemOutputCollector(System.err, System.in);
        this.builtins.setOutputHandler(outputBuffer);
        this.builtins.setErrorHandler(warnMsgBuffer);
        this.builtins.setExecutionContext(this);
        addDefaultImports();
    }
    
    public ExecutionContext(ClassLoader classLoader, IOutputHandler outputHandler, IOutputHandler errorHandler) {
        this.classLoader = classLoader;
        this.scopeManager = new ScopeManager();
        this.imports = new ArrayList<>();
        this.customClasses = new HashMap<>();
        this.builtins = new Builtins();
        this.loopDepth = 0;
        this.outputBuffer = outputHandler != null ? outputHandler : new SystemOutputCollector(System.out, System.in);
        this.warnMsgBuffer = errorHandler != null ? errorHandler : new SystemOutputCollector(System.err, System.in);
        this.builtins.setOutputHandler(outputBuffer);
        this.builtins.setErrorHandler(warnMsgBuffer);
        this.builtins.setExecutionContext(this);
        addDefaultImports();
    }
    
    public ExecutionContext(ClassLoader classLoader, List<String> imports) {
        this.classLoader = classLoader;
        this.scopeManager = new ScopeManager();
        this.imports = imports != null ? new ArrayList<>(imports) : new ArrayList<>();
        this.customClasses = new HashMap<>();
        this.builtins = new Builtins();
        this.loopDepth = 0;
        this.outputBuffer = new SystemOutputCollector(System.out, System.in);
        this.warnMsgBuffer = new SystemOutputCollector(System.err, System.in);
        this.builtins.setOutputHandler(outputBuffer);
        this.builtins.setErrorHandler(warnMsgBuffer);
        this.builtins.setExecutionContext(this);
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
        this.outputBuffer = parent.outputBuffer;
        this.warnMsgBuffer = parent.warnMsgBuffer;
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
    
    public ScopeManager getScopeManager() {
        return scopeManager;
    }
    
    public List<String> getImports() {
        return Collections.unmodifiableList(imports);
    }
    
    public void addImport(String importStmt) {
        if (!imports.contains(importStmt)) {
            imports.add(importStmt);
        }
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
    
    public void addBuiltIn(String name, Builtins.BuiltinFunction function) {
        builtins.registerFunction(name, function);
    }
    
    public String getOutput() {
        return outputBuffer.getString();
    }
    
    public void clearOutput() {
        outputBuffer.clear();
    }
    
    public void print(String text) {
        outputBuffer.print(text);
    }
    
    public void printf(String format, Object... args) {
        outputBuffer.printf(format, args);
    }
    
    public void println(String text) {
        outputBuffer.println(text);
    }
    
    public void printStackTrace(Throwable th) {
        outputBuffer.printStackTrace(th);
    }
    
    public String getWarnMessages() {
        return warnMsgBuffer.getString();
    }
    
    public void clearWarnMessages() {
        warnMsgBuffer.clear();
    }
    
    public void printWarn(String text) {
        warnMsgBuffer.print(text);
    }
    
    public void printfWarn(String format, Object... args) {
        warnMsgBuffer.printf(format, args);
    }
    
    public void printlnWarn(String text) {
        warnMsgBuffer.println(text);
    }
    
    public void printStackTraceWarn(Throwable th) {
        warnMsgBuffer.printStackTrace(th);
    }
    
    public IOutputHandler getOutputBuffer() {
        return outputBuffer;
    }
    
    public void setOutputBuffer(IOutputHandler outputBuffer) {
        this.outputBuffer = outputBuffer;
        this.builtins.setOutputHandler(outputBuffer);
    }
    
    public IOutputHandler getWarnMsgBuffer() {
        return warnMsgBuffer;
    }
    
    public void setWarnMsgBuffer(IOutputHandler warnMsgBuffer) {
        this.warnMsgBuffer = warnMsgBuffer;
        this.builtins.setErrorHandler(warnMsgBuffer);
    }
    
    public void setBuiltInOutputBuffer(IOutputHandler stream) {
        this.outputBuffer = stream;
        this.builtins.setOutputHandler(stream);
    }
    
    public void setBuiltInErrorBuffer(IOutputHandler stream) {
        this.warnMsgBuffer = stream;
        this.builtins.setErrorHandler(stream);
    }
    
    public void setVariable(String name, Object value, Class<?> type) {
        scopeManager.declareVariable(name, type, value, false);
    }
    
    public Variable getVariable(String name) {
        ScopeManager.Variable var = scopeManager.getVariable(name);
        return new Variable(var.getValue(), var.getType());
    }
    
    public boolean hasVariable(String name) {
        return scopeManager.hasVariable(name);
    }
    
    public void deleteVariable(String name) {
        scopeManager.deleteVariable(name);
    }
    
    public Map<String, Variable> getAllVariables() {
        Map<String, Variable> result = new HashMap<>();
        Map<String, ScopeManager.Variable> vars = scopeManager.getAllVariablesDetailed();
        for (Map.Entry<String, ScopeManager.Variable> entry : vars.entrySet()) {
            result.put(entry.getKey(), new Variable(entry.getValue().getValue(), entry.getValue().getType()));
        }
        return result;
    }
    
    public void clearVariables() {
        scopeManager.clearCurrentScope();
    }
    
    public boolean isPrintAST() {
        return printAST;
    }
    
    public void setPrintAST(boolean printAST) {
        this.printAST = printAST;
    }
    
    public void enterScope() {
        scopeManager.enterScope();
    }
    
    public void exitScope() {
        scopeManager.exitScope();
    }
}
