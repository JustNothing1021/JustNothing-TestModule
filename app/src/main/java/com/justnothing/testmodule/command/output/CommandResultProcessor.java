package com.justnothing.testmodule.command.output;

import com.justnothing.testmodule.protocol.json.model.ClassInfo;
import com.justnothing.testmodule.protocol.json.model.FieldInfo;
import com.justnothing.testmodule.protocol.json.model.MethodInfo;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 命令结果处理器。
 * 
 * <p>负责将命令执行结果转换为结构化的JSON数据。
 * 支持的命令：
 * <ul>
 *   <li>class info - 返回ClassInfo</li>
 *   <li>class list - 返回类名列表</li>
 *   <li>invoke - 返回InvokeResult</li>
 * </ul>
 * </p>
 */
public class CommandResultProcessor {
    
    private static final Logger logger = Logger.getLoggerForName("CommandResultProcessor");
    
    /**
     * 处理命令结果，尝试转换为结构化数据。
     * 
     * @param command 命令字符串
     * @param output 输出内容
     * @param handler 输出处理器
     */
    public static void processResult(String command, String output, InteractiveOutputHandler handler) {
        if (command == null || command.trim().isEmpty()) {
            handler.println(output);
            return;
        }
        
        String[] parts = command.trim().split("\\s+", 3);
        String mainCommand = parts[0].toLowerCase();
        String subCommand = parts.length > 1 ? parts[1].toLowerCase() : "";
        
        try {
            switch (mainCommand) {
                case "class", "cinfo", "cgraph", "canalyze", "clist", "cinvoke", "cfield", "csearch", "cconstructor", "creflect" -> {
                    if ("info".equals(subCommand) || mainCommand.equals("cinfo")) {
                        processClassInfo(command, output, handler);
                    } else {
                        handler.println(output);
                    }
                }
                case "invoke" -> {
                    processInvokeResult(command, output, handler);
                }
                default -> {
                    handler.println(output);
                }
            }
        } catch (Exception e) {
            logger.warn("处理命令结果失败: " + e.getMessage());
            handler.println(output);
        }
    }
    
    /**
     * 处理 class info 命令结果。
     */
    private static void processClassInfo(String command, String output, InteractiveOutputHandler handler) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length < 3) {
            handler.println(output);
            return;
        }
        
        String className = parts[2];
        
        try {
            Class<?> clazz = ClassResolver.findClassOrFail(className);
            ClassInfo classInfo = ClassInfo.fromClass(clazz);
            
            List<MethodInfo> methods = new ArrayList<>();
            for (Method method : clazz.getDeclaredMethods()) {
                methods.add(MethodInfo.fromMethod(method));
            }
            classInfo.setMethods(methods);
            
            List<MethodInfo> constructors = new ArrayList<>();
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                constructors.add(MethodInfo.fromConstructor(constructor));
            }
            classInfo.setConstructors(constructors);
            
            List<FieldInfo> fields = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                fields.add(FieldInfo.fromField(field));
            }
            classInfo.setFields(fields);
            
            classInfo.setClassLoader(clazz.getClassLoader() != null 
                ? clazz.getClassLoader().toString() 
                : "Bootstrap ClassLoader");
            
            handler.setStructuredResult(classInfo);
            
        } catch (ClassNotFoundException e) {
            handler.println("错误: 类未找到: " + className);
        } catch (Exception e) {
            handler.println(output);
        }
    }
    
    /**
     * 处理 invoke 命令结果。
     */
    private static void processInvokeResult(String command, String output, InteractiveOutputHandler handler) {
        handler.println(output);
    }
}
