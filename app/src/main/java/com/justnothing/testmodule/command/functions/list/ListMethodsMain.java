package com.justnothing.testmodule.command.functions.list;

import static com.justnothing.testmodule.constants.CommandServer.CMD_LIST_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

import de.robv.android.xposed.XposedHelpers;

public class ListMethodsMain {


    public static class ListLogger extends Logger {
        @Override
        public String getTag() {
            return "ListExecutor";
        }
    }

    public static final ListLogger logger = new ListLogger();

    public static String getHelpText() {
        return String.format("""
                语法: list [options] <class>
                
                列出一个类的所有方法.
                
                可选项:
                    -vb, --verbose      详细输出完整类名
                
                示例:
                    list -vb java.lang.String
                    list com.android.server.am.ActivityManagerService
                
                (Submodule list %s)
                """, CMD_LIST_VER);
    }


    public static String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();
        
        logger.debug("开始执行list命令，参数: " + Arrays.toString(args));
        logger.debug("目标包: " + targetPackage + ", 类加载器: " + classLoader);
        
        if (args.length < 1) {
            logger.warn("参数不足，需要至少1个参数");
            return getHelpText();
        }

        boolean verbose = args[0].equals("-vb") || args[0].equals("--verbose");
        if (verbose && args.length < 2) {
            logger.warn("详细模式需要指定类名");
            return getHelpText();
        }
        String className = args[args.length - 1];
        
        logger.debug("目标类名: " + className + ", 详细模式: " + verbose);

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
            sb.append("类名: ").append(className).append("\n");
            sb.append("使用的包: ").append(targetPackage != null ? targetPackage : "default").append("\n");
            sb.append("类加载器: ").append(classLoader != null ? classLoader : "无").append("\n");
            sb.append("\n方法列表:\n");

            logger.debug("开始获取类方法");
            Method[] methods = targetClass.getDeclaredMethods();
            logger.debug("找到 " + methods.length + " 个方法");
            
            Arrays.sort(methods, Comparator.comparing(Method::getName)
                    .thenComparingInt(Method::getParameterCount));

            int staticCount = 0;
            int instanceCount = 0;

            for (Method method : methods) {
                int modifiers = method.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    staticCount++;
                } else {
                    instanceCount++;
                }
                if (verbose) {
                    sb.append("  ").append(method.toString()).append("\n");
                } else {
                    sb.append("  ").append(getShortMethodDescriptor(method)).append("\n");
                }
            }

            sb.append("\n结果:\n");
            sb.append("  静态方法: ").append(staticCount).append("\n");
            sb.append("  实例方法: ").append(instanceCount).append("\n");
            sb.append("  总计: ").append(methods.length).append("\n");
            
            logger.info("执行成功，找到 " + methods.length + " 个方法 (静态: " + staticCount + ", 实例: " + instanceCount + ")");
            logger.debug("执行结果:\n" + sb.toString());
            return sb.toString();

        } catch (Exception e) {
            logger.error("执行list命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    public static String getShortMethodDescriptor(Method method) {
        StringBuilder sb = new StringBuilder();
        int modifiers = method.getModifiers();

        // 修饰符
        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");

        if (Modifier.isStatic(modifiers)) sb.append("static ");
        if (Modifier.isFinal(modifiers)) sb.append("final ");
        if (Modifier.isSynchronized(modifiers)) sb.append("synchronized ");
        if (Modifier.isNative(modifiers)) sb.append("native ");

        if (Modifier.isInterface(modifiers)) sb.append("interface "); // 其实到了下面就已经开始不对劲了
        if (Modifier.isVolatile(modifiers)) sb.append("volatile ");
        if (Modifier.isStrict(modifiers)) sb.append("strict ");
        if (Modifier.isTransient(modifiers)) sb.append("transient ");


        // 返回类型和方法名
        sb.append(method.getReturnType().getSimpleName()).append(" ");
        sb.append(method.getName()).append("(");

        // 参数
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i].getSimpleName());
            if (i < params.length - 1) sb.append(", ");
        }

        sb.append(")");
        return sb.toString();
    }
}
