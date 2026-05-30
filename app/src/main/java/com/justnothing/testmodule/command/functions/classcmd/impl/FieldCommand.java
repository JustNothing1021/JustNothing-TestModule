package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.classcmd.AbstractClassCommand;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.FieldRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.GetFieldValueResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@SubCommandInfo(description = "查看或修改类的字段值，支持静态字段和实例字段", usage = "class field <class_name> [operation] [args...]", examples = {
        "class field java.lang.String",
        "class field java.lang.String --get value",
        "class field java.lang.String get value",
        "class field java.lang.String --set value \"hello\"",
        "class field java.lang.String set value \"hello\"",
        "class field MyClass get myField -i \"myInstance\""
}, optionsDesc = """
        操作符:
            get, --get, -g         获取字段值 (需提供字段名)
            set, --set, -s         设置字段值 (格式: set <field> <value>)

        目标选项:
            --class               目标类名 (位置参数 position=1)
            --instance, -i        目标实例表达式 (用于非静态字段)

        显示选项:
            -v, --value           显示字段值
            -t, --type            显示字段类型
            -m, --modifiers       显示修饰符
            -a, --all             显示所有信息 (默认)

        访问控制:
            --super               访问父类字段
            --interfaces          访问接口字段
            --static-only         仅静态字段
        """)
public class FieldCommand extends AbstractClassCommand<FieldRequest, GetFieldValueResult> {

    public FieldCommand() {
        super("class field", FieldRequest.class, GetFieldValueResult.class);
    }

    @Override
    protected GetFieldValueResult executeClassCommand(ClassCommandContext<FieldRequest> context) throws Exception {
        FieldRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();
        String operationMode = request.getOperationMode();

        if (className == null || className.isEmpty()) {
            throw new IllegalCommandLineArgumentException("参数不足: class field <class_name> [operation] [args...]");
        }

        context.logger().debug("目标类: " + className + ", 操作模式: " + operationMode);

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());
        context.logger().info("成功加载类: " + targetClass.getName());

        GetFieldValueResult result = new GetFieldValueResult();

        switch (operationMode) {
            case "get" -> handleGetOperation(context, request, targetClass, result);
            case "set" -> handleSetOperation(context, request, targetClass, result);
            default -> handleListOperation(context, request, targetClass, result);
        }

        return result;
    }

    private void handleGetOperation(ClassCommandContext<FieldRequest> context, FieldRequest request,
            Class<?> targetClass, GetFieldValueResult result) throws Exception {
        // 使用分离存储的子字段
        String fieldName = request.getGetTargetFieldName();

        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalCommandLineArgumentException("--get 操作需要提供字段名");
        }

        Field field = findField(targetClass, fieldName, request);
        if (field == null) {
            throw new IllegalCommandLineArgumentException("找不到字段: " + fieldName);
        }

        field.setAccessible(true);

        boolean fieldIsStatic = Modifier.isStatic(field.getModifiers());
        Object instance = getFieldInstance(context, fieldIsStatic, targetClass, request);

        Object value = field.get(instance);

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
        context.execContext().println(value != null ? value.toString() : "null",
                value != null ? Colors.LIGHT_GREEN : Colors.LIGHT_BLUE);
    }

    private void handleSetOperation(ClassCommandContext<FieldRequest> context, FieldRequest request,
            Class<?> targetClass, GetFieldValueResult result) throws Exception {
        // 使用分离存储的子字段
        String fieldName = request.getSetTargetFieldName();
        String valueStr = request.getSetValueToSet();

        if (fieldName == null || fieldName.isEmpty() || valueStr == null || valueStr.isEmpty()) {
            throw new IllegalCommandLineArgumentException("--set 操作需要 <fieldName> 和 <value> 参数");
        }

        Field field = findField(targetClass, fieldName, request);
        if (field == null) {
            throw new IllegalCommandLineArgumentException("找不到字段: " + fieldName);
        }

        field.setAccessible(true);

        boolean fieldIsStatic = Modifier.isStatic(field.getModifiers());
        Object instance = getFieldInstance(context, fieldIsStatic, targetClass, request);

        Object value = context.parseValue(valueStr, field.getType());
        field.set(instance, value);

        result.setValueString(value != null ? value.toString() : "null");
        result.setValueTypeName(value != null ? value.getClass().getName() : "void/null");
        result.setValueHash(System.identityHashCode(value));

        context.execContext().print("✅ 成功设置字段 [", Colors.CYAN);
        context.execContext().print(fieldName, Colors.YELLOW);
        context.execContext().print("] = ", Colors.CYAN);
        context.execContext().println(valueStr, Colors.LIGHT_GREEN);
    }

    private void handleListOperation(ClassCommandContext<FieldRequest> context, FieldRequest request,
            Class<?> targetClass, GetFieldValueResult result) throws Exception {
        Field[] fields = targetClass.getDeclaredFields();
        context.execContext().println("=== 字段列表 ===", Colors.CYAN);
        context.execContext().print("类: ", Colors.CYAN);
        context.execContext().println(targetClass.getName(), Colors.GREEN);
        context.execContext().print("字段总数: ", Colors.CYAN);
        context.execContext().println(String.valueOf(fields.length), Colors.YELLOW);
        context.execContext().println("");

        result.setValueString("Found " + fields.length + " fields in " + request.getClassName());
        result.setValueTypeName(request.getClassName());

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

    private Field findField(Class<?> targetClass, String fieldName, FieldRequest request) {
        boolean staticOnly = request.isStaticOnly();
        boolean accessSuper = request.isAccessSuper();
        boolean accessInterfaces = request.isAccessInterfaces();

        try {
            Field field;

            if (staticOnly) {
                field = ClassResolver.findStaticField(targetClass, fieldName,
                        accessSuper, accessInterfaces);
            } else {
                field = findInstanceField(targetClass, fieldName, accessSuper, accessInterfaces);

                if (field == null) {
                    logger.debug("未找到实例字段 '" + fieldName + "'，尝试查找静态字段...");
                    field = ClassResolver.findStaticField(targetClass, fieldName,
                            accessSuper, accessInterfaces);

                    if (field != null) {
                        logger.debug("✅ 在静态字段中找到: " + fieldName);
                    }
                }
            }

            return field;
        } catch (Exception e) {
            logger.debug("查找字段 '" + fieldName + "' 时出错: " + e.getMessage());
            return null;
        }
    }

    private Object getFieldInstance(ClassCommandContext<FieldRequest> context, boolean isStatic,
            Class<?> targetClass, FieldRequest request) throws Exception {
        if (isStatic)
            return null;

        String instanceExpr = request.getTargetInstance();
        if (instanceExpr == null || instanceExpr.isEmpty()) {
            throw new IllegalCommandLineArgumentException(
                    "非静态字段需要提供目标实例 (使用 --instance/-i 参数)");
        }

        context.execContext().print("解析目标实例表达式: ", Colors.CYAN);
        context.execContext().println(instanceExpr, Colors.YELLOW);

        Object instance = context.parseValue(instanceExpr, targetClass);

        context.execContext().print("✅ 目标实例: ", Colors.CYAN);
        context.execContext().println(instance != null ? instance.toString() : "null",
                instance != null ? Colors.LIGHT_GREEN : Colors.LIGHT_BLUE);

        return instance;
    }

    private Field findInstanceField(Class<?> targetClass, String fieldName,
            boolean accessSuper, boolean accessInterfaces) {
        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (!Modifier.isStatic(field.getModifiers())) {
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
            }
            if (!accessSuper)
                break;
            current = current.getSuperclass();
        }

        if (accessInterfaces) {
            for (Class<?> _interface : targetClass.getInterfaces()) {
                try {
                    Field field = _interface.getDeclaredField(fieldName);
                    if (!Modifier.isStatic(field.getModifiers())) {
                        return field;
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
        }

        return null;
    }
}
