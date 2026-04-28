package com.justnothing.javainterpreter.builtins;


import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.exception.ErrorCode;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.utils.TypeUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class MethodReference {
    private final Class<?> targetClass;
    private final Object targetInstance;
    private final String methodName;
    private final boolean isStatic;
    private final boolean isUnboundInstanceMethod;
    private final ASTNode sourceNode;
    
    public MethodReference(Class<?> targetClass, Object targetInstance, String methodName, boolean isStatic, ASTNode sourceNode) {
        this.targetClass = targetClass;
        this.targetInstance = targetInstance;
        this.methodName = methodName;
        this.isStatic = isStatic;
        this.isUnboundInstanceMethod = false;
        this.sourceNode = sourceNode;
    }
    
    public static MethodReference createUnboundInstanceMethod(Class<?> targetClass, String methodName, ASTNode sourceNode) {
        return new MethodReference(targetClass, null, methodName, false, true, sourceNode);
    }
    
    private MethodReference(Class<?> targetClass, Object targetInstance, String methodName,
                            boolean isStatic, boolean isUnboundInstanceMethod, ASTNode sourceNode) {
        this.targetClass = targetClass;
        this.targetInstance = targetInstance;
        this.methodName = methodName;
        this.isStatic = isStatic;
        this.isUnboundInstanceMethod = isUnboundInstanceMethod;
        this.sourceNode = sourceNode;
    }
    
    public Class<?> getTargetClass() {
        return targetClass;
    }
    
    public Object getTargetInstance() {
        return targetInstance;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public boolean isStatic() {
        return isStatic;
    }
    
    public boolean isUnboundInstanceMethod() {
        return isUnboundInstanceMethod;
    }
    
    public Object invoke(Object... args) throws EvaluationException {
        try {
            if (isUnboundInstanceMethod) {
                if (args == null || args.length == 0) {
                    throw new EvaluationException(
                        "Unbound instance method reference requires target instance as first argument",
                        ErrorCode.EVAL_INVALID_OPERATION,
                        sourceNode
                    );
                }
                Object instance = args[0];
                if (instance == null) {
                    throw new EvaluationException(
                        "Cannot invoke method on null instance",
                        ErrorCode.METHOD_INVOCATION_TARGET_NULL,
                        sourceNode
                    );
                }
                Object[] methodArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new Object[0];
                Method method = findMethod(instance.getClass(), methodArgs);
                method.setAccessible(true);
                return method.invoke(instance, methodArgs);
            }
            
            Method method = findMethod(targetClass, args);
            method.setAccessible(true);
            
            if (isStatic) {
                return method.invoke(null, args);
            } else {
                return method.invoke(targetInstance, args);
            }
        } catch (EvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to invoke method reference: " + methodName + " - " + e.getMessage(),
                ErrorCode.METHOD_NOT_FOUND,
                sourceNode
            );
        }
    }
    
    private Method findMethod(Class<?> clazz, Object[] args) throws NoSuchMethodException {
        Class<?>[] argTypes = new Class<?>[args != null ? args.length : 0];
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
        }
        
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == argTypes.length) {
                    if (argTypes.length == 0) {
                        return method;
                    }
                    boolean match = true;
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (argTypes[i] != null && !TypeUtils.isAssignable(paramTypes[i], argTypes[i])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return method;
                    }
                }
            }
        }
        
        throw new NoSuchMethodException(methodName + Arrays.toString(argTypes));
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
            new MethodReferenceInvocationHandler(this, interfaceClass)
        );
    }
    
    public static class MethodReferenceInvocationHandler implements InvocationHandler {
        private final MethodReference methodReference;
        private final Class<?> interfaceClass;

        public MethodReferenceInvocationHandler(MethodReference methodReference) {
            this(methodReference, null);
        }

        public MethodReferenceInvocationHandler(MethodReference methodReference, Class<?> interfaceClass) {
            this.methodReference = methodReference;
            this.interfaceClass = interfaceClass;
        }
        
        public MethodReference getMethodReference() {
            return methodReference;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("toString")) {
                    return methodReference.toString();
                }
                if (method.getName().equals("hashCode")) {
                    return System.identityHashCode(proxy);
                }
            }

            Object[] invokeArgs = args != null ? args : new Object[0];
            return methodReference.invoke(invokeArgs);
        }
    }
    
        
        @Override
        public String toString() {
        String type;
        if (isUnboundInstanceMethod) {
            type = "unbound ";
        } else if (isStatic) {
            type = "static ";
        } else {
            type = "bound=" + targetInstance + " ";
        }
        return "MethodReference[" + type + targetClass.getSimpleName() + "::" + methodName + "]";
    }
}
