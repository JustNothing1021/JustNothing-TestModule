package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.protocol.json.handler.RequestHandler;
import com.justnothing.testmodule.protocol.json.request.ClassHierarchyRequest;
import com.justnothing.testmodule.protocol.json.response.ClassHierarchyResult;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import org.json.JSONObject;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassHierarchyRequestHandler implements RequestHandler<ClassHierarchyRequest, ClassHierarchyResult> {
    
    private static final Logger logger = Logger.getLoggerForName("ClassHierarchyRequestHandler");
    
    @Override
    public String getCommandType() {
        return "ClassHierarchy";
    }
    
    @Override
    public ClassHierarchyRequest parseRequest(JSONObject obj) {
        return new ClassHierarchyRequest().fromJson(obj);
    }
    
    @Override
    public ClassHierarchyResult createResult(String requestId) {
        return new ClassHierarchyResult(requestId);
    }
    
    @Override
    public ClassHierarchyResult handle(ClassHierarchyRequest request) {
        String className = request.getClassName();
        logger.debug("处理类继承图请求: " + className);
        
        ClassHierarchyResult result = new ClassHierarchyResult(request.getRequestId());
        
        if (className == null || className.isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "类名不能为空"));
            return result;
        }
        
        try {
            Class<?> clazz = ClassResolver.findClassOrFail(className);
            
            List<ClassHierarchyResult.HierarchyClassInfo> classChain = new ArrayList<>();
            List<List<String>> interfacesPerLevel = new ArrayList<>();
            
            while (clazz != null) {
                ClassHierarchyResult.HierarchyClassInfo info = new ClassHierarchyResult.HierarchyClassInfo(
                    clazz.getName(),
                    clazz.isInterface(),
                    clazz.isAnnotation(),
                    clazz.isEnum(),
                    Modifier.isAbstract(clazz.getModifiers()),
                    Modifier.isFinal(clazz.getModifiers())
                );
                classChain.add(info);
                
                List<String> interfaces = new ArrayList<>();
                Collections.addAll(interfaces, getClassNames(clazz.getInterfaces()));
                interfacesPerLevel.add(interfaces);
                
                clazz = clazz.getSuperclass();
            }
            
            Collections.reverse(classChain);
            Collections.reverse(interfacesPerLevel);
            
            result.setClassChain(classChain);
            result.setInterfacesPerLevel(interfacesPerLevel);
            
            logger.info("类继承图查询成功: " + className);
            
        } catch (ClassNotFoundException e) {
            logger.error("类未找到: " + className, e);
            result.setError(new CommandResult.ErrorInfo("CLASS_NOT_FOUND", "类未找到: " + className));
        } catch (Exception e) {
            logger.error("处理类继承图失败: " + className, e);
            result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "处理类继承图失败: " + e.getMessage()));
        }
        
        return result;
    }
    
    private String[] getClassNames(Class<?>[] classes) {
        String[] names = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            names[i] = classes[i].getName();
        }
        return names;
    }
}
