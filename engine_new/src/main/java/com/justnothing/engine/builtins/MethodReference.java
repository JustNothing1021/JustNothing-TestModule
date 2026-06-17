package com.justnothing.engine.builtins;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

public class MethodReference {

    private final String methodName;
    private final Function<Object[], Object> refFunc;

    public MethodReference(String methodName, Function<Object[], Object> refFunc) {
        this.methodName = methodName;
        this.refFunc = refFunc;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object invoke(Object... args) {
        return refFunc.apply(args);
    }

    public Function<Object[], Object> asFunction() {
        return refFunc;
    }

    @SuppressWarnings("unchecked")
    public <T> T asInterface(Class<T> fiType) {
        Method sam = Lambda.getSAM(fiType);
        if (sam == null) return (T) refFunc;
        return (T) Proxy.newProxyInstance(
                fiType.getClassLoader(),
                new Class<?>[]{fiType},
                (proxy, method, javaArgs) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return Lambda.handleObjectMethod(proxy, method, javaArgs);
                    }
                    if (!method.equals(sam)) {
                        if (method.isDefault()) {
                            return InvocationHandler.invokeDefault(proxy, method, javaArgs);
                        }
                        throw new UnsupportedOperationException("Not a SAM: " + method);
                    }
                    return refFunc.apply(javaArgs != null ? javaArgs : new Object[0]);
                });
    }
}
