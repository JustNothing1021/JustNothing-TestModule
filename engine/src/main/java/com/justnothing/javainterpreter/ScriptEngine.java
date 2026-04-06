package com.justnothing.javainterpreter;

import com.justnothing.javainterpreter.api.DefaultOutputHandler;
import com.justnothing.javainterpreter.api.IClassFinder;
import com.justnothing.javainterpreter.api.IOutputHandler;

import java.util.Map;

public class ScriptEngine {
    
    private final ScriptRunner runner;
    private final IOutputHandler outputHandler;
    private final IOutputHandler errorHandler;
    
    public ScriptEngine() {
        this(Thread.currentThread().getContextClassLoader());
    }
    
    public ScriptEngine(ClassLoader classLoader) {
        this.outputHandler = new DefaultOutputHandler();
        this.errorHandler = new DefaultOutputHandler();
        this.runner = new ScriptRunner(classLoader, outputHandler, errorHandler);
    }
    
    public ScriptEngine(ClassLoader classLoader, IOutputHandler outputHandler, IOutputHandler errorHandler) {
        this.outputHandler = outputHandler;
        this.errorHandler = errorHandler;
        this.runner = new ScriptRunner(classLoader, outputHandler, errorHandler);
    }
    
    public Object execute(String code) {
        return runner.executeWithResult(code);
    }
    
    public Object execute(String code, IOutputHandler output, IOutputHandler error) {
        return runner.executeWithResult(code, output, error);
    }
    
    public void setVariable(String name, Object value) {
        runner.setVariable(name, value);
    }
    
    public Object getVariable(String name) {
        return runner.getVariable(name);
    }
    
    public boolean hasVariable(String name) {
        return runner.hasVariable(name);
    }
    
    public void deleteVariable(String name) {
        runner.deleteVariable(name);
    }
    
    public Map<String, Object> getAllVariables() {
        return runner.getAllVariablesAsObject();
    }
    
    public void clearVariables() {
        runner.clearVariables();
    }
    
    public void addImport(String importStmt) {
        runner.addImport(importStmt);
    }
    
    public void setClassFinder(IClassFinder classFinder) {
        runner.setClassFinder(classFinder);
    }
    
    public IClassFinder getClassFinder() {
        return runner.getClassFinder();
    }
    
    public String getOutput() {
        return runner.getOutput();
    }
    
    public String getErrors() {
        return runner.getWarnMessages();
    }
    
    public void clearOutput() {
        outputHandler.clear();
        errorHandler.clear();
    }
    
    public ScriptRunner getRunner() {
        return runner;
    }
    
    public static ScriptEngine create() {
        return new ScriptEngine();
    }
    
    public static ScriptEngine create(ClassLoader classLoader) {
        return new ScriptEngine(classLoader);
    }
    
    public static Object eval(String code) {
        return new ScriptEngine().execute(code);
    }
    
    public static Object eval(String code, ClassLoader classLoader) {
        return new ScriptEngine(classLoader).execute(code);
    }
}
