package com.justnothing.testmodule.command.functions.classcmd;


import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FieldCommand extends AbstractClassCommand {

    public FieldCommand() {
        super("class field");
    }

    @Override
    protected String executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        
        if (args.length < 1) {
            context.getLogger().warn("参数不足, 需要至少1个参数");
            return getHelpText();
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
                    if (i + 1 < args.length - 1) {
                        valueToSet = args[i + 1];
                        i++;
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
            return "错误: 获取/设置字段值需要指定字段名\n" + getHelpText();
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
                return "找不到字段: " + fieldName + "\n" + getHelpText();
            }

            field.setAccessible(true);

            if (getValue || setValue) {
                if (Modifier.isStatic(field.getModifiers())) {
                    if (getValue) {
                        Object value = field.get(null);
                        return "字段值: " + (value != null ? value.toString() : "null");
                    } else {
                        Object value = context.parseValue(valueToSet, field.getType());
                        field.set(null, value);
                        return "成功设置字段值: " + valueToSet;
                    }
                } else {
                    return "错误: 无法获取/设置非静态字段，需要提供实例对象";
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== 字段信息 ===\n");
            sb.append("字段名: ").append(field.getName()).append("\n");
            sb.append("类型: ").append(field.getType().getName()).append("\n");

            if (showAll || showType) {
                sb.append("类型: ").append(field.getType().getName()).append("\n");
            }

            if (showAll || showModifiers) {
                sb.append("修饰符: ").append(ReflectionUtils.getModifiersString(field.getModifiers())).append("\n");
            }

            if (showAll || showValue) {
                if (Modifier.isStatic(field.getModifiers())) {
                    try {
                        Object value = field.get(null);
                        sb.append("值: ").append(value != null ? value.toString() : "null").append("\n");
                    } catch (Exception e) {
                        sb.append("值: 无法获取 (").append(e.getMessage()).append(")\n");
                    }
                } else {
                    sb.append("值: 非静态字段，需要实例对象\n");
                }
            }

            if (showAll) {
                sb.append("声明类: ").append(field.getDeclaringClass().getName()).append("\n");
            }

            return sb.toString();

        } else {
            Field[] fields = targetClass.getDeclaredFields();
            StringBuilder sb = new StringBuilder();
            
            sb.append("=== 字段列表 ===\n");
            sb.append("类: ").append(targetClass.getName()).append("\n");
            sb.append("字段总数: ").append(fields.length).append("\n\n");
            
            if (fields.length == 0) {
                sb.append("无字段\n");
            } else {
                for (Field field : fields) {
                    sb.append("  ").append(ReflectionUtils.getDescriptor(field)).append("\n");
                }
            }
            
            return sb.toString();
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
