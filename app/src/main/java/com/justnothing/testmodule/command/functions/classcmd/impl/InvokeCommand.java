package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.functions.classcmd.AbstractClassCommand;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeMethodRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.MethodInvokeResult;
import com.justnothing.testmodule.command.functions.classcmd.util.ExpressionParser;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InvokeCommand extends AbstractClassCommand<InvokeMethodRequest, MethodInvokeResult> {

    public InvokeCommand() {
        super("class invoke", InvokeMethodRequest.class, MethodInvokeResult.class);
    }

    @Override
    protected MethodInvokeResult executeClassCommand(ClassCommandContext<InvokeMethodRequest> context) throws Exception {
        InvokeMethodRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();
        String methodName = request.getMethodName();
        List<String> rawParams = request.getParams();

        if (className == null || className.isEmpty() || methodName == null || methodName.isEmpty()) {
            CommandExceptionHandler.handleException(
                "class invoke",
                new IllegalArgumentException("参数不足, 需要至少2个参数: class invoke <class> <method> [params...]"),
                context.execContext(),
                "参数错误"
            );
            return null;
        }

        boolean accessSuper = request.isAccessSuper();
        boolean accessInterfaces = request.isAccessInterfaces();
        boolean isStaticMode = request.isStatic();
        boolean freeMode = request.isFreeMode();

        MethodInvokeResult result = new MethodInvokeResult();
        result.setClassName(className);
        result.setMethodName(methodName);
        result.setSuccess(true);

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());

        List<Object> params = new ArrayList<>();
        List<Class<?>> paramTypes = new ArrayList<>();

        if (!freeMode) {
            for (int i = 0; i < rawParams.size(); i++) {
                String paramStr = rawParams.get(i);

                try {
                    ExpressionParser.ParseResult parseResult = ExpressionParser.parse(paramStr, context.classLoader());
                    params.add(parseResult.value());
                    paramTypes.add(parseResult.type());

                    String typeHint = parseResult.hasTypeHint() ? " (有类型提示)" : "";
                    String valueStr = parseResult.value() != null ? parseResult.value().toString() : "null";
                    context.logger().info("参数" + (params.size() - 1) +
                            ": (" + parseResult.type().getName() + ")" + valueStr + typeHint);
                } catch (Exception e) {
                    Map<String, Object> errContext = Map.of(
                            "参数索引", i,
                            "参数表达式", paramStr,
                            "错误信息", e.getMessage() != null ? e.getMessage() : "没有详细信息"
                    );
                    CommandExceptionHandler.handleException("class invoke", e, context.execContext(), errContext, "无法解析参数: " + paramStr);
                    result.setSuccess(false);
                    return result;
                }
            }
        } else {
            context.logger().info("自由模式: 跳过类型推断，使用原始字符串参数");
            params.addAll(rawParams);
            paramTypes.addAll(rawParams.stream().map(s -> String.class).toList());
        }

        if (!params.isEmpty()) {
            context.execContext().println("调用参数：", Colors.BLUE);
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

        Method method = ClassCommandContext.findMethod(targetClass, methodName,
                paramTypes.toArray(new Class<?>[0]), true, accessSuper, accessInterfaces);

        if (method == null) {
            method = ClassCommandContext.findMethod(targetClass, methodName,
                    paramTypes.toArray(new Class<?>[0]), false, accessSuper, accessInterfaces);

            if (method == null) {
                context.logger().warn("没有找到类" + className + "的方法" + methodName);
                context.execContext().print("没有找到方法: ", Colors.RED);
                context.execContext().print(methodName, Colors.YELLOW);
                context.execContext().print("(", Colors.MAGENTA);
                for (int i = 0; i < paramTypes.size(); i++) {
                    context.execContext().print(paramTypes.get(i).getSimpleName(), Colors.GREEN);
                    if (i < paramTypes.size() - 1) {
                        context.execContext().print(", ", Colors.WHITE);
                    }
                }
                context.execContext().println(")", Colors.MAGENTA);

                context.execContext().println("");
                context.execContext().print("目前找到符合名称 '", Colors.CYAN);
                context.execContext().print(methodName, Colors.YELLOW);
                context.execContext().println("' 的方法有:", Colors.CYAN);
                boolean found = false;
                for (Method m : targetClass.getDeclaredMethods()) {
                    if (m.getName().contains(methodName)) {
                        context.execContext().print("  ", Colors.GRAY);
                        DescriptorColorizer.printColoredDescriptor(context.execContext(), m, true);
                        context.execContext().println("");
                        found = true;
                    }
                }
                if (!found) {
                    context.execContext().println("(暂无)", Colors.GRAY);
                }

                result.setSuccess(false);
                return result;
            }
        }

        context.execContext().print("找到方法: ", Colors.CYAN);
        DescriptorColorizer.printColoredDescriptor(context.execContext(), method, true);
        context.execContext().println("");
        context.execContext().println("");

        result.setMethodInfo(MethodInfo.fromMethod(method));
        result.setStatic(Modifier.isStatic(method.getModifiers()));

        method.setAccessible(true);
        Object returnValue;

        if (Modifier.isStatic(method.getModifiers())) {
            if (!isStaticMode) {
                context.logger().info("检测到静态方法，自动切换为静态调用模式");
            }
            returnValue = ReflectionUtils.callStaticMethod(className, methodName, params);
        } else {
            if (isStaticMode) {
                CommandExceptionHandler.handleException(
                    "class invoke",
                    new IllegalStateException("方法 " + methodName + " 不是静态方法，但使用了 -s 选项"),
                    context.execContext(),
                    Map.of("类名", className, "方法名", methodName),
                    "非静态方法不能使用 -s 选项"
                );
                result.setSuccess(false);
                return result;
            }

            String targetInstanceStr = request.getTargetInstance();
            Object targetInstance = null;

            if (targetInstanceStr != null && !targetInstanceStr.isEmpty()) {
                targetInstance = context.parseValue(targetInstanceStr, targetClass);
                context.logger().info("使用指定的目标实例: " + targetInstance);
            } else {
                targetInstance = findSingletonInstance(targetClass, context);
                if (targetInstance == null) {
                    try {
                        targetInstance = targetClass.getDeclaredConstructor().newInstance();
                        context.logger().info("通过无参构造创建实例: " + targetInstance);
                    } catch (Exception e) {
                        Map<String, Object> errContext = Map.of(
                                "类名", className,
                                "方法名", methodName,
                                "参数", params,
                                "错误信息", e.getMessage() == null ? e.getMessage() : "没有详细信息"
                        );
                        CommandExceptionHandler.handleException("class invoke", e, context.execContext(), errContext, "非静态方法需要一个示例，在创建实例的时候出现错误");
                        result.setSuccess(false);
                        return result;
                    }
                }
            }
            returnValue = ReflectionUtils.callMethod(targetInstance, methodName, params);
        }

        result.setReturnValue(returnValue);

        if (returnValue == null) {
            context.logger().info("调用成功，返回: null");
            context.execContext().print("结果: ", Colors.CYAN);
            context.execContext().println("null", Colors.LIGHT_BLUE);
        } else {
            context.logger().info("调用成功，返回：(" + returnValue.getClass().getName() + returnValue);
            result.setReturnType(returnValue.getClass().getName());
            result.setReturnHashCode(System.identityHashCode(returnValue));

            context.execContext().println("结果:", Colors.CYAN);
            context.execContext().println("==========================", Colors.CYAN);
            context.execContext().println(String.valueOf(returnValue), Colors.WHITE);
            context.execContext().println("==========================", Colors.CYAN);
            context.execContext().print("类型: ", Colors.CYAN);
            context.execContext().println(returnValue.getClass().getName(), Colors.GREEN);
            context.execContext().print("Hash: ", Colors.CYAN);
            context.execContext().println(String.valueOf(System.identityHashCode(returnValue)), Colors.LIGHT_GREEN);
        }

        return result;
    }

    @Override
    public String getHelpText() {
        return """
            语法: class invoke [options] <class> <method> [params...]

            调用某个类中的单一方法。
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
                - 三元运算: x > 0 ? x : -x

            选项:
                --super       查找父类方法
                --interfaces  查找接口方法
                -s            调用静态方法
                -f, --free    自由模式（跳过类型推断）

            示例:
                class invoke java.lang.Integer parseInt "123"
                class invoke java.lang.Integer parseInt String:"123"
                class invoke java.lang.Math max 10 20
                class invoke java.lang.Math max int:10 int:20
                class invoke android.app.ActivityThread currentActivityThread
                class invoke com.example.MyClass myMethod "text" 123 true
            """;
    }



    private Object findSingletonInstance(Class<?> clazz, ClassCommandContext<InvokeMethodRequest> context) {
        String[] singletonFieldNames = {
                "INSTANCE", "instance", "mInstance", "sInstance",
                "sSingleton", "mSingleton", "gInstance"
        };

        for (String fieldName : singletonFieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                if (Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    Object instance = field.get(null);
                    if (clazz.isInstance(instance)) {
                        context.logger().debug("找到单例实例: " + fieldName);
                        return instance;
                    }
                }
            } catch (Exception e) {
                context.logger().debug("未找到单例字段: " + fieldName);
            }
        }

        return null;
    }

}
