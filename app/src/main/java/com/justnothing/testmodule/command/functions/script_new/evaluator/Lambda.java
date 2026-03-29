package com.justnothing.testmodule.command.functions.script_new.evaluator;

import com.justnothing.testmodule.command.functions.script_new.ast.nodes.LambdaNode;
import com.justnothing.testmodule.command.functions.script_new.exception.ErrorCode;
import com.justnothing.testmodule.command.functions.script_new.exception.EvaluationException;
import com.justnothing.testmodule.command.functions.script_new.exception.ReturnException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Lambda implements Function<Object[], Object> {
    private final LambdaNode lambdaNode;
    private final ExecutionContext closureContext;
    private final List<String> parameterNames;
    private final Map<String, ScopeManager.Variable> capturedVariables;
    
    public Lambda(LambdaNode lambdaNode, ExecutionContext closureContext) {
        this.lambdaNode = lambdaNode;
        this.closureContext = closureContext;
        this.parameterNames = new java.util.ArrayList<>();
        for (LambdaNode.Parameter param : lambdaNode.getParameters()) {
            this.parameterNames.add(param.getName());
        }
        this.capturedVariables = new HashMap<>();
        captureVariables();
    }
    
    private void captureVariables() {
        ScopeManager scopeManager = closureContext.getScopeManager();
        for (int level = 1; level <= scopeManager.getCurrentLevel(); level++) {
            Map<String, ScopeManager.Variable> vars = scopeManager.getVariablesAtLevel(level);
            if (vars != null) {
                for (Map.Entry<String, ScopeManager.Variable> entry : vars.entrySet()) {
                    if (!capturedVariables.containsKey(entry.getKey())) {
                        capturedVariables.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }
    
    public LambdaNode getLambdaNode() {
        return lambdaNode;
    }
    
    public ExecutionContext getClosureContext() {
        return closureContext;
    }
    
    public List<String> getParameterNames() {
        return parameterNames;
    }
    
    public int getParameterCount() {
        return parameterNames.size();
    }
    
    public Object call(Object... args) throws EvaluationException {
        return invoke(args);
    }
    
    @Override
    public Object apply(Object[] args) {
        return invoke(args);
    }
    
    public Object invoke(Object[] args) throws EvaluationException {
        if (args != null && args.length != parameterNames.size()) {
            throw new EvaluationException(
                "Lambda expects " + parameterNames.size() + " arguments but got " + args.length,
                lambdaNode.getLocation(),
                ErrorCode.METHOD_INVALID_ARGUMENTS
            );
        }
        
        ExecutionContext callContext = new ExecutionContext(closureContext);
        
        callContext.getScopeManager().enterScope();
        
        try {
            for (Map.Entry<String, ScopeManager.Variable> entry : capturedVariables.entrySet()) {
                String varName = entry.getKey();
                if (!parameterNames.contains(varName)) {
                    ScopeManager.Variable var = entry.getValue();
                    callContext.getScopeManager().declareCapturedVariable(
                        var.getName(), var.getType(), var, var.isFinal()
                    );
                }
            }
            
            for (int i = 0; i < parameterNames.size(); i++) {
                Object argValue = args != null && i < args.length ? args[i] : null;
                callContext.getScopeManager().declareVariable(
                    parameterNames.get(i),
                    argValue != null ? argValue.getClass() : Object.class,
                    argValue,
                    false
                );
            }
            
            return ASTEvaluator.evaluate(lambdaNode.getBody(), callContext);
        } catch (ReturnException e) {
            return e.getValue();
        } finally {
            callContext.getScopeManager().exitScope();
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T asInterface(Class<T> interfaceClass) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Target must be an interface: " + interfaceClass.getName());
        }
        
        ClassLoader classLoader = interfaceClass.getClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        
        return (T) Proxy.newProxyInstance(
            classLoader,
            new Class<?>[] { interfaceClass },
            new LambdaInvocationHandler(this)
        );
    }
    
    public static class LambdaInvocationHandler implements InvocationHandler {
        private final Lambda lambda;
        
        LambdaInvocationHandler(Lambda lambda) {
            this.lambda = lambda;
        }
        
        public Lambda getLambda() {
            return lambda;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("toString") && method.getParameterCount() == 0) {
                return lambda.toString();
            }
            if (method.getName().equals("hashCode") && method.getParameterCount() == 0) {
                return System.identityHashCode(lambda);
            }
            if (method.getName().equals("equals") && method.getParameterCount() == 1) {
                return proxy == args[0];
            }
            
            Object[] invokeArgs = args != null ? args : new Object[0];
            return lambda.invoke(invokeArgs);
        }
    }
    
    @Override
    public String toString() {
        return "Lambda(params=" + parameterNames + ")";
    }
}
