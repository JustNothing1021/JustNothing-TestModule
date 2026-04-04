package com.justnothing.testmodule.command.functions.classcmd;


import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class FieldCommand extends AbstractClassCommand {

    public FieldCommand() {
        super("class field");
    }

    @Override
    protected String executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        
        if (args.length < 1) {
            return CommandExceptionHandler.handleException(
                "class field",
                new IllegalArgumentException("参数不足, 需要至少1个参数: class field <class_name> [field_name]"),
                context.getExecContext(),
                "参数错误"
            );
        }

        boolean getValue = false;
        boolean setValue = false;
        boolean showValue = false;
        boolean showType = false;
        boolean showModifiers = false;
        boolean showAll = true;
        boolean accessSuper = false;
        boolean accessInterfaces = false;
        
        String valueToSet = null;
        String fieldName = null;
        String className = args[args.length - 1];

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            switch (arg) {
                case "-g", "--get" -> {
                    getValue = true;
                    showAll = false;
                }
                case "-s", "--set" -> {
                    setValue = true;
                    showAll = false;
                    if (i + 3 < args.length - 1) {
                        className = args[i + 1];
                        fieldName = args[i + 2];
                        valueToSet = args[i + 3];
                    } else {
                        return CommandExceptionHandler.handleException(
                            "class field",
                            new IllegalArgumentException("提供给-s的参数不足, 需要至少3个: class field -s <class> <field> <value>"),
                            context.getExecContext(),
                            "参数错误"
                        );
                    }
                }
                case "-v", "--value" -> {
                    showValue = true;
                    showAll = false;
                }
                case "-t", "--type" -> {
                    showType = true;
                    showAll = false;
                }
                case "-m", "--modifiers" -> {
                    showModifiers = true;
                    showAll = false;
                }
                case "-a", "--all" -> showAll = true;
                case "--super" -> accessSuper = true;
                case "--interfaces" -> accessInterfaces = true;
            }
        }
        
        if ((getValue || setValue || showValue) && args.length < 2) {
            return CommandExceptionHandler.handleException(
                "class field",
                new IllegalArgumentException("获取/设置字段值需要指定字段名: class field -g <class> <field>"),
                context.getExecContext(),
                "参数错误"
            );
        }
        
        if (args.length >= 2) {
            fieldName = args[args.length - 2];
            if (fieldName.startsWith("-")) {
                fieldName = null;
            }
        }
        
        context.getLogger().debug("目标类: " + className + ", 字段名: " + fieldName + ", 显示全部: " + showAll);

        Class<?> targetClass = context.loadClass(className);
        context.getLogger().info("成功加载类: " + targetClass.getName());

        if (fieldName != null) {
            Field field = ClassResolver.findStaticField(targetClass, fieldName, accessSuper, accessInterfaces);

            if (field == null) {
                return CommandExceptionHandler.handleException(
                    "class field",
                    new NoSuchFieldException("找不到字段: " + fieldName),
                    context.getExecContext(),
                    Map.of("类名", className, "字段名", fieldName),
                    "字段查找失败"
                );
            }

            field.setAccessible(true);

            if (getValue || setValue) {
                if (Modifier.isStatic(field.getModifiers())) {
                    if (getValue) {
                        Object value = field.get(null);
                        context.getExecContext().print("字段值: ", Colors.CYAN);
                        if (value != null) {
                            context.getExecContext().println(value.toString(), Colors.LIGHT_GREEN);
                        } else {
                            context.getExecContext().println("null", Colors.LIGHT_BLUE);
                        }
                        return null;
                    } else {
                        Object value = context.parseValue(valueToSet, field.getType());
                        field.set(null, value);
                        context.getExecContext().print("成功设置字段值: ", Colors.CYAN);
                        context.getExecContext().println(valueToSet, Colors.LIGHT_GREEN);
                        return null;
                    }
                } else {
                    return CommandExceptionHandler.handleException(
                        "class field",
                        new IllegalStateException("无法获取/设置非静态字段，需要提供实例对象"),
                        context.getExecContext(),
                        Map.of("类名", className, "字段名", fieldName),
                        "非静态字段访问失败"
                    );
                }
            }

            context.getExecContext().println("=== 字段信息 ===", Colors.CYAN);
            context.getExecContext().print("字段名: ", Colors.CYAN);
            context.getExecContext().println(field.getName(), Colors.CYAN);
            context.getExecContext().print("类型: ", Colors.CYAN);
            context.getExecContext().println(field.getType().getName(), Colors.GREEN);

            if (showAll || showType) {
                context.getExecContext().print("类型: ", Colors.CYAN);
                context.getExecContext().println(field.getType().getName(), Colors.GREEN);
            }

            if (showAll || showModifiers) {
                context.getExecContext().print("修饰符: ", Colors.CYAN);
                context.getExecContext().println(ReflectionUtils.getModifiersString(field.getModifiers()), Colors.DARK_BLUE);
            }

            if (showAll || showValue) {
                if (Modifier.isStatic(field.getModifiers())) {
                    try {
                        Object value = field.get(null);
                        context.getExecContext().print("值: ", Colors.CYAN);
                        if (value != null) {
                            context.getExecContext().println(value.toString(), Colors.LIGHT_GREEN);
                        } else {
                            context.getExecContext().println("null", Colors.LIGHT_BLUE);
                        }
                    } catch (Exception e) {
                        context.getExecContext().print("值: 无法获取 (", Colors.CYAN);
                        context.getExecContext().print(e.getMessage(), Colors.RED);
                        context.getExecContext().println(")", Colors.CYAN);
                    }
                } else {
                    context.getExecContext().println("值: 非静态字段，需要实例对象", Colors.GRAY);
                }
            }

            if (showAll) {
                context.getExecContext().print("声明类: ", Colors.CYAN);
                context.getExecContext().println(field.getDeclaringClass().getName(), Colors.GREEN);
            }

            return null;

        } else {
            Field[] fields = targetClass.getDeclaredFields();
            
            context.getExecContext().println("=== 字段列表 ===", Colors.CYAN);
            context.getExecContext().print("类: ", Colors.CYAN);
            context.getExecContext().println(targetClass.getName(), Colors.GREEN);
            context.getExecContext().print("字段总数: ", Colors.CYAN);
            context.getExecContext().println(String.valueOf(fields.length), Colors.YELLOW);
            context.getExecContext().println("");
            
            if (fields.length == 0) {
                context.getExecContext().println("无字段", Colors.GRAY);
            } else {
                for (Field field : fields) {
                    context.getExecContext().print("  ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(context.getExecContext(), field, true);
                    context.getExecContext().println("");
                }
            }
            
            return null;
        }
    }

    @Override
    public String getHelpText() {
        return """
            语法: class field [options] <class_name> [field_name]
            
            查看类的字段详细信息或获取/设置字段值.
            
            选项:
                -g, --get <参数>   获取字段值 (需要提供字段名)
                -s, --set <参数>   设置字段值 (需要提供字段名)
                -v, --value       显示字段值
                -t, --type        显示字段类型
                -m, --modifiers   显示修饰符
                -a, --all         显示所有信息 (默认)
            
            示例:
                class field java.lang.String
                class field -g java.lang.System out
                class field -s com.justnothing.testmodule.SomeClass someField "something"
            """;
    }


}
