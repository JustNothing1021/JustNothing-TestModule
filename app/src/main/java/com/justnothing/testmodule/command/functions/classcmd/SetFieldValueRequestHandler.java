package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.protocol.json.handler.RequestHandler;
import com.justnothing.testmodule.protocol.json.request.SetFieldValueRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.SetFieldValueResult;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class SetFieldValueRequestHandler implements RequestHandler<SetFieldValueRequest, SetFieldValueResult> {
    
    private static final Logger logger = Logger.getLoggerForName("SetFieldValueRequestHandler");
    
    @Override
    public String getCommandType() {
        return "SetFieldValue";
    }
    
    @Override
    public SetFieldValueRequest parseRequest(JSONObject obj) {
        try {
            return new SetFieldValueRequest().fromJson(obj);
        } catch (org.json.JSONException e) {
            logger.error("解析请求失败", e);
            return null;
        }
    }
    
    @Override
    public SetFieldValueResult createResult(String requestId) {
        return new SetFieldValueResult(requestId);
    }
    
    @Override
    public SetFieldValueResult handle(SetFieldValueRequest request) {
        String className = request.getClassName();
        String fieldName = request.getFieldName();
        String targetInstanceExpr = request.getTargetInstance();
        String valueExpr = request.getValueExpression();
        String valueTypeHint = request.getValueTypeHint();
        boolean isStatic = request.isStatic();
        
        logger.debug("处理设置字段值请求: " + className + "." + fieldName);
        
        SetFieldValueResult result = new SetFieldValueResult(request.getRequestId());
        
        if (className == null || className.isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "类名不能为空"));
            return result;
        }
        
        if (fieldName == null || fieldName.isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "字段名不能为空"));
            return result;
        }
        
        if (valueExpr == null || valueExpr.isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "值表达式不能为空"));
            return result;
        }
        
        ClassLoader classLoader = null;
        try {
            Class<?> targetClass = ClassResolver.findClassOrFail(className);
            classLoader = targetClass.getClassLoader();
            
            Field field = ReflectionUtils.findField(targetClass, fieldName);
            if (field == null) {
                result.setError(new CommandResult.ErrorInfo("FIELD_NOT_FOUND", 
                    "字段未找到: " + fieldName));
                return result;
            }
            
            field.setAccessible(true);
            
            Object targetInstance = null;
            if (!Modifier.isStatic(field.getModifiers())) {
                if (targetInstanceExpr == null || targetInstanceExpr.isEmpty()) {
                    result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "实例字段需要提供目标实例表达式"));
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
            
            Object value;
            try {
                ExpressionParser.ParseResult parseResult;
                if (valueTypeHint != null && !valueTypeHint.isEmpty()) {
                    Class<?> hintClass = resolveType(valueTypeHint, classLoader);
                    parseResult = ExpressionParser.parse(valueExpr, classLoader, hintClass);
                } else {
                    parseResult = ExpressionParser.parse(valueExpr, classLoader, field.getType());
                }
                value = parseResult.value();
                logger.debug("解析值: " + value + " (类型: " + parseResult.type().getName() + ")");
            } catch (Exception e) {
                result.setError(new CommandResult.ErrorInfo("PARSE_ERROR", 
                    "解析值表达式失败: " + valueExpr + " - " + e.getMessage()));
                return result;
            }
            
            field.set(targetInstance, value);
            
            result.setSuccess(true);
            logger.info("字段值设置成功: " + fieldName + " = " + value);
            
        } catch (ClassNotFoundException e) {
            logger.error("类未找到: " + className, e);
            result.setError(new CommandResult.ErrorInfo("CLASS_NOT_FOUND", "类未找到: " + className));
        } catch (Exception e) {
            logger.error("字段值设置失败: " + className + "." + fieldName, e);
            Throwable cause = e.getCause();
            String errorMsg = cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : e.getMessage();
            result.setError(new CommandResult.ErrorInfo("SET_ERROR", "设置失败: " + errorMsg));
        } finally {
            if (classLoader != null) {
                ExpressionParser.clearVariables(classLoader);
            }
        }
        
        return result;
    }
    
    private Class<?> resolveType(String typeName, ClassLoader classLoader) throws ClassNotFoundException {
        switch (typeName) {
            case "int": return int.class;
            case "long": return long.class;
            case "double": return double.class;
            case "float": return float.class;
            case "boolean": return boolean.class;
            case "short": return short.class;
            case "byte": return byte.class;
            case "char": return char.class;
            case "void": return void.class;
            default:
                if (typeName.endsWith("[]")) {
                    String componentType = typeName.substring(0, typeName.length() - 2);
                    Class<?> componentClass = resolveType(componentType, classLoader);
                    return java.lang.reflect.Array.newInstance(componentClass, 0).getClass();
                }
                return Class.forName(typeName, true, classLoader);
        }
    }
}
