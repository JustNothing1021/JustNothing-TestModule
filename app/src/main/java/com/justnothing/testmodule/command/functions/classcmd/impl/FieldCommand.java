package com.justnothing.testmodule.command.functions.classcmd.impl;


import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.classcmd.AbstractClassCommand;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.GetFieldValueRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.GetFieldValueResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

@SubCommandInfo(
    description = "查看或修改类的字段值，支持静态字段和实例字段",
    usage = "class field [options] <class_name> [field_name]",
    examples = {
        "class field java.lang.String",
        "class field -g java.lang.System out",
        "class field -s com.example.MyClass someField \"newValue\""
    },
    optionsDesc = """
            选项:
                -g, --get             获取字段值 (需提供字段名)
                -s, --set             设置字段值 (格式: -s <class> <field> <value>)
                -v, --value           显示字段值
                -t, --type            显示字段类型
                -m, --modifiers       显示修饰符
                -a, --all             显示所有信息 (默认)
                --super               访问父类字段
                --interfaces          访问接口字段
            """
)
public class FieldCommand extends AbstractClassCommand<GetFieldValueRequest, GetFieldValueResult> {

    public FieldCommand() {
        super("class field", GetFieldValueRequest.class, GetFieldValueResult.class);
    }

    @Override
    protected GetFieldValueResult executeClassCommand(ClassCommandContext<GetFieldValueRequest> context) throws Exception {
        GetFieldValueRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();
        String fieldName = request.getFieldName();
        String operation = request.getOperation();

        if (className == null || className.isEmpty()) {
            CommandExceptionHandler.handleException(
                    "class field",
                    new IllegalArgumentException("参数不足: class field <class_name> [field_name]"),
                    context.execContext(),
                    "参数错误"
            );
            return null;
        }

        context.logger().debug("目标类: " + className + ", 字段名: " + fieldName + ", 操作: " + operation);

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());
        context.logger().info("成功加载类: " + targetClass.getName());

        GetFieldValueResult result = new GetFieldValueResult();

        boolean isStatic = request.isStatic();
        String targetInstanceStr = request.getTargetInstance();

        if (fieldName != null) {

            Field field;
            Object targetInstance = null;

            if (isStatic || (targetInstanceStr == null || targetInstanceStr.isEmpty())) {
                field = ClassResolver.findStaticField(targetClass, fieldName,
                        request.isAccessSuper(), request.isAccessInterfaces());
            } else {
                field = findInstanceField(targetClass, fieldName,
                        request.isAccessSuper(), request.isAccessInterfaces());
                targetInstance = context.parseValue(targetInstanceStr, targetClass);
                context.logger().info("使用目标实例: " + targetInstance + " (类: " + targetClass.getName() + ")");
            }

            if (field == null) {
                CommandExceptionHandler.handleException(
                        "class field",
                        new NoSuchFieldException("找不到字段: " + fieldName),
                        context.execContext(),
                        Map.of("类名", className, "字段名", fieldName),
                        "字段查找失败"
                );
                result.setValueString("ERROR");
                return result;
            }

            field.setAccessible(true);

            if ("get".equals(operation)) {
                boolean fieldIsStatic = Modifier.isStatic(field.getModifiers());
                Object instance = fieldIsStatic ? null : targetInstance;

                if (!fieldIsStatic && instance == null) {
                    CommandExceptionHandler.handleException(
                            "class field",
                            new IllegalStateException("无法获取非静态字段，需要提供实例对象 (使用 --instance 参数)"),
                            context.execContext(),
                            Map.of("类名", className, "字段名", fieldName),
                            "非静态字段访问失败"
                    );
                    result.setValueString("ERROR");
                    return result;
                }

                Object value = field.get(instance);

                // 映射到 GetFieldValueResult 字段
                if (value != null) {
                    result.setValueString(value.toString());
                    result.setValueTypeName(value.getClass().getName());
                    result.setValueHash(System.identityHashCode(value));
                } else {
                    result.setValueString("null");
                    result.setValueTypeName("null");
                    result.setValueHash(0);
                }

                context.execContext().print("字段值: ", Colors.CYAN);
                if (value != null) {
                    context.execContext().println(value.toString(), Colors.LIGHT_GREEN);
                } else {
                    context.execContext().println("null", Colors.LIGHT_BLUE);
                }
                return result;
            }

            if ("set".equals(operation)) {
                boolean fieldIsStatic = Modifier.isStatic(field.getModifiers());
                Object instance = fieldIsStatic ? null : targetInstance;

                if (!fieldIsStatic && instance == null) {
                    CommandExceptionHandler.handleException(
                            "class field",
                            new IllegalStateException("无法设置非静态字段，需要提供实例对象 (使用 --instance 参数)"),
                            context.execContext(),
                            Map.of("类名", className, "字段名", fieldName),
                            "非静态字段访问失败"
                    );
                    result.setValueString("ERROR");
                    return result;
                }

                Object value = context.parseValue(request.getValueToSet(), field.getType());
                field.set(instance, value);

                // 映射到 GetFieldValueResult 字段
                result.setValueString(value != null ? value.toString() : "null");
                result.setValueTypeName(value != null ? value.getClass().getName() : "void/null");
                result.setValueHash(System.identityHashCode(value));

                context.execContext().print("成功设置字段值: ", Colors.CYAN);
                context.execContext().println(request.getValueToSet(), Colors.LIGHT_GREEN);
                return result;
            }

            // info 模式 - 显示字段详细信息
            context.execContext().println("=== 字段信息 ===", Colors.CYAN);
            context.execContext().print("字段名: ", Colors.CYAN);
            context.execContext().println(field.getName(), Colors.CYAN);

            if (request.isShowAll() || request.isShowType()) {
                context.execContext().print("类型: ", Colors.CYAN);
                context.execContext().println(field.getType().getName(), Colors.GREEN);
            }

            if (request.isShowAll() || request.isShowModifiers()) {
                context.execContext().print("修饰符: ", Colors.CYAN);
                context.execContext().println(ReflectionUtils.getModifiersString(field.getModifiers()), Colors.BLUE);
            }

            if (request.isShowAll() || request.isShowValue()) {
                boolean isFieldStatic = Modifier.isStatic(field.getModifiers());
                Object fieldInstance = isFieldStatic ? null : targetInstance;

                if (!isFieldStatic && fieldInstance == null) {
                    context.execContext().println("值: 非静态字段，需要实例对象", Colors.GRAY);
                    result.setValueString("N/A (非静态)");
                    result.setValueTypeName(field.getType().getName());
                } else {
                    try {
                        Object fieldValue = field.get(fieldInstance);

                        // 映射到 GetFieldValueResult 字段
                        if (fieldValue != null) {
                            result.setValueString(fieldValue.toString());
                            result.setValueTypeName(fieldValue.getClass().getName());
                            result.setValueHash(System.identityHashCode(fieldValue));
                        } else {
                            result.setValueString("null");
                            result.setValueTypeName(field.getType().getName());
                            result.setValueHash(0);
                        }

                        context.execContext().print("值: ", Colors.CYAN);
                        if (fieldValue != null) {
                            context.execContext().println(fieldValue.toString(), Colors.LIGHT_GREEN);
                        } else {
                            context.execContext().println("null", Colors.LIGHT_BLUE);
                        }
                    } catch (Exception e) {
                        context.execContext().print("值: 无法获取 (", Colors.CYAN);
                        String msg = e.getMessage();
                        context.execContext().print(msg == null ? "没有详细信息" : msg, Colors.RED);
                        context.execContext().println(")", Colors.CYAN);
                        result.setValueString("ERROR: " + (msg == null ? e.getClass().getSimpleName() : msg));
                    }
                }
            }

            if (request.isShowAll()) {
                context.execContext().print("声明类: ", Colors.CYAN);
                context.execContext().println(field.getDeclaringClass().getName(), Colors.GREEN);
            }

        } else {
            // list 模式 - 列出所有字段
            Field[] fields = targetClass.getDeclaredFields();
            context.execContext().println("=== 字段列表 ===", Colors.CYAN);
            context.execContext().print("类: ", Colors.CYAN);
            context.execContext().println(targetClass.getName(), Colors.GREEN);
            context.execContext().print("字段总数: ", Colors.CYAN);
            context.execContext().println(String.valueOf(fields.length), Colors.YELLOW);
            context.execContext().println("");

            result.setValueString("Found " + fields.length + " fields in " + className);
            result.setValueTypeName(className);

            if (fields.length == 0) {
                context.execContext().println("无字段", Colors.GRAY);
            } else {
                for (Field f : fields) {
                    context.execContext().print("  ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(context.execContext(), f, true);
                    context.execContext().println("");
                }
            }

        }
        return result;
    }


    private Field findInstanceField(Class<?> targetClass, String fieldName, boolean accessSuper, boolean accessInterfaces) {
        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (!Modifier.isStatic(field.getModifiers())) {
                    return field;
                }
            } catch (NoSuchFieldException ignored) {}
            if (!accessSuper) break;
            current = current.getSuperclass();
        }

        if (accessInterfaces) {
            for (Class<?> _interface : targetClass.getInterfaces()) {
                try {
                    Field field = _interface.getDeclaredField(fieldName);
                    if (!Modifier.isStatic(field.getModifiers())) {
                        return field;
                    }
                } catch (NoSuchFieldException ignored) {}
            }
        }

        return null;
    }
}
