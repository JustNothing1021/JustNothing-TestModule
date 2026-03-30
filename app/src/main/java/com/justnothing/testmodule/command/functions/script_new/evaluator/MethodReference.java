package com.justnothing.testmodule.command.functions.script_new.evaluator;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.exception.ErrorCode;
import com.justnothing.testmodule.command.functions.script_new.exception.EvaluationException;

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
    private final SourceLocation location;
    
    public MethodReference(Class<?> targetClass, Object targetInstance, String methodName, boolean isStatic, SourceLocation location) {
        this.targetClass = targetClass;
        this.targetInstance = targetInstance;
        this.methodName = methodName;
        this.isStatic = isStatic;
        this.isUnboundInstanceMethod = false;
        this.location = location;
    }
    
    public static MethodReference createUnboundInstanceMethod(Class<?> targetClass, String methodName, SourceLocation location) {
        MethodReference ref = new MethodReference(targetClass, null, methodName, false, location);
        return new MethodReference(targetClass, null, methodName, false, true, location);
    }
    
    private MethodReference(Class<?> targetClass, Object targetInstance, String methodName, boolean isStatic, boolean isUnboundInstanceMethod, SourceLocation location) {
        this.targetClass = targetClass;
        this.targetInstance = targetInstance;
        this.methodName = methodName;
        this.isStatic = isStatic;
        this.isUnboundInstanceMethod = isUnboundInstanceMethod;
        this.location = location;
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
                        location,
                        ErrorCode.EVAL_INVALID_OPERATION
                    );
                }
                Object instance = args[0];
                if (instance == null) {
                    throw new EvaluationException(
                        "Cannot invoke method on null instance",
                        location,
                        ErrorCode.EVAL_NULL_POINTER
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
                location,
                ErrorCode.METHOD_NOT_FOUND
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
                        if (argTypes[i] != null && !isAssignable(paramTypes[i], argTypes[i])) {
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
    
    private boolean isAssignable(Class<?> targetType, Class<?> sourceType) {
        if (targetType.isPrimitive()) {
            if (targetType == int.class) return sourceType == Integer.class || sourceType == int.class;
            if (targetType == long.class) return sourceType == Long.class || sourceType == long.class;
            if (targetType == double.class) return sourceType == Double.class || sourceType == double.class;
            if (targetType == float.class) return sourceType == Float.class || sourceType == float.class;
            if (targetType == boolean.class) return sourceType == Boolean.class || sourceType == boolean.class;
            if (targetType == char.class) return sourceType == Character.class || sourceType == char.class;
            if (targetType == byte.class) return sourceType == Byte.class || sourceType == byte.class;
            if (targetType == short.class) return sourceType == Short.class || sourceType == short.class;
        }
        return targetType.isAssignableFrom(sourceType);
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
            new MethodReferenceInvocationHandler(this)
        );
    }
    
    public static class MethodReferenceInvocationHandler implements InvocationHandler {
        private final MethodReference methodReference;
        
        public MethodReferenceInvocationHandler(MethodReference methodReference) {
            this.methodReference = methodReference;
        }
        
        public MethodReference getMethodReference() {
            return methodReference;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("toString") && method.getParameterCount() == 0) {
                return methodReference.toString();
            }
            if (method.getName().equals("hashCode") && method.getParameterCount() == 0) {
                return System.identityHashCode(methodReference);
            }
            if (method.getName().equals("equals") && method.getParameterCount() == 1) {
                return proxy == args[0];
            }
            
            Object[] invokeArgs = args != null ? args : new Object[0];
            return methodReference.invoke(invokeArgs);
        }
    }
    
    @NonNull
    @Override
    public String toString() {
        String type;
        if (isUnboundInstanceMethod) {
            type = "unbound ";
        } else if (isStatic) {
            type = "static ";
        } else {
            type = "";
        }
        return "MethodReference[" + type + targetClass.getSimpleName() + "::" + methodName + "]";
    }
}
