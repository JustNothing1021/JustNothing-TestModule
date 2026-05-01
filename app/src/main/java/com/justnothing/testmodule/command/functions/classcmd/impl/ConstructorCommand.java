package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.functions.classcmd.AbstractClassCommand;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeConstructorRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ConstructorInvokeResult;
import com.justnothing.testmodule.command.functions.classcmd.util.ExpressionParser;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstructorCommand extends AbstractClassCommand<InvokeConstructorRequest, ConstructorInvokeResult> {

    public ConstructorCommand() {
        super("class constructor", InvokeConstructorRequest.class, ConstructorInvokeResult.class);
    }

    @Override
    protected ConstructorInvokeResult executeClassCommand(ClassCommandContext<InvokeConstructorRequest> context) throws Exception {
        InvokeConstructorRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();
        List<String> rawParams = request.getParams();
        boolean freeMode = request.isFreeMode();
        String signature = request.getSignature();

        if (className == null || className.isEmpty()) {
            CommandExceptionHandler.handleException(
                "class constructor",
                new IllegalArgumentException("参数不足: class constructor <class_name> [params...]"),
                context.execContext(),
                "参数错误"
            );
            return null;
        }

        List<Object> params = new ArrayList<>();
        List<Class<?>> paramTypes = new ArrayList<>();

        if (!freeMode) {
            for (int i = 0; i < rawParams.size(); i++) {
                String paramStr = rawParams.get(i);

                try {
                    ExpressionParser.ParseResult result = ExpressionParser.parse(paramStr, context.classLoader());
                    params.add(result.value());
                    paramTypes.add(result.type());

                    String typeHint = result.hasTypeHint() ? " (有类型提示)" : "";
                    String valueStr = result.value() != null ? result.value().toString() : "null";
                    context.logger().info("参数" + (params.size() - 1) +
                            ": (" + result.type().getName() + ")" + valueStr + typeHint);
                } catch (Exception e) {
                    context.logger().warn("无法解析参数: " + paramStr);
                    Map<String, Object> errContext = new HashMap<>();
                    errContext.put("参数索引", i - 1);
                    errContext.put("参数表达式", paramStr);
                    CommandExceptionHandler.handleException("class constructor", e, context.execContext(), errContext, "解析参数失败");
                    return null;
                }
            }
        } else {
            context.logger().info("自由模式: 跳过类型推断，使用原始字符串参数");
            params.addAll(rawParams);
            paramTypes.addAll(rawParams.stream().map(s -> String.class).toList());
        }

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());
        ConstructorInvokeResult result = new ConstructorInvokeResult();
        result.setClassName(className);
        result.setSuccess(true);

        if (!params.isEmpty()) {
            context.execContext().println("调用参数：", Colors.CYAN);
            for (int i = 0; i < params.size(); i++) {
                context.execContext().print("参数", Colors.YELLOW);
                context.execContext().print("[", Colors.WHITE);
                context.execContext().print(String.valueOf(i), Colors.LIGHT_GREEN);
                context.execContext().print("]", Colors.WHITE);
                context.execContext().print(" = ", Colors.WHITE);
                Object value = params.get(i);
                if (value == null) {
                    context.execContext().print("null", Colors.LIGHT_BLUE);
                } else {
                    context.execContext().print(String.valueOf(value), Colors.LIGHT_BLUE);
                }
                context.execContext().print(" (", Colors.WHITE);
                context.execContext().print(paramTypes.get(i).getName(), Colors.GREEN);
                context.execContext().println(")", Colors.WHITE);
            }
            context.execContext().println("");
        }

        Constructor<?> constructor;
        if (signature != null && !signature.isEmpty()) {
            context.logger().info("使用签名匹配: " + signature);
            constructor = ReflectionUtils.findConstructorBySignature(targetClass, signature);
        } else if (!request.getParamTypes().isEmpty()) {
            List<Class<?>> explicitTypes = new ArrayList<>();
            for (String typeName : request.getParamTypes()) {
                try {
                    explicitTypes.add(Class.forName(typeName));
                } catch (ClassNotFoundException e) {
                    context.logger().warn("无法找到类型: " + typeName + ", 使用 String.class 代替");
                    explicitTypes.add(String.class);
                }
            }
            constructor = ReflectionUtils.findConstructor(targetClass, explicitTypes.toArray(new Class<?>[0]));
        } else {
            constructor = ReflectionUtils.findConstructor(targetClass, paramTypes.toArray(new Class<?>[0]));
        }

        if (constructor == null) {
            context.logger().warn("没有找到类" + className + "的匹配构造函数");
            context.execContext().println("没有找到匹配的构造函数", Colors.RED);
            context.execContext().print("参数类型: ", Colors.CYAN);
            for (int i = 0; i < paramTypes.size(); i++) {
                context.execContext().print(paramTypes.get(i).getName(), Colors.GREEN);
                if (i < paramTypes.size() - 1) {
                    context.execContext().print(", ", Colors.WHITE);
                }
            }
            context.execContext().println("");
            context.execContext().println("");
            context.execContext().println("可用的构造函数:", Colors.CYAN);
            for (Constructor<?> c : targetClass.getDeclaredConstructors()) {
                context.execContext().print("  ", Colors.GRAY);
                DescriptorColorizer.printColoredDescriptor(context.execContext(), c, true);
                context.execContext().println("");
            }

            result.setSuccess(false);
            return result;
        }

        context.execContext().print("找到构造函数: ", Colors.CYAN);
        DescriptorColorizer.printColoredDescriptor(context.execContext(), constructor, true);
        context.execContext().println("");
        context.execContext().println("");

        result.setConstructorInfo(MethodInfo.fromConstructor(constructor));

        constructor.setAccessible(true);
        Object instance = constructor.newInstance(params.toArray());

        result.setInstance(instance);
        result.setInstanceType(instance.getClass().getName());
        result.setInstanceHashCode(System.identityHashCode(instance));

        context.logger().info("创建实例成功: " + instance);
        context.execContext().println("创建实例成功", Colors.GREEN);
        context.execContext().println("============================", Colors.CYAN);
        context.execContext().println(String.valueOf(instance), Colors.WHITE);
        context.execContext().println("============================", Colors.CYAN);
        context.execContext().print("类型: ", Colors.CYAN);
        context.execContext().println(instance.getClass().getName(), Colors.YELLOW);
        context.execContext().print("Hash: ", Colors.CYAN);
        context.execContext().println(String.valueOf(System.identityHashCode(instance)), Colors.LIGHT_GREEN);

        return result;
    }

    @Override
    public String getHelpText() {
        return """
            语法: class constructor [options] <class_name> [params...]

            创建类的实例。
            参数支持表达式语法，可以直接写值或使用类型提示。

            参数格式:
                - 直接表达式: 123, "hello", true, null
                - 带类型提示: int:123, String:"hello", boolean:true

            表达式支持:
                - 字面量: 123, 3.14, "text", true, null
                - 算术运算: 1 + 2, 10 * 5
                - 字符串拼接: "Hello " + "World"
                - 方法调用: Math.abs(-5)
                - 字段访问: SomeClass.FIELD
                - 对象创建: new ArrayList()

            选项:
                -f, --free      自由模式（跳过类型推断）

            示例:
                class constructor java.lang.Integer 114514
                class constructor java.lang.Integer int:114514
                class constructor java.lang.String "1919810"
                class constructor java.lang.String String:"1919810"
                class constructor java.util.ArrayList
                class constructor java.io.File "/sdcard/test.txt"
            """;
    }
}
