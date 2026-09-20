package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.MethodListRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.MethodListResult;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.format.DescriptorColorizer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

@SubCommandInfo(
    description = ClassTexts.SUB_CLASS_LIST_DESC,
    usage = "class list [options] <class_name>",
    examples = {
        "class list java.lang.String",
        "class list -v java.util.ArrayList",
    },
    optionsDesc = ClassTexts.SUB_CLASS_LIST_OPTIONS
)
public class ClassListCommand extends AbstractClassCommand<MethodListRequest, MethodListResult> {

    public ClassListCommand() {
        super("class list", MethodListRequest.class, MethodListResult.class);
    }

    @Override
    protected MethodListResult executeClassCommand(ClassCommandContext<MethodListRequest> context) throws Exception {
        MethodListRequest request = context.execContext().getCommandRequest();
        boolean verbose = request.isVerbose();
        String className = request.getClassName();

        if (className == null || className.isEmpty()) {
            throw new IllegalCommandLineArgumentException("参数不足: class list [options] <class>");
        }

        context.logger().debug("目标类名: " + className + ", 详细模式: " + verbose);

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());
        context.logger().info("成功加载类: " + targetClass.getName());

        MethodListResult result = new MethodListResult();
        result.setClassName(className);
        result.setTargetPackage(context.targetPackage() != null ? context.targetPackage() : "default");
        result.setClassLoader(context.classLoader() != null ? context.classLoader().toString() : "无");

        context.execContext().print("类名: ", Colors.CYAN);
        context.execContext().println(className, Colors.GREEN);
        context.execContext().print("使用的包: ", Colors.CYAN);
        context.execContext().println(result.getTargetPackage(), Colors.YELLOW);
        context.execContext().print("类加载器: ", Colors.CYAN);
        context.execContext().println(result.getClassLoader(), Colors.GRAY);
        context.execContext().println("");
        context.execContext().println("方法列表:", Colors.CYAN);

        context.logger().debug("开始获取类方法");
        Method[] methods = targetClass.getDeclaredMethods();
        context.logger().debug("找到 " + methods.length + " 个方法");

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

            context.execContext().print("  ", Colors.GRAY);
            DescriptorColorizer.printColoredDescriptor(context.execContext(), method, !verbose);
            context.execContext().println("");

            result.getMethods().add(MethodInfo.fromMethod(method));
        }

        result.setStaticCount(staticCount);
        result.setInstanceCount(instanceCount);
        result.setTotalCount(methods.length);

        context.execContext().println("");
        context.execContext().println("结果:", Colors.CYAN);
        context.execContext().print("  静态方法: ", Colors.CYAN);
        context.execContext().println(String.valueOf(staticCount), Colors.YELLOW);
        context.execContext().print("  实例方法: ", Colors.CYAN);
        context.execContext().println(String.valueOf(instanceCount), Colors.YELLOW);
        context.execContext().print("  总计: ", Colors.CYAN);
        context.execContext().println(String.valueOf(methods.length), Colors.YELLOW);

        context.logger().info("执行成功，找到 " + methods.length + " 个方法 (静态: " + staticCount + ", 实例: " + instanceCount + ")");
        return result;
    }
}
