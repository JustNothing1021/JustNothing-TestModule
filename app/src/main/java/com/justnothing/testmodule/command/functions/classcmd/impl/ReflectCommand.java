package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.functions.classcmd.AbstractClassCommand;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.request.ReflectClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ReflectOperationResult;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

public class ReflectCommand extends AbstractClassCommand<ReflectClassRequest, ReflectOperationResult> {

    public ReflectCommand() {
        super("class reflect", ReflectClassRequest.class, ReflectOperationResult.class);
    }

    @Override
    protected ReflectOperationResult executeClassCommand(ClassCommandContext<ReflectClassRequest> context) throws Exception {
        ReflectClassRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();
        String type = request.getOperationType();
        String memberName = request.getMemberName();

        if (className == null || type == null || memberName == null) {
            CommandExceptionHandler.handleException(
                "class reflect",
                new IllegalArgumentException("参数不足, 需要至少3个参数: class reflect <class> <type> <name> [options]"),
                context.execContext(),
                "参数错误"
            );
            return null;
        }

        ReflectOperationResult result = new ReflectOperationResult();
        result.setClassName(className);
        result.setMemberName(memberName);
        result.setOperationType(type);
        result.setSuccess(true);

        String valueToSet = request.getValueToSet();
        String[] methodParams = null;
        boolean accessSuper = request.isAccessSuper();
        boolean accessInterfaces = request.isAccessInterfaces();
        boolean rawOutput = request.isRawOutput();

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());

        switch (type) {
            case "field" -> handleReflectField(targetClass, memberName, valueToSet, accessInterfaces, rawOutput, context, result);
            case "method" -> handleReflectMethod(targetClass, memberName, methodParams, accessSuper, accessInterfaces, rawOutput, context, result);
            case "constructor" -> handleReflectConstructor(targetClass, methodParams, rawOutput, context, result);
            case "static" -> handleReflectStatic(targetClass, memberName, valueToSet, rawOutput, context, result);
            default -> {
                CommandExceptionHandler.handleException(
                    "class reflect",
                    new IllegalArgumentException("未知类型: " + type),
                    context.execContext(),
                    "参数错误"
                );
                result.setSuccess(false);
            }
        }
        return result;
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

    private void handleReflectField(Class<?> targetClass, String fieldName, String valueToSet,
                boolean accessInterfaces, boolean rawOutput, ClassCommandContext<ReflectClassRequest> context, ReflectOperationResult result) {
        try {
            Field field = findReflectField(targetClass, fieldName, accessInterfaces);

            if (field == null) {
                CommandExceptionHandler.handleException(
                    "class reflect field",
                    new NoSuchFieldException("找不到字段: " + fieldName),
                    context.execContext(),
                    Map.of("类名", targetClass.getName(), "字段名", fieldName),
                    "字段查找失败"
                );
                result.setSuccess(false);
                return;
            }

            field.setAccessible(true);
            result.setFieldInfo(FieldInfo.fromField(field));

            context.execContext().print("找到字段: ", Colors.CYAN);
            DescriptorColorizer.printColoredDescriptor(context.execContext(), field, true);
            context.execContext().println("");
            context.execContext().println("");

            Object value;
            if (valueToSet != null) {
                value = context.parseValue(valueToSet, field.getType());
                field.set(null, value);
                context.logger().info("设置字段 " + fieldName + " = " + value);
                result.setValue(value);
                result.setValueType(value != null ? value.getClass().getName() : null);
                context.execContext().print("字段 ", Colors.CYAN);
                context.execContext().print(fieldName, Colors.CYAN);
                context.execContext().print(" 已设置为: ", Colors.CYAN);
            } else {
                value = field.get(null);
                context.logger().info("获取字段 " + fieldName + " = " + value);
                result.setValue(value);
                result.setValueType(value != null ? value.getClass().getName() : null);
                context.execContext().print("字段 ", Colors.CYAN);
                context.execContext().print(fieldName, Colors.CYAN);
                context.execContext().print(" = ", Colors.WHITE);
            }
            context.execContext().println(context.formatValue(value, rawOutput), Colors.LIGHT_GREEN);

        } catch (Exception e) {
            CommandExceptionHandler.handleException("class reflect field", e, context.execContext(), "处理字段失败");
            result.setSuccess(false);
        }
    }

    private void handleReflectMethod(Class<?> targetClass, String methodName, String[] params,
          boolean accessSuper, boolean accessInterfaces, boolean rawOutput, ClassCommandContext<ReflectClassRequest> context, ReflectOperationResult result) {
        try {
            Method method = findReflectMethod(targetClass, methodName, params, accessSuper, accessInterfaces);
            
            if (method == null) {
                CommandExceptionHandler.handleException(
                    "class reflect method",
                    new NoSuchMethodException("找不到方法: " + methodName),
                    context.execContext(),
                    Map.of("类名", targetClass.getName(), "方法名", methodName),
                    "方法查找失败"
                );
                result.setSuccess(false);
                return;
            }

            method.setAccessible(true);
            result.setMethodInfo(MethodInfo.fromMethod(method));

            context.execContext().print("找到方法: ", Colors.CYAN);
            DescriptorColorizer.printColoredDescriptor(context.execContext(), method, true);
            context.execContext().println("");
            context.execContext().println("");

            Object returnValue;
            if (Modifier.isStatic(method.getModifiers())) {
                returnValue = method.invoke(null, context.convertParams(params, method.getParameterTypes()));
            } else {
                CommandExceptionHandler.handleException(
                    "class reflect method",
                    new IllegalStateException("方法 " + methodName + " 不是静态方法，需要实例对象"),
                    context.execContext(),
                    Map.of("类名", targetClass.getName(), "方法名", methodName),
                    "非静态方法调用失败"
                );
                result.setSuccess(false);
                return;
            }

            context.logger().info("调用方法 " + methodName + " = " + returnValue);
            context.execContext().print("方法 ", Colors.CYAN);
            context.execContext().print(methodName, Colors.CYAN);
            context.execContext().print(" 返回: ", Colors.CYAN);
            context.execContext().println(context.formatValue(returnValue, rawOutput), Colors.LIGHT_GREEN);

            result.setValue(returnValue);
            result.setValueType(returnValue != null ? returnValue.getClass().getName() : null);

        } catch (Exception e) {
            CommandExceptionHandler.handleException("class reflect method", e, context.execContext(), "调用方法失败");
            result.setSuccess(false);
        }
    }

    private void handleReflectConstructor(Class<?> targetClass, String[] params, boolean rawOutput,
          ClassCommandContext<ReflectClassRequest> context, ReflectOperationResult result) {
        try {
            Constructor<?> constructor = findReflectConstructor(targetClass, params);
            
            if (constructor == null) {
                CommandExceptionHandler.handleException(
                    "class reflect constructor",
                    new NoSuchMethodException("找不到匹配的构造函数"),
                    context.execContext(),
                    Map.of("类名", targetClass.getName()),
                    "构造函数查找失败"
                );
                result.setSuccess(false);
                return;
            }

            constructor.setAccessible(true);
            result.setMethodInfo(MethodInfo.fromConstructor(constructor));

            context.execContext().print("找到构造函数: ", Colors.CYAN);
            DescriptorColorizer.printColoredDescriptor(context.execContext(), constructor, true);
            context.execContext().println("");
            context.execContext().println("");

            Object instance = constructor.newInstance(context.convertParams(params, constructor.getParameterTypes()));

            context.logger().info("创建实例: " + instance);
            context.execContext().print("创建实例: ", Colors.CYAN);
            context.execContext().println(context.formatValue(instance, rawOutput), Colors.LIGHT_GREEN);

            result.setValue(instance);
            result.setValueType(instance != null ? instance.getClass().getName() : null);

        } catch (Exception e) {
            CommandExceptionHandler.handleException("class reflect constructor", e, context.execContext(), "创建实例失败");
            result.setSuccess(false);
        }
    }

    private void handleReflectStatic(Class<?> targetClass, String memberName, String valueToSet,
         boolean rawOutput, ClassCommandContext<ReflectClassRequest> context, ReflectOperationResult result) {
        try {
            Field field = findReflectField(targetClass, memberName, false);
            
            if (field == null) {
                CommandExceptionHandler.handleException(
                    "class reflect static",
                    new NoSuchFieldException("找不到静态字段: " + memberName),
                    context.execContext(),
                    Map.of("类名", targetClass.getName(), "字段名", memberName),
                    "静态字段查找失败"
                );
                result.setSuccess(false);
                return;
            }

            if (!Modifier.isStatic(field.getModifiers())) {
                CommandExceptionHandler.handleException(
                    "class reflect static",
                    new IllegalStateException(memberName + " 不是静态字段"),
                    context.execContext(),
                    Map.of("类名", targetClass.getName(), "字段名", memberName),
                    "非静态字段错误"
                );
                result.setSuccess(false);
                return;
            }

            field.setAccessible(true);
            result.setFieldInfo(FieldInfo.fromField(field));

            context.execContext().print("找到静态字段: ", Colors.CYAN);
            DescriptorColorizer.printColoredDescriptor(context.execContext(), field, true);
            context.execContext().println("");
            context.execContext().println("");

            Object value;
            if (valueToSet != null) {
                value = context.parseValue(valueToSet, field.getType());
                field.set(null, value);
                context.logger().info("设置静态字段 " + memberName + " = " + value);
                context.execContext().print("静态字段 ", Colors.CYAN);
                context.execContext().print(memberName, Colors.CYAN);
                context.execContext().print(" 已设置为: ", Colors.CYAN);
            } else {
                value = field.get(null);
                context.logger().info("获取静态字段 " + memberName + " = " + value);
                context.execContext().print("静态字段 ", Colors.CYAN);
                context.execContext().print(memberName, Colors.CYAN);
                context.execContext().print(" = ", Colors.WHITE);
            }
            context.execContext().println(context.formatValue(value, rawOutput), Colors.LIGHT_GREEN);

            result.setValue(value);
            result.setValueType(value != null ? value.getClass().getName() : null);

        } catch (Exception e) {
            CommandExceptionHandler.handleException("class reflect static", e, context.execContext(), "处理静态字段失败");
            result.setSuccess(false);
        }
    }

    private Field findReflectField(Class<?> targetClass, String fieldName, boolean accessInterfaces) {
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
