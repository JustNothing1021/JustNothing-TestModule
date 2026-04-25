package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.protocol.json.handler.RequestHandler;
import com.justnothing.testmodule.protocol.json.model.ClassInfo;
import com.justnothing.testmodule.protocol.json.model.FieldInfo;
import com.justnothing.testmodule.protocol.json.model.MethodInfo;
import com.justnothing.testmodule.protocol.json.request.ClassInfoRequest;
import com.justnothing.testmodule.protocol.json.response.ClassInfoResult;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ClassInfoRequestHandler implements RequestHandler<ClassInfoRequest, ClassInfoResult> {
    
    private static final Logger logger = Logger.getLoggerForName("ClassInfoRequestHandler");
    
    @Override
    public String getCommandType() {
        return "ClassInfo";
    }
    
    @Override
    public ClassInfoRequest parseRequest(JSONObject obj) {
        return new ClassInfoRequest().fromJson(obj);
    }

    @Override
    public ClassInfoResult createResult(String requestId) {
        return new ClassInfoResult(requestId);
    }
    
    @Override
    public ClassInfoResult handle(ClassInfoRequest request) {
        String className = request.getClassName();
        logger.debug("处理类信息请求: " + className);
        
        if (className == null || className.isEmpty()) {
            logger.error("类名不能为空");
            ClassInfoResult result = new ClassInfoResult(request.getRequestId());
            result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "类名不能为空"));
            return result;
        }
        
        ClassInfoResult result = new ClassInfoResult(request.getRequestId());
        
        try {
            logger.debug("加载类: " + className);
            Class<?> clazz = ClassResolver.findClassOrFail(className);
            ClassInfo classInfo = ClassInfo.fromClass(clazz);
            
            if (request.isShowMethods()) {
                List<MethodInfo> methods = new ArrayList<>();
                for (Method method : clazz.getDeclaredMethods()) {
                    methods.add(MethodInfo.fromMethod(method));
                }
                classInfo.setMethods(methods);
                logger.debug("添加方法: " + methods.size());
            }
            
            if (request.isShowConstructors()) {
                List<MethodInfo> constructors = new ArrayList<>();
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    constructors.add(MethodInfo.fromConstructor(constructor));
                }
                classInfo.setConstructors(constructors);
                logger.debug("添加构造函数: " + constructors.size());
            }
            
            if (request.isShowFields()) {
                List<FieldInfo> fields = new ArrayList<>();
                for (Field field : clazz.getDeclaredFields()) {
                    fields.add(FieldInfo.fromField(field));
                }
                classInfo.setFields(fields);
                logger.debug("添加字段: " + fields.size());
            }
            
            if (request.isShowSuper() && clazz.getSuperclass() != null) {
                classInfo.setSuperClass(clazz.getSuperclass().getName());
            }
            
            if (request.isShowInterfaces()) {
                List<String> interfaces = new ArrayList<>();
                for (Class<?> iface : clazz.getInterfaces()) {
                    interfaces.add(iface.getName());
                }
                classInfo.setInterfaces(interfaces);
            }
            
            classInfo.setClassLoader(clazz.getClassLoader() != null 
                ? clazz.getClassLoader().toString() 
                : "Bootstrap ClassLoader");
            
            result.setClassInfo(classInfo);
            logger.info("类信息查询成功: " + className);
            
        } catch (ClassNotFoundException e) {
            logger.error("类未找到: " + className, e);
            result.setError(new CommandResult.ErrorInfo("CLASS_NOT_FOUND", "类未找到: " + className));
        } catch (Exception e) {
            logger.error("处理类信息失败: " + className, e);
            result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "处理类信息失败: " + e.getMessage()));
        }
        
        return result;
    }
}
