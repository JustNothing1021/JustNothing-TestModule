package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.request.ReflectClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ReflectOperationResult;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.format.DescriptorColorizer;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

@SubCommandInfo(
    description = ClassTexts.SUB_CLASS_REFLECT_DESC,
    usage = "class reflect <class> <type> <name> [options]",
    examples = {
        "class reflect java.lang.System field out",
        "class reflect java.lang.Integer method parseInt -p \"String:\\\"525113244\\\"",
        "class reflect java.lang.String constructor -p \"String:\\\"bruh\\\"",
        "class reflect java.lang.System static out"
    },
    optionsDesc = ClassTexts.SUB_CLASS_REFLECT_OPTIONS
)
public class ClassReflectCommand extends AbstractClassCommand<ReflectClassRequest, ReflectOperationResult> {

    public ClassReflectCommand() {
        super("class reflect", ReflectClassRequest.class, ReflectOperationResult.class);
    }

    @Override
    protected ReflectOperationResult executeClassCommand(ClassCommandContext<ReflectClassRequest> context) throws Exception {
        ReflectClassRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();
        String type = request.getOperationType();
        String memberName = request.getMemberName();

        if (className == null || type == null || memberName == null) {
            throw new IllegalCommandLineArgumentException(Text.zhEn(
                    "参数不足, 需要至少3个参数: class reflect <class> <type> <name> [options]",
                    "Not enough arguments; at least 3 are required: class reflect <class> <type> <name> [options]").text());
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
            default -> throw new IllegalCommandLineArgumentException(
                    CliMessages.ERR_UNKNOWN_TYPE.format(type));
        }
        return result;
    }

    private void handleReflectField(Class<?> targetClass, String fieldName, String valueToSet,
                boolean accessInterfaces, boolean rawOutput, ClassCommandContext<ReflectClassRequest> context, ReflectOperationResult result) {
        try {
            Field field = findReflectField(targetClass, fieldName, accessInterfaces);

            if (field == null) {
                CommandExceptionHandler.handleException(
                    "class reflect field",
                    new NoSuchFieldException(ClassTexts.ERR_FIELD_NOT_FOUND.format(fieldName)),
                    context.execContext(),
                    Map.of(CliMessages.CONTEXT_CLASS_NAME.text(), targetClass.getName(),
                           CliMessages.CONTEXT_FIELD_NAME.text(), fieldName),
                    Text.zhEn("字段查找失败", "Field lookup failed").text()
                );
                result.setSuccess(false);
                return;
            }

            field.setAccessible(true);
            result.setFieldInfo(FieldInfo.fromField(field));

            context.execContext().print(Text.zhEn("找到字段: ", "Found field: ").text(), Colors.CYAN);
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
                context.execContext().print(Text.zhEn("字段 ", "Field ").text(), Colors.CYAN);
                context.execContext().print(fieldName, Colors.CYAN);
                context.execContext().print(ClassTexts.LABEL_SET_TO.text(), Colors.CYAN);
            } else {
                value = field.get(null);
                context.logger().info("获取字段 " + fieldName + " = " + value);
                result.setValue(value);
                result.setValueType(value != null ? value.getClass().getName() : null);
                context.execContext().print(Text.zhEn("字段 ", "Field ").text(), Colors.CYAN);
                context.execContext().print(fieldName, Colors.CYAN);
                context.execContext().print(" = ", Colors.WHITE);
            }
            context.execContext().println(context.formatValue(value, rawOutput), Colors.LIGHT_GREEN);

        } catch (Exception e) {
            CommandExceptionHandler.handleException("class reflect field", e, context.execContext(),
                    Text.zhEn("处理字段失败", "Failed to handle the field").text());
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
                    new NoSuchMethodException(Text.zhEn("找不到方法: %s", "Method not found: %s").format(methodName)),
                    context.execContext(),
                    Map.of(CliMessages.CONTEXT_CLASS_NAME.text(), targetClass.getName(),
                           CliMessages.CONTEXT_METHOD_NAME.text(), methodName),
                    Text.zhEn("方法查找失败", "Method lookup failed").text()
                );
                result.setSuccess(false);
                return;
            }

            method.setAccessible(true);
            result.setMethodInfo(MethodInfo.fromMethod(method));

            context.execContext().print(ClassTexts.LABEL_FOUND_METHOD.text(), Colors.CYAN);
            DescriptorColorizer.printColoredDescriptor(context.execContext(), method, true);
            context.execContext().println("");
            context.execContext().println("");

            Object returnValue;
            if (Modifier.isStatic(method.getModifiers())) {
                returnValue = method.invoke(null, context.convertParams(params, method.getParameterTypes()));
            } else {
                CommandExceptionHandler.handleException(
                    "class reflect method",
                    new IllegalStateException(Text.zhEn(
                            "方法 %s 不是静态方法，需要实例对象",
                            "Method %s is not static; an instance is required").format(methodName)),
                    context.execContext(),
                    Map.of(CliMessages.CONTEXT_CLASS_NAME.text(), targetClass.getName(),
                           CliMessages.CONTEXT_METHOD_NAME.text(), methodName),
                    Text.zhEn("非静态方法调用失败", "Cannot invoke a non-static method this way").text()
                );
                result.setSuccess(false);
                return;
            }

            context.logger().info("调用方法 " + methodName + " = " + returnValue);
            context.execContext().print(Text.zhEn("方法 ", "Method ").text(), Colors.CYAN);
            context.execContext().print(methodName, Colors.CYAN);
            context.execContext().print(Text.zhEn(" 返回: ", " returned: ").text(), Colors.CYAN);
            context.execContext().println(context.formatValue(returnValue, rawOutput), Colors.LIGHT_GREEN);

            result.setValue(returnValue);
            result.setValueType(returnValue != null ? returnValue.getClass().getName() : null);

        } catch (Exception e) {
            CommandExceptionHandler.handleException("class reflect method", e, context.execContext(),
                    Text.zhEn("调用方法失败", "Method invocation failed").text());
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
                    new NoSuchMethodException(Text.zhEn("找不到匹配的构造函数", "No matching constructor found").text()),
                    context.execContext(),
                    Map.of(CliMessages.CONTEXT_CLASS_NAME.text(), targetClass.getName()),
                    Text.zhEn("构造函数查找失败", "Constructor lookup failed").text()
                );
                result.setSuccess(false);
                return;
            }

            constructor.setAccessible(true);
            result.setMethodInfo(MethodInfo.fromConstructor(constructor));

            context.execContext().print(ClassTexts.LABEL_FOUND_CONSTRUCTOR.text(), Colors.CYAN);
            DescriptorColorizer.printColoredDescriptor(context.execContext(), constructor, true);
            context.execContext().println("");
            context.execContext().println("");

            Object instance = constructor.newInstance(context.convertParams(params, constructor.getParameterTypes()));

            context.logger().info("创建实例: " + instance);
            context.execContext().print(Text.zhEn("创建实例: ", "Created instance: ").text(), Colors.CYAN);
            context.execContext().println(context.formatValue(instance, rawOutput), Colors.LIGHT_GREEN);

            result.setValue(instance);
            result.setValueType(instance.getClass().getName());

        } catch (Exception e) {
            CommandExceptionHandler.handleException("class reflect constructor", e, context.execContext(),
                    Text.zhEn("创建实例失败", "Failed to create the instance").text());
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
                    new NoSuchFieldException(Text.zhEn("找不到静态字段: %s", "Static field not found: %s").format(memberName)),
                    context.execContext(),
                    Map.of(CliMessages.CONTEXT_CLASS_NAME.text(), targetClass.getName(),
                           CliMessages.CONTEXT_FIELD_NAME.text(), memberName),
                    Text.zhEn("静态字段查找失败", "Static field lookup failed").text()
                );
                result.setSuccess(false);
                return;
            }

            if (!Modifier.isStatic(field.getModifiers())) {
                CommandExceptionHandler.handleException(
                    "class reflect static",
                    new IllegalStateException(Text.zhEn("%s 不是静态字段", "%s is not a static field").format(memberName)),
                    context.execContext(),
                    Map.of(CliMessages.CONTEXT_CLASS_NAME.text(), targetClass.getName(),
                           CliMessages.CONTEXT_FIELD_NAME.text(), memberName),
                    Text.zhEn("非静态字段错误", "Not a static field").text()
                );
                result.setSuccess(false);
                return;
            }

            field.setAccessible(true);
            result.setFieldInfo(FieldInfo.fromField(field));

            context.execContext().print(Text.zhEn("找到静态字段: ", "Found static field: ").text(), Colors.CYAN);
            DescriptorColorizer.printColoredDescriptor(context.execContext(), field, true);
            context.execContext().println("");
            context.execContext().println("");

            Object value;
            if (valueToSet != null) {
                value = context.parseValue(valueToSet, field.getType());
                field.set(null, value);
                context.logger().info("设置静态字段 " + memberName + " = " + value);
                context.execContext().print(ClassTexts.LABEL_STATIC_FIELD.text(), Colors.CYAN);
                context.execContext().print(memberName, Colors.CYAN);
                context.execContext().print(ClassTexts.LABEL_SET_TO.text(), Colors.CYAN);
            } else {
                value = field.get(null);
                context.logger().info("获取静态字段 " + memberName + " = " + value);
                context.execContext().print(ClassTexts.LABEL_STATIC_FIELD.text(), Colors.CYAN);
                context.execContext().print(memberName, Colors.CYAN);
                context.execContext().print(" = ", Colors.WHITE);
            }
            context.execContext().println(context.formatValue(value, rawOutput), Colors.LIGHT_GREEN);

            result.setValue(value);
            result.setValueType(value != null ? value.getClass().getName() : null);

        } catch (Exception e) {
            CommandExceptionHandler.handleException("class reflect static", e, context.execContext(),
                    Text.zhEn("处理静态字段失败", "Failed to handle the static field").text());
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
