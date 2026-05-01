package com.justnothing.testmodule.command.handlers.classcmd;

import com.justnothing.testmodule.command.functions.classcmd.util.ExpressionParser;
import com.justnothing.testmodule.command.proxy.RequestHandler;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeConstructorRequest;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeConstructorResult;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class InvokeConstructorRequestHandler implements RequestHandler<InvokeConstructorRequest, InvokeConstructorResult> {
    
    private static final Logger logger = Logger.getLoggerForName("InvokeConstructorRequestHandler");
    
    @Override
    public String getCommandType() {
        return "InvokeConstructor";
    }
    
    @Override
    public InvokeConstructorRequest parseRequest(JSONObject obj) {
        try {
            return new InvokeConstructorRequest().fromJson(obj);
        } catch (org.json.JSONException e) {
            logger.error("解析请求失败", e);
            return null;
        }
    }
    
    @Override
    public InvokeConstructorResult createResult(String requestId) {
        return new InvokeConstructorResult(requestId);
    }
    
    @Override
    public InvokeConstructorResult handle(InvokeConstructorRequest request) {
        String className = request.getClassName();
        String signature = request.getSignature();
        List<String> paramExpressions = request.getParams();
        List<String> paramTypeHints = request.getParamTypes();
        boolean freeMode = request.isFreeMode();
        
        logger.debug("处理构造函数调用请求: " + className + ", 签名: " + signature);
        
        InvokeConstructorResult result = new InvokeConstructorResult(request.getRequestId());
        
        if (className == null || className.isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "类名不能为空"));
            return result;
        }
        
        ClassLoader classLoader = null;
        try {
            Class<?> targetClass = ClassResolver.findClassOrFail(className);
            classLoader = targetClass.getClassLoader();
            
            Constructor<?> targetConstructor = null;
            Class<?>[] expectedParamTypes = null;
            
            if (!freeMode && signature != null && !signature.isEmpty()) {
                targetConstructor = findConstructorBySignature(targetClass, signature);
                if (targetConstructor != null) {
                    expectedParamTypes = targetConstructor.getParameterTypes();
                    logger.debug("非自由模式: 找到目标构造函数 " + targetConstructor + ", 期望参数类型: " + java.util.Arrays.toString(expectedParamTypes));
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
            
            Constructor<?> constructor = targetConstructor;
            if (constructor == null) {
                constructor = ReflectionUtils.findConstructor(targetClass, paramTypes.toArray(new Class<?>[0]));
            }
            
            if (constructor == null) {
                StringBuilder availableConstructors = new StringBuilder("可用的构造函数:\n");
                for (Constructor<?> c : targetClass.getDeclaredConstructors()) {
                    availableConstructors.append("  ").append(c).append("\n");
                }
                result.setError(new CommandResult.ErrorInfo("CONSTRUCTOR_NOT_FOUND", 
                    "未找到匹配的构造函数，参数类型: " + paramTypes + "\n" + availableConstructors));
                return result;
            }
            
            logger.debug("找到构造函数: " + constructor);
            
            constructor.setAccessible(true);
            
            Object[] invokeArgs = prepareConstructorArguments(constructor, params);
            
            Object instance = constructor.newInstance(invokeArgs);
            
            result.setSuccess(true);
            result.setResultString(String.valueOf(instance));
            result.setResultTypeName(instance.getClass().getName());
            result.setResultHash(System.identityHashCode(instance));
            
            logger.info("构造函数调用成功: " + instance);
            
        } catch (ClassNotFoundException e) {
            logger.error("类未找到: " + className, e);
            result.setError(new CommandResult.ErrorInfo("CLASS_NOT_FOUND", "类未找到: " + className));
        } catch (Exception e) {
            logger.error("构造函数调用失败: " + className, e);
            result.setError(new CommandResult.ErrorInfo("INVOCATION_ERROR", "调用失败: " + e.getMessage()));
        } finally {
            if (classLoader != null) {
                ExpressionParser.clearVariables(classLoader);
            }
        }
        
        return result;
    }
    
    private Constructor<?> findConstructorBySignature(Class<?> targetClass, String signature) {
        for (Constructor<?> c : targetClass.getDeclaredConstructors()) {
            String constructorSig = buildConstructorSignature(c);
            if (constructorSig.equals(signature)) {
                return c;
            }
        }
        return null;
    }
    
    private String buildConstructorSignature(Constructor<?> constructor) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        Class<?>[] paramTypes = constructor.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(paramTypes[i].getName());
        }
        sb.append(")");
        return sb.toString();
    }
    
    private Object[] prepareConstructorArguments(Constructor<?> constructor, List<Object> args) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        
        if (!constructor.isVarArgs() || paramTypes.length == 0) {
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
