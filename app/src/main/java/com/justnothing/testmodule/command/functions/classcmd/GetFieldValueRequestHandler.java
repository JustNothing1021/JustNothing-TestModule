package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.protocol.json.handler.RequestHandler;
import com.justnothing.testmodule.protocol.json.request.GetFieldValueRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.GetFieldValueResult;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class GetFieldValueRequestHandler implements RequestHandler<GetFieldValueRequest, GetFieldValueResult> {
    
    private static final Logger logger = Logger.getLoggerForName("GetFieldValueRequestHandler");
    
    @Override
    public String getCommandType() {
        return "GetFieldValue";
    }
    
    @Override
    public GetFieldValueRequest parseRequest(JSONObject obj) {
        try {
            return new GetFieldValueRequest().fromJson(obj);
        } catch (org.json.JSONException e) {
            logger.error("解析请求失败", e);
            return null;
        }
    }
    
    @Override
    public GetFieldValueResult createResult(String requestId) {
        return new GetFieldValueResult(requestId);
    }
    
    @Override
    public GetFieldValueResult handle(GetFieldValueRequest request) {
        String className = request.getClassName();
        String fieldName = request.getFieldName();
        String targetInstanceExpr = request.getTargetInstance();
        boolean isStatic = request.isStatic();
        
        logger.debug("处理字段值获取请求: " + className + "." + fieldName);
        
        GetFieldValueResult result = new GetFieldValueResult(request.getRequestId());
        
        if (className == null || className.isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "类名不能为空"));
            return result;
        }
        
        if (fieldName == null || fieldName.isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "字段名不能为空"));
            return result;
        }
        
        ClassLoader classLoader = null;
        try {
            Class<?> targetClass = ClassResolver.findClassOrFail(className);
            classLoader = targetClass.getClassLoader();
            
            Field field = null;
            Class<?> searchClass = targetClass;
            while (searchClass != null && field == null) {
                try {
                    field = searchClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    searchClass = searchClass.getSuperclass();
                }
            }
            
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
            
            Object value = field.get(targetInstance);
            
            result.setSuccess(true);
            if (value != null) {
                result.setValueString(String.valueOf(value));
                result.setValueTypeName(value.getClass().getName());
                result.setValueHash(System.identityHashCode(value));
            } else {
                result.setValueString("null");
                result.setValueTypeName(field.getType().getName());
                result.setValueHash(0);
            }
            
            logger.info("字段值获取成功: " + fieldName + " = " + value);
            
        } catch (ClassNotFoundException e) {
            logger.error("类未找到: " + className, e);
            result.setError(new CommandResult.ErrorInfo("CLASS_NOT_FOUND", "类未找到: " + className));
        } catch (Exception e) {
            logger.error("字段值获取失败: " + className + "." + fieldName, e);
            result.setError(new CommandResult.ErrorInfo("ACCESS_ERROR", "访问失败: " + e.getMessage()));
        } finally {
            if (classLoader != null) {
                ExpressionParser.clearVariables(classLoader);
            }
        }
        
        return result;
    }
}
