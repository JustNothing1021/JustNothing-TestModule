package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

public class ListCommand extends AbstractClassCommand {

    public ListCommand() {
        super("class list");
    }

    @Override
    protected String executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        
        boolean verbose = args.length > 0 && (args[0].equals("-v") || args[0].equals("--verbose"));
        if (verbose && args.length < 2) {
            context.getLogger().warn("详细模式需要指定类名");
            return getHelpText();
        }
        
        String className = args[args.length - 1];
        
        context.getLogger().debug("目标类名: " + className + ", 详细模式: " + verbose);

        Class<?> targetClass = context.loadClass(className);
        context.getLogger().info("成功加载类: " + targetClass.getName());

        StringBuilder sb = new StringBuilder();
        sb.append("类名: ").append(className).append("\n");
        sb.append("使用的包: ").append(context.getTargetPackage() != null ? context.getTargetPackage() : "default").append("\n");
        sb.append("类加载器: ").append(context.getClassLoader() != null ? context.getClassLoader() : "无").append("\n");
        sb.append("\n方法列表:\n");

        context.getLogger().debug("开始获取类方法");
        Method[] methods = targetClass.getDeclaredMethods();
        context.getLogger().debug("找到 " + methods.length + " 个方法");
        
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
            sb.append("  ").append(ReflectionUtils.getDescriptor(method, !verbose)).append("\n");
        }

        sb.append("\n结果:\n");
        sb.append("  静态方法: ").append(staticCount).append("\n");
        sb.append("  实例方法: ").append(instanceCount).append("\n");
        sb.append("  总计: ").append(methods.length).append("\n");
        
        context.getLogger().info("执行成功，找到 " + methods.length + " 个方法 (静态: " + staticCount + ", 实例: " + instanceCount + ")");
        context.getLogger().debug("执行结果:\n" + sb);
        return sb.toString();
    }

    @Override
    public String getHelpText() {
        return """
            语法: class list [options] <class>
        
            列出一个类的所有方法.
        
            可选项:
                -v, --verbose      详细输出完整类名
        
            示例:
                class list -v java.lang.String
                class list com.android.server.am.ActivityManagerService
            """;
    }


}
