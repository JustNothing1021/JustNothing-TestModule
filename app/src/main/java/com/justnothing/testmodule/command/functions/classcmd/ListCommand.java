package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

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
            return CommandExceptionHandler.handleException(
                "class list",
                new IllegalArgumentException("详细模式需要指定类名: class list -v <class>"),
                context.getExecContext(),
                "参数错误"
            );
        }
        
        String className = args[args.length - 1];
        
        context.getLogger().debug("目标类名: " + className + ", 详细模式: " + verbose);

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.getClassLoader());
        context.getLogger().info("成功加载类: " + targetClass.getName());

        context.getExecContext().print("类名: ", Colors.CYAN);
        context.getExecContext().println(className, Colors.GREEN);
        context.getExecContext().print("使用的包: ", Colors.CYAN);
        context.getExecContext().println(context.getTargetPackage() != null ? context.getTargetPackage() : "default", Colors.YELLOW);
        context.getExecContext().print("类加载器: ", Colors.CYAN);
        context.getExecContext().println(context.getClassLoader() != null ? context.getClassLoader().toString() : "无", Colors.GRAY);
        context.getExecContext().println("");
        context.getExecContext().println("方法列表:", Colors.CYAN);

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
            context.getExecContext().print("  ", Colors.GRAY);
            DescriptorColorizer.printColoredDescriptor(context.getExecContext(), method, !verbose);
            context.getExecContext().println("");
        }

        context.getExecContext().println("");
        context.getExecContext().println("结果:", Colors.CYAN);
        context.getExecContext().print("  静态方法: ", Colors.CYAN);
        context.getExecContext().println(String.valueOf(staticCount), Colors.YELLOW);
        context.getExecContext().print("  实例方法: ", Colors.CYAN);
        context.getExecContext().println(String.valueOf(instanceCount), Colors.YELLOW);
        context.getExecContext().print("  总计: ", Colors.CYAN);
        context.getExecContext().println(String.valueOf(methods.length), Colors.YELLOW);
        
        context.getLogger().info("执行成功，找到 " + methods.length + " 个方法 (静态: " + staticCount + ", 实例: " + instanceCount + ")");
        return null;
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
