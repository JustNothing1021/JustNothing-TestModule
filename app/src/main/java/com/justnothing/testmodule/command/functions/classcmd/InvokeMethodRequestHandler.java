package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.protocol.json.handler.RequestHandler;
import com.justnothing.testmodule.protocol.json.request.InvokeMethodRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.InvokeMethodResult;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class InvokeMethodRequestHandler implements RequestHandler<InvokeMethodRequest, InvokeMethodResult> {
    
    private static final Logger logger = Logger.getLoggerForName("InvokeMethodRequestHandler");
    
    @Override
    public String getCommandType() {
        return "InvokeMethod";
    }
    
    @Override
    public InvokeMethodRequest parseRequest(JSONObject obj) {
        try {
            return new InvokeMethodRequest().fromJson(obj);
        } catch (org.json.JSONException e) {
            logger.error("解析请求失败", e);
            return null;
        }
    }
    
    @Override
    public InvokeMethodResult createResult(String requestId) {
        return new InvokeMethodResult(requestId);
    }
    
    @Override
    public InvokeMethodResult handle(InvokeMethodRequest request) {
        String className = request.getClassName();
        String methodName = request.getMethodName();
        String signature = request.getSignature();
        String targetInstanceExpr = request.getTargetInstance();
        List<String> paramExpressions = request.getParams();
        List<String> paramTypeHints = request.getParamTypes();
        boolean freeMode = request.isFreeMode();
        boolean isStatic = request.isStatic();
        
        logger.debug("处理方法调用请求: " + className + "." + methodName + ", 签名: " + signature);
        
        InvokeMethodResult result = new InvokeMethodResult(request.getRequestId());
        
        if (className == null || className.isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "类名不能为空"));
            return result;
        }
        
        if (methodName == null || methodName.isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "方法名不能为空"));
            return result;
        }
        
        ClassLoader classLoader = null;
        try {
            Class<?> targetClass = ClassResolver.findClassOrFail(className);
            classLoader = targetClass.getClassLoader();
            
            Object targetInstance = null;
            if (!isStatic) {
                if (targetInstanceExpr == null || targetInstanceExpr.isEmpty()) {
                    result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "实例方法需要提供目标实例表达式"));
                    return result;
                }
                try {
                    ExpressionParser.ParseResult instanceResult = ExpressionParser.parse(targetInstanceExpr, classLoader);
                    targetInstance = instanceResult.value();
                    if (targetInstance == null) {
                        result.setError(new CommandResult.ErrorInfo("NULL_INSTANCE", "目标实例为 null"));
                        return result;
                    }
                    logger.debug("目标实例: " + targetInstance);
                } catch (Exception e) {
                    result.setError(new CommandResult.ErrorInfo("PARSE_ERROR", 
                        "解析目标实例失败: " + targetInstanceExpr + " - " + e.getMessage()));
                    return result;
                }
            }
            
            Method targetMethod = null;
            Class<?>[] expectedParamTypes = null;
            
            if (!freeMode && signature != null && !signature.isEmpty()) {
                targetMethod = findMethodBySignature(targetClass, methodName, signature);
                if (targetMethod != null) {
                    expectedParamTypes = targetMethod.getParameterTypes();
                    logger.debug("非自由模式: 找到目标方法 " + targetMethod + ", 期望参数类型: " + java.util.Arrays.toString(expectedParamTypes));
                }
            }
            
            List<Object> params = new ArrayList<>();
            List<Class<?>> paramTypes = new ArrayList<>();
            List<String> imports = new ArrayList<>();
            imports.add(className);
            for (int i = 0; i < paramExpressions.size(); i++) {
                String expr = paramExpressions.get(i);
                String typeHint = (paramTypeHints != null && i < paramTypeHints.size()) ? paramTypeHints.get(i) : null;
                Class<?> expectedType = null;
                
                if (!freeMode && expectedParamTypes != null && i < expectedParamTypes.length) {
                    expectedType = expectedParamTypes[i];
                }
                
                try {
                    ExpressionParser.ParseResult parseResult;
                    
                    if (typeHint != null && !typeHint.isEmpty()) {
                        Class<?> hintClass = ClassResolver.findClassOrFail(typeHint, classLoader);
                        parseResult = ExpressionParser.parse(expr, classLoader, hintClass, imports);
                        logger.debug("参数" + i + " (类型提示=" + typeHint + "): " + parseResult.value());
                    } else if (expectedType != null) {
                        parseResult = ExpressionParser.parse(expr, classLoader, expectedType, imports);
                        logger.debug("参数" + i + " (期望类型=" + expectedType.getName() + "): " + parseResult.value() + " (" + parseResult.type().getName() + ")");
                    } else {
                        parseResult = ExpressionParser.parse(expr, classLoader);
                        logger.debug("参数" + i + " (自动推断): " + parseResult.value() + " (" + parseResult.type().getName() + ")");
                    }
                    
                    params.add(parseResult.value());
                    paramTypes.add(parseResult.type());
                } catch (Exception e) {
                    result.setError(new CommandResult.ErrorInfo("PARSE_ERROR", 
                        "解析参数" + i + "失败: " + expr + " - " + e.getMessage()));
                    return result;
                }
            }
            
            Method method = targetMethod;
            if (method == null) {
                for (Method m : targetClass.getDeclaredMethods()) {
                    if (m.getName().equals(methodName) && 
                        ClassResolver.isApplicableArgs(m.getParameterTypes(), paramTypes, m.isVarArgs())) {
                        method = m;
                        break;
                    }
                }
            }
            
            if (method == null) {
                Class<?> superClass = targetClass.getSuperclass();
                while (superClass != null && method == null) {
                    for (Method m : superClass.getDeclaredMethods()) {
                        if (m.getName().equals(methodName) && 
                            ClassResolver.isApplicableArgs(m.getParameterTypes(), paramTypes, m.isVarArgs())) {
                            method = m;
                            break;
                        }
                    }
                    superClass = superClass.getSuperclass();
                }
            }
            
            if (method == null) {
                StringBuilder availableMethods = new StringBuilder("可用的方法:\n");
                for (Method m : targetClass.getDeclaredMethods()) {
                    if (m.getName().equals(methodName)) {
                        availableMethods.append("  ").append(m).append("\n");
                    }
                }
                result.setError(new CommandResult.ErrorInfo("METHOD_NOT_FOUND", 
                    "未找到匹配的方法，参数类型: " + paramTypes + "\n" + availableMethods));
                return result;
            }
            
            logger.debug("找到方法: " + method);
            
            method.setAccessible(true);
            
            Object[] invokeArgs = prepareMethodArguments(method, params);
            
            Object returnValue;
            if (isStatic) {
                returnValue = method.invoke(null, invokeArgs);
            } else {
                returnValue = method.invoke(targetInstance, invokeArgs);
            }
            
            result.setSuccess(true);
            if (returnValue != null) {
                result.setResultString(String.valueOf(returnValue));
                result.setResultTypeName(returnValue.getClass().getName());
                result.setResultHash(System.identityHashCode(returnValue));
            } else {
                result.setResultString("null");
                result.setResultTypeName(method.getReturnType().getName());
                result.setResultHash(0);
            }
            
            if (!isStatic && targetInstance != null) {
                result.setInstanceAfterInvocation(String.valueOf(targetInstance));
                result.setInstanceHash(System.identityHashCode(targetInstance));
            }
            
            logger.info("方法调用成功: " + methodName + " -> " + returnValue);
            
        } catch (ClassNotFoundException e) {
            logger.error("类未找到: " + className, e);
            result.setError(new CommandResult.ErrorInfo("CLASS_NOT_FOUND", "类未找到: " + className));
        } catch (Exception e) {
            logger.error("方法调用失败: " + className + "." + methodName, e);
            Throwable cause = e.getCause();
            String errorMsg = cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : e.getMessage();
            result.setError(new CommandResult.ErrorInfo("INVOCATION_ERROR", "调用失败: " + errorMsg));
        } finally {
            if (classLoader != null) {
                ExpressionParser.clearVariables(classLoader);
            }
        }
        
        return result;
    }
    
    private Method findMethodBySignature(Class<?> targetClass, String methodName, String signature) {
        for (Method m : targetClass.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                String methodSig = buildMethodSignature(m);
                if (methodSig.equals(signature)) {
                    return m;
                }
            }
        }
        
        Class<?> superClass = targetClass.getSuperclass();
        while (superClass != null) {
            for (Method m : superClass.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    String methodSig = buildMethodSignature(m);
                    if (methodSig.equals(signature)) {
                        return m;
                    }
                }
            }
            superClass = superClass.getSuperclass();
        }
        
        return null;
    }
    
    private String buildMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(paramTypes[i].getName());
        }
        sb.append(")");
        return sb.toString();
    }
    
    private Object[] prepareMethodArguments(Method method, List<Object> args) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        if (!method.isVarArgs() || paramTypes.length == 0) {
            return args.toArray();
        }
        
        int fixedParamCount = paramTypes.length - 1;
        Class<?> varArgsType = paramTypes[fixedParamCount];
        Class<?> varArgsComponentType = varArgsType.getComponentType();
        
        if (args.size() < fixedParamCount) {
            throw new IllegalArgumentException("参数不足");
        }
        
        Object[] invokeArgs = new Object[paramTypes.length];
        
        for (int i = 0; i < fixedParamCount; i++) {
            invokeArgs[i] = args.get(i);
        }
        
        int varArgCount = args.size() - fixedParamCount;

        assert varArgsComponentType != null;
        if (varArgCount == 1) {
            Object lastArg = args.get(fixedParamCount);
            
            if (lastArg != null && lastArg.getClass().isArray() &&
                    ReflectionUtils.isTypeCompatible(varArgsComponentType, lastArg.getClass().getComponentType())) {
                invokeArgs[fixedParamCount] = lastArg;
            } else {
                Object varArgArray = Array.newInstance(varArgsComponentType, 1);
                Array.set(varArgArray, 0, lastArg);
                invokeArgs[fixedParamCount] = varArgArray;
            }
        } else if (varArgCount == 0) {
            invokeArgs[fixedParamCount] = Array.newInstance(varArgsComponentType, 0);
        } else {
            Object varArgArray = Array.newInstance(varArgsComponentType, varArgCount);
            for (int i = 0; i < varArgCount; i++) {
                Array.set(varArgArray, i, args.get(fixedParamCount + i));
            }
            invokeArgs[fixedParamCount] = varArgArray;
        }
        
        return invokeArgs;
    }
    
}
