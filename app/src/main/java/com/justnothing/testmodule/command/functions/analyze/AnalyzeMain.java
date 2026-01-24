package com.justnothing.testmodule.command.functions.analyze;

import static com.justnothing.testmodule.constants.CommandServer.CMD_ANALYZE_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedHelpers;

public class AnalyzeMain extends CommandBase {

    public AnalyzeMain() {
        super("AnalyzeExecutor");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: analyze [选项] <类名>
                
                分析一个类并显示详细信息.
                
                选项:
                    -f, --fields      只显示字段
                    -m, --methods     只显示方法
                    -a, --all         显示所有信息 (默认)
                
                示例:
                    analyze java.lang.String
                    analyze -f com.android.server.am.ActivityManagerService
                    analyze -a java.util.ArrayList
                
                (Submodule analyze %s)
                """, CMD_ANALYZE_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();
        
        logger.debug("执行analyze命令，参数: " + java.util.Arrays.toString(args));
        logger.debug("目标包: " + targetPackage + ", 类加载器: " + classLoader);
        
        if (args.length < 1) {
            logger.warn("参数不足，需要至少1个参数");
            return getHelpText();
        }

        boolean showFields = false;
        boolean showMethods = false;
        boolean showAll = true;
        String className = args[args.length - 1];

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            if (arg.equals("-f") || arg.equals("--fields")) {
                showFields = true;
                showMethods = false;
                showAll = false;
            } else if (arg.equals("-m") || arg.equals("--methods")) {
                showMethods = true;
                showFields = false;
                showAll = false;
            } else if (arg.equals("-a") || arg.equals("--all")) {
                showAll = true;
                showFields = false;
                showMethods = false;
            }
        }
        
        logger.debug("目标类: " + className + ", 显示字段: " + showFields + ", 显示方法: " + showMethods + ", 显示全部: " + showAll);

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

            StringBuilder sb = new StringBuilder();
            
            if (showAll || showFields) {
                sb.append("=== 字段 ===\n");
                Field[] fields = targetClass.getDeclaredFields();
                if (fields.length == 0) {
                    sb.append("无字段\n");
                } else {
                    for (Field field : fields) {
                        sb.append("  ").append(getFieldDescriptor(field)).append("\n");
                    }
                }
                sb.append("字段总数: ").append(fields.length).append("\n\n");
            }

            if (showAll || showMethods) {
                sb.append("=== 方法 ===\n");
                Method[] methods = targetClass.getDeclaredMethods();
                if (methods.length == 0) {
                    sb.append("无方法\n");
                } else {
                    for (Method method : methods) {
                        sb.append("  ").append(getMethodDescriptor(method)).append("\n");
                    }
                }
                sb.append("方法总数: ").append(methods.length).append("\n\n");
            }

            if (showAll) {
                sb.append("=== 类信息 ===\n");
                sb.append("类名: ").append(targetClass.getName()).append("\n");
                sb.append("简单类名: ").append(targetClass.getSimpleName()).append("\n");
                sb.append("包名: ").append(targetClass.getPackage() != null ? targetClass.getPackage().getName() : "无").append("\n");
                sb.append("是否为数组: ").append(targetClass.isArray()).append("\n");
                sb.append("是否为接口: ").append(targetClass.isInterface()).append("\n");
                sb.append("是否为注解: ").append(targetClass.isAnnotation()).append("\n");
                sb.append("是否为枚举: ").append(targetClass.isEnum()).append("\n");
                sb.append("是否为原始类型: ").append(targetClass.isPrimitive()).append("\n");
                sb.append("是否为抽象类: ").append(Modifier.isAbstract(targetClass.getModifiers())).append("\n\n");

                sb.append("=== 父类 ===\n");
                Class<?> superClass = targetClass.getSuperclass();
                if (superClass != null) {
                    sb.append(superClass.getName()).append("\n");
                } else {
                    sb.append("无父类\n");
                }
                sb.append("\n");

                sb.append("=== 实现的接口 ===\n");
                Class<?>[] interfaces = targetClass.getInterfaces();
                if (interfaces.length == 0) {
                    sb.append("无接口\n");
                } else {
                    for (Class<?> iface : interfaces) {
                        sb.append("  - ").append(iface.getName()).append("\n");
                    }
                }
                sb.append("接口总数: ").append(interfaces.length).append("\n\n");

                sb.append("=== 包信息 ===\n");
                sb.append("包: ").append(targetPackage != null ? targetPackage : "default").append("\n");
                sb.append("类加载器: ").append(classLoader != null ? classLoader.toString() : "无").append("\n");
            }
            
            logger.info("执行成功");
            logger.debug("执行结果:\n" + sb.toString());
            return sb.toString();

        } catch (Exception e) {
            logger.error("执行analyze命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String getFieldDescriptor(Field field) {
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

    private String getMethodDescriptor(Method method) {
        StringBuilder sb = new StringBuilder();
        int modifiers = method.getModifiers();

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
        

        sb.append(method.getReturnType().getSimpleName()).append(" ");
        sb.append(method.getName()).append("(");

        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i].getSimpleName());
            if (i < params.length - 1) sb.append(", ");
        }

        sb.append(")");
        return sb.toString();
    }
}
