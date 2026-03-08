package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectCommand extends AbstractClassCommand {

    public ReflectCommand() {
        super("class reflect");
    }

    @Override
    protected String executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        
        if (args.length < 3) {
            return "参数不足, 需要至少3个参数: class reflect <class> <type> <name> [options]\n" + getHelpText();
        }

        String className = args[0];
        String type = args[1];
        String memberName = args[2];
        
        String valueToSet = null;
        String[] methodParams = null;
        boolean accessSuper = false;
        boolean accessInterfaces = false;
        boolean rawOutput = false;

        for (int i = 3; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-v", "--value" -> {
                    if (i + 1 < args.length) {
                        valueToSet = args[++i];
                    }
                }
                case "-p", "--params" -> {
                    if (i + 1 < args.length) {
                        String paramsStr = args[++i];
                        methodParams = context.parseParams(paramsStr);
                    }
                }
                case "-s", "--super" -> accessSuper = true;
                case "-i", "--interfaces" -> accessInterfaces = true;
                case "-r", "--raw" -> rawOutput = true;
            }
        }
        
        Class<?> targetClass = context.loadClass(className);
        if (targetClass == null) {
            context.getLogger().error("找不到类: " + className);
            return "找不到类: " + className;
        }

        return switch (type) {
            case "field" -> handleReflectField(targetClass, memberName, valueToSet, accessSuper, accessInterfaces, rawOutput, context);
            case "method" -> handleReflectMethod(targetClass, memberName, methodParams, accessSuper, accessInterfaces, rawOutput, context);
            case "constructor" -> handleReflectConstructor(targetClass, methodParams, rawOutput, context);
            case "static" -> handleReflectStatic(targetClass, memberName, valueToSet, rawOutput, context);
            default -> "未知类型: " + type + "\n" + getHelpText();
        };
    }

    @Override
    public String getHelpText() {
        return """
            语法: class reflect <class> <type> <name> [options]
            
            使用统一的反射接口访问和操作类的私有成员。
            
            类型 (type参数) 说明:
                field        - 获取/设置字段值
                method       - 调用方法
                constructor  - 创建实例
                static       - 访问静态成员
            
            选项:
                -v, --value <value>      设置字段值
                -p, --params <args>      方法参数（空格分隔）
                -s, --super             访问父类成员
                -i, --interfaces         访问接口成员
                -r, --raw                原始输出（不格式化）
            
            示例:
                class reflect java.lang.System field out
                class reflect java.lang.Integer method parseInt -p "String:\\"525113244\\""
                class reflect java.lang.String constructor -p "String:\\"bruh\\""
                class reflect java.lang.System static out
            """;
    }

    private String handleReflectField(Class<?> targetClass, String fieldName, String valueToSet, 
                                     boolean accessSuper, boolean accessInterfaces, boolean rawOutput, ClassCommandContext context) {
        try {
            Field field = findReflectField(targetClass, fieldName, accessSuper, accessInterfaces);
            
            if (field == null) {
                return "找不到字段: " + fieldName;
            }
            
            field.setAccessible(true);
            
            if (valueToSet != null) {
                Object value = context.parseValue(valueToSet, field.getType());
                if (Modifier.isStatic(field.getModifiers())) {
                    field.set(null, value);
                } else {
                    // TODO
                    field.set(null, value);
                }
                context.getLogger().info("设置字段 " + fieldName + " = " + value);
                return "字段 " + fieldName + " 已设置为: " + context.formatValue(value, rawOutput);
            } else {
                Object value = field.get(null);
                context.getLogger().info("获取字段 " + fieldName + " = " + value);
                return "字段 " + fieldName + " = " + context.formatValue(value, rawOutput);
            }
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("class reflect field", e, context.getLogger(), "处理字段失败");
        }
    }

    private String handleReflectMethod(Class<?> targetClass, String methodName, String[] params,
                                      boolean accessSuper, boolean accessInterfaces, boolean rawOutput, ClassCommandContext context) {
        try {
            Method method = findReflectMethod(targetClass, methodName, params, accessSuper, accessInterfaces);
            
            if (method == null) {
                return "找不到方法: " + methodName;
            }
            
            method.setAccessible(true);
            
            Object result;
            if (Modifier.isStatic(method.getModifiers())) {
                result = method.invoke(null, context.convertParams(params, method.getParameterTypes()));
            } else {
                return "方法 " + methodName + " 不是静态方法，需要实例对象";
            }
            
            context.getLogger().info("调用方法 " + methodName + " = " + result);
            return "方法 " + methodName + " 返回: " + context.formatValue(result, rawOutput);
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("class reflect method", e, context.getLogger(), "调用方法失败");
        }
    }

    private String handleReflectConstructor(Class<?> targetClass, String[] params, boolean rawOutput, ClassCommandContext context) {
        try {
            Constructor<?> constructor = findReflectConstructor(targetClass, params);
            
            if (constructor == null) {
                return "找不到匹配的构造函数";
            }
            
            constructor.setAccessible(true);
            Object instance = constructor.newInstance(context.convertParams(params, constructor.getParameterTypes()));
            
            context.getLogger().info("创建实例: " + instance);
            return "创建实例: " + context.formatValue(instance, rawOutput);
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("class reflect constructor", e, context.getLogger(), "创建实例失败");
        }
    }

    private String handleReflectStatic(Class<?> targetClass, String memberName, String valueToSet, boolean rawOutput, ClassCommandContext context) {
        try {
            Field field = findReflectField(targetClass, memberName, false, false);
            
            if (field == null) {
                return "找不到静态字段: " + memberName;
            }
            
            if (!Modifier.isStatic(field.getModifiers())) {
                return memberName + " 不是静态字段";
            }
            
            field.setAccessible(true);
            
            if (valueToSet != null) {
                Object value = context.parseValue(valueToSet, field.getType());
                field.set(null, value);
                context.getLogger().info("设置静态字段 " + memberName + " = " + value);
                return "静态字段 " + memberName + " 已设置为: " + context.formatValue(value, rawOutput);
            } else {
                Object value = field.get(null);
                context.getLogger().info("获取静态字段 " + memberName + " = " + value);
                return "静态字段 " + memberName + " = " + context.formatValue(value, rawOutput);
            }
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("class reflect static", e, context.getLogger(), "处理静态字段失败");
        }
    }

    private Field findReflectField(Class<?> targetClass, String fieldName, boolean accessSuper, boolean accessInterfaces) {
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
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        
        return null;
    }

    private Method findReflectMethod(@NotNull Class<?> targetClass, String methodName, String[] params,
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
            if (accessSuper) {
                currentClass = currentClass.getSuperclass();
            } else {
                break;
            }
        }
        
        if (accessInterfaces) {
            Class<?>[] interfaces = targetClass.getInterfaces();
            for (Class<?> _interface : interfaces) {
                Method[] methods = _interface.getDeclaredMethods();
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

    private Constructor<?> findReflectConstructor(Class<?> targetClass, String[] params) {
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
}
