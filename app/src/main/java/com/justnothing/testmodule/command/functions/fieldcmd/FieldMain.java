package com.justnothing.testmodule.command.functions.fieldcmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_FIELD_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedHelpers;

public class FieldMain {

    public static class FieldLogger extends Logger {
        @Override
        public String getTag() {
            return "FieldExecutor";
        }
    }

    public static final FieldLogger logger = new FieldLogger();

    public static String getHelpText() {
        return String.format("""
                语法: field [选项] <类名> [字段名]
                
                查看类的字段详细信息或获取/设置字段值.
                
                选项:
                    -g, --get <值>    获取字段值 (需要提供字段名)
                    -s, --set <值>    设置字段值 (需要提供字段名)
                    -v, --value       显示字段值
                    -t, --type        显示字段类型
                    -m, --modifiers   显示修饰符
                    -a, --all         显示所有信息 (默认)
                
                示例:
                    field java.lang.String
                    field -g java.lang.System out
                    field -s java.lang.System out "test"
                    field -v com.example.MyClass myField
                
                (Submodule field %s)
                """, CMD_FIELD_VER);
    }

    public static String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();
        
        logger.debug("执行field命令，参数: " + java.util.Arrays.toString(args));
        logger.debug("目标包: " + targetPackage + ", 类加载器: " + classLoader);
        
        if (args.length < 1) {
            logger.warn("参数不足，需要至少1个参数");
            return getHelpText();
        }

        boolean getValue = false;
        boolean setValue = false;
        boolean showValue = false;
        boolean showType = false;
        boolean showModifiers = false;
        boolean showAll = true;
        
        String valueToSet = null;
        String fieldName = null;
        String className = args[args.length - 1];

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            if (arg.equals("-g") || arg.equals("--get")) {
                getValue = true;
                showAll = false;
            } else if (arg.equals("-s") || arg.equals("--set")) {
                setValue = true;
                showAll = false;
                if (i + 1 < args.length - 1) {
                    valueToSet = args[i + 1];
                    i++;
                }
            } else if (arg.equals("-v") || arg.equals("--value")) {
                showValue = true;
                showAll = false;
            } else if (arg.equals("-t") || arg.equals("--type")) {
                showType = true;
                showAll = false;
            } else if (arg.equals("-m") || arg.equals("--modifiers")) {
                showModifiers = true;
                showAll = false;
            } else if (arg.equals("-a") || arg.equals("--all")) {
                showAll = true;
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
        
        logger.debug("目标类: " + className + ", 字段名: " + fieldName + ", 显示全部: " + showAll);

        try {
            Class<?> targetClass;
            try {
                logger.debug("尝试加载类: " + className);
                if (classLoader == null) {
                    logger.debug("使用默认类加载器");
                    targetClass = XposedHelpers.findClass(className, null);
                } else {
                    logger.debug("使用提供的类加载器: " + classLoader);
                    targetClass = XposedHelpers.findClass(className, classLoader);
                }
                logger.info("成功加载类: " + targetClass.getName());
            } catch (Throwable e) {
                logger.error("加载类失败: " + className, e);
                logger.warn("类加载器为: " + targetPackage);
                return "找不到类: " + className +
                        "\n使用的包: " + (targetPackage != null ? targetPackage : "default") +
                        "\n类加载器: " + (classLoader != null ? classLoader : "无") +
                        "\n错误信息: " + e.getMessage() + "\n堆栈追踪: " + Log.getStackTraceString(e);
            }

            if (fieldName != null) {
                try {
                    Field field = targetClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    
                    if (getValue || setValue) {
                        if (Modifier.isStatic(field.getModifiers())) {
                            if (getValue) {
                                Object value = field.get(null);
                                return "字段值: " + (value != null ? value.toString() : "null");
                            } else if (setValue) {
                                Object value = parseValue(valueToSet, field.getType());
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
                    
                    if (showAll || showModifiers) {
                        sb.append("修饰符: ").append(getModifiers(field.getModifiers())).append("\n");
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
                        sb.append("是否为final: ").append(Modifier.isFinal(field.getModifiers())).append("\n");
                        sb.append("是否为volatile: ").append(Modifier.isVolatile(field.getModifiers())).append("\n");
                        sb.append("是否为transient: ").append(Modifier.isTransient(field.getModifiers())).append("\n");
                    }
                    
                    return sb.toString();
                    
                } catch (NoSuchFieldException e) {
                    return "找不到字段: " + fieldName + "\n" + getHelpText();
                }
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
                        sb.append("  ").append(getFieldDescriptor(field)).append("\n");
                    }
                }
                
                return sb.toString();
            }

        } catch (Exception e) {
            logger.error("执行field命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private static Object parseValue(String value, Class<?> type) {
        if (value == null) return null;
        
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        } else if (type == char.class || type == Character.class) {
            return value.charAt(0);
        } else {
            return value;
        }
    }

    private static String getModifiers(int modifiers) {
        StringBuilder sb = new StringBuilder();
        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");
        
        if (Modifier.isStatic(modifiers)) sb.append("static ");
        if (Modifier.isFinal(modifiers)) sb.append("final ");
        if (Modifier.isSynchronized(modifiers)) sb.append("synchronized ");
        if (Modifier.isVolatile(modifiers)) sb.append("volatile ");
        if (Modifier.isTransient(modifiers)) sb.append("transient ");
        if (Modifier.isNative(modifiers)) sb.append("native ");
        if (Modifier.isAbstract(modifiers)) sb.append("abstract ");
        if (Modifier.isStrict(modifiers)) sb.append("strictfp ");
        if (Modifier.isInterface(modifiers)) sb.append("interface ");
        
        return sb.toString().trim();
    }

    private static String getFieldDescriptor(Field field) {
        StringBuilder sb = new StringBuilder();
        int modifiers = field.getModifiers();

        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");

        if (Modifier.isStatic(modifiers)) sb.append("static ");
        if (Modifier.isFinal(modifiers)) sb.append("final ");
        if (Modifier.isSynchronized(modifiers)) sb.append("synchronized ");
        if (Modifier.isNative(modifiers)) sb.append("native ");
        if (Modifier.isAbstract(modifiers)) sb.append("abstract ");
        if (Modifier.isStrict(modifiers)) sb.append("strictfp ");
        if (Modifier.isVolatile(modifiers)) sb.append("volatile ");
        if (Modifier.isTransient(modifiers)) sb.append("transient ");
        if (Modifier.isInterface(modifiers)) sb.append("interface ");

        sb.append(field.getType().getSimpleName()).append(" ");
        sb.append(field.getName());
        return sb.toString();
    }
}
