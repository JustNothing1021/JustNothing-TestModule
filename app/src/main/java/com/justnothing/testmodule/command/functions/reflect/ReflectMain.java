package com.justnothing.testmodule.command.functions.reflect;

import static com.justnothing.testmodule.constants.CommandServer.CMD_REFLECT_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.utils.data.ClassLoaderManager;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class ReflectMain extends CommandBase {


    public ReflectMain() {
        super("Reflect");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: reflect <class> <type> <name> [options] [value]
                
                使用反射访问和操作类的私有成员，研究系统底层逻辑.
                
                参数:
                    class      - 目标类名（完整类名）
                    type       - 操作类型：field, method, constructor, static
                    name       - 成员名称
                
                类型说明:
                    field        - 获取/设置字段值
                    method       - 调用方法
                    constructor  - 创建实例
                    static       - 访问静态成员
                
                选项:
                    -v, --value <value>      设置字段值
                    -p, --params <args>      方法参数（Type:value格式）
                    -s, --super             访问父类成员
                    -i, --interfaces         访问接口成员
                    -r, --raw                原始输出（不格式化）
                
                参数格式说明:
                    Type:value  - 类型和值，例如：Integer:123, String:"hello", Boolean:true
                    支持的类型：Integer, Long, Float, Double, Boolean, String, Character
                
                示例:
                    reflect java.lang.System field out
                    reflect java.lang.System field -v "test" out
                    reflect java.lang.String method valueOf -p String:"123"
                    reflect java.lang.String constructor
                    reflect -s java.lang.String field value
                
                注意:
                    - 可以访问私有成员
                    - 使用-s选项访问父类成员
                    - 使用-i选项访问接口成员
                    - 方法参数必须使用Type:value格式
                    - 字符串值需要用引号包裹
                    - 使用上层命令指定ClassLoader（如：methods -cl android）
                
                (Submodule reflect %s)
                """, CMD_REFLECT_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        if (args.length < 3) {
            return getHelpText();
        }

        String className = args[0];
        String type = args[1];
        String memberName = args[2];
        
        boolean accessSuper = false;
        boolean accessInterfaces = false;
        boolean rawOutput = false;
        String valueToSet = null;
        String[] methodParams = null;
        
        for (int i = 3; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-s") || arg.equals("--super")) {
                accessSuper = true;
            } else if (arg.equals("-i") || arg.equals("--interfaces")) {
                accessInterfaces = true;
            } else if (arg.equals("-r") || arg.equals("--raw")) {
                rawOutput = true;
            } else if (arg.equals("-v") || arg.equals("--value")) {
                if (i + 1 < args.length) {
                    valueToSet = args[++i];
                }
            } else if (arg.equals("-p") || arg.equals("--params")) {
                if (i + 1 < args.length) {
                    String paramsStr = args[++i];
                    methodParams = parseParams(paramsStr);
                }
            }
        }
        
        ClassLoader classLoader = context.classLoader();
        
        try {
            Class<?> targetClass;
            if (classLoader != null) {
                targetClass = XposedHelpers.findClass(className, classLoader);
            } else {
                targetClass = XposedHelpers.findClass(className, null);
            }
            if (targetClass == null) {
                logger.error("找不到类: " + className);
                return "找不到类: " + className;
            }
        
            return switch (type) {
                case "field" -> handleField(targetClass, memberName, valueToSet, accessSuper, accessInterfaces, rawOutput);
                case "method" -> handleMethod(targetClass, memberName, methodParams, accessSuper, accessInterfaces, rawOutput);
                case "constructor" -> handleConstructor(targetClass, methodParams, rawOutput);
                case "static" -> handleStatic(targetClass, memberName, valueToSet, rawOutput);
                default -> "未知类型: " + type + "\n" + getHelpText();
            };
            
        } catch (Exception e) {
            logger.error("执行reflect命令失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }
    
    private String handleField(Class<?> targetClass, String fieldName, String valueToSet, 
                               boolean accessSuper, boolean accessInterfaces, boolean rawOutput) {
        try {
            Field field = findField(targetClass, fieldName, accessSuper, accessInterfaces);
            
            if (field == null) {
                return "找不到字段: " + fieldName;
            }
            
            field.setAccessible(true);
            
            if (valueToSet != null) {
                Object value = parseValue(valueToSet, field.getType());
                if (targetClass.isMemberClass() && Modifier.isStatic(field.getModifiers())) {
                    field.set(null, value);
                } else {
                    field.set(null, value);
                }
                logger.info("设置字段 " + fieldName + " = " + value);
                return "字段 " + fieldName + " 已设置为: " + formatValue(value, rawOutput);
            } else {
                Object value = field.get(null);
                logger.info("获取字段 " + fieldName + " = " + value);
                return "字段 " + fieldName + " = " + formatValue(value, rawOutput);
            }
            
        } catch (Exception e) {
            logger.error("处理字段失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }
    
    private String handleMethod(Class<?> targetClass, String methodName, String[] params,
                                boolean accessSuper, boolean accessInterfaces, boolean rawOutput) {
        try {
            Method method = findMethod(targetClass, methodName, params, accessSuper, accessInterfaces);
            
            if (method == null) {
                return "找不到方法: " + methodName;
            }
            
            method.setAccessible(true);
            
            Object result;
            if (Modifier.isStatic(method.getModifiers())) {
                result = method.invoke(null, convertParams(params, method.getParameterTypes()));
            } else {
                return "方法 " + methodName + " 不是静态方法，需要实例对象";
            }
            
            logger.info("调用方法 " + methodName + " = " + result);
            return "方法 " + methodName + " 返回: " + formatValue(result, rawOutput);
            
        } catch (Exception e) {
            logger.error("调用方法失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }
    
    private String handleConstructor(Class<?> targetClass, String[] params, boolean rawOutput) {
        try {
            Constructor<?> constructor = findConstructor(targetClass, params);
            
            if (constructor == null) {
                return "找不到匹配的构造函数";
            }
            
            constructor.setAccessible(true);
            Object instance = constructor.newInstance(convertParams(params, constructor.getParameterTypes()));
            
            logger.info("创建实例: " + instance);
            return "创建实例: " + formatValue(instance, rawOutput);
            
        } catch (Exception e) {
            logger.error("创建实例失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }
    
    private String handleStatic(Class<?> targetClass, String memberName, String valueToSet, boolean rawOutput) {
        try {
            Field field = findField(targetClass, memberName, false, false);
            
            if (field == null) {
                return "找不到静态字段: " + memberName;
            }
            
            if (!Modifier.isStatic(field.getModifiers())) {
                return memberName + " 不是静态字段";
            }
            
            field.setAccessible(true);
            
            if (valueToSet != null) {
                Object value = parseValue(valueToSet, field.getType());
                field.set(null, value);
                logger.info("设置静态字段 " + memberName + " = " + value);
                return "静态字段 " + memberName + " 已设置为: " + formatValue(value, rawOutput);
            } else {
                Object value = field.get(null);
                logger.info("获取静态字段 " + memberName + " = " + value);
                return "静态字段 " + memberName + " = " + formatValue(value, rawOutput);
            }
            
        } catch (Exception e) {
            logger.error("处理静态字段失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }
    
    private Field findField(Class<?> targetClass, String fieldName, boolean accessSuper, boolean accessInterfaces) {
        Class<?> currentClass = targetClass;
        
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        
        if (accessInterfaces) {
            assert targetClass != null;
            Class<?>[] interfaces = targetClass.getInterfaces();
            for (Class<?> _interface : interfaces) {
                try {
                    return _interface.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    continue;
                }
            }
        }
        
        return null;
    }
    
    private Method findMethod(Class<?> targetClass, String methodName, String[] params, 
                              boolean accessSuper, boolean accessInterfaces) {
        Class<?> currentClass = targetClass;
        
        while (currentClass != null) {
            Method[] methods = currentClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    if (params == null || params.length == 0) {
                        if (method.getParameterCount() == 0) {
                            return method;
                        }
                    } else {
                        Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes.length == params.length) {
                            return method;
                        }
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        
        if (accessInterfaces) {
            Class<?>[] interfaces = targetClass.getInterfaces();
            for (Class<?> iface : interfaces) {
                Method[] methods = iface.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getName().equals(methodName)) {
                        if (params == null || params.length == 0) {
                            if (method.getParameterCount() == 0) {
                                return method;
                            }
                        } else {
                            Class<?>[] paramTypes = method.getParameterTypes();
                            if (paramTypes.length == params.length) {
                                return method;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    private Constructor<?> findConstructor(Class<?> targetClass, String[] params) {
        Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            if (params == null || params.length == 0) {
                if (constructor.getParameterCount() == 0) {
                    return constructor;
                }
            } else {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length == params.length) {
                    return constructor;
                }
            }
        }
        
        return null;
    }
    
    private String[] parseParams(String paramsStr) {
        List<String> params = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < paramsStr.length(); i++) {
            char c = paramsStr.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    params.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            params.add(current.toString());
        }
        
        return params.toArray(new String[0]);
    }
    
    private Object[] convertParams(String[] params, Class<?>[] paramTypes) {
        if (params == null || params.length == 0) {
            return new Object[0];
        }
        
        Object[] result = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (i < paramTypes.length) {
                result[i] = parseValue(params[i], paramTypes[i]);
            } else {
                result[i] = parseValue(params[i], String.class);
            }
        }
        
        return result;
    }
    
    private Object parseValue(String valueStr, Class<?> targetType) {
        if (valueStr == null) {
            return null;
        }
        
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(valueStr);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(valueStr);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(valueStr);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(valueStr);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(valueStr);
        } else if (targetType == char.class || targetType == Character.class) {
            if (valueStr.length() == 1) {
                return valueStr.charAt(0);
            } else if (valueStr.startsWith("'") && valueStr.endsWith("'") && valueStr.length() == 3) {
                return valueStr.charAt(1);
            }
            throw new IllegalArgumentException("无法解析字符: " + valueStr);
        } else if (targetType == String.class) {
            if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
                return valueStr.substring(1, valueStr.length() - 1);
            }
            return valueStr;
        } else {
            return valueStr;
        }
    }
    
    private String formatValue(Object value, boolean rawOutput) {
        if (value == null) {
            return "null";
        }
        
        if (rawOutput) {
            return value.toString();
        }
        
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValue(Array.get(value, i), false));
            }
            sb.append("]");
            return sb.toString();
        }
        
        return value.toString();
    }
    
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\n    at ").append(element.toString());
        }
        return sb.toString();
    }
}
