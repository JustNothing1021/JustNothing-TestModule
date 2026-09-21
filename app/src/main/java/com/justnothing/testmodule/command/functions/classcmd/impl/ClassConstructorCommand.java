package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeConstructorRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeConstructorResult;
import com.justnothing.testmodule.utils.expr.ExpressionParser;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.format.DescriptorColorizer;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SubCommandInfo(
    description = ClassTexts.SUB_CLASS_CONSTRUCTOR_DESC,
    usage = ClassTexts.SUB_CLASS_CONSTRUCTOR_USAGE,
    examples = {
        "class constructor java.lang.Integer 114514",
        "class constructor java.lang.Integer int:114514",
        "class constructor java.lang.String \"1919810\"",
        "class constructor java.lang.String String:\"1919810\"",
        "class constructor java.util.ArrayList",
        "class constructor java.io.File \"/sdcard/test.txt\""
    },
    optionsDesc = ClassTexts.SUB_CLASS_CONSTRUCTOR_OPTIONS
)
public class ClassConstructorCommand extends AbstractClassCommand<InvokeConstructorRequest, InvokeConstructorResult> {

    public ClassConstructorCommand() {
        super("class constructor", InvokeConstructorRequest.class, InvokeConstructorResult.class);
    }

    @Override
    protected InvokeConstructorResult executeClassCommand(ClassCommandContext<InvokeConstructorRequest> context) throws Exception {
        InvokeConstructorRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();
        List<String> rawParams = request.getParams();
        boolean freeMode = request.isFreeMode();
        String signature = request.getSignature();

        if (className == null || className.isEmpty()) {
            throw new IllegalCommandLineArgumentException(Text.zhEn(
                    "参数不足: class constructor <class_name> [params...]",
                    "Not enough arguments: class constructor <class_name> [params...]").text());
        }

        List<Object> params = new ArrayList<>();
        List<Class<?>> paramTypes = new ArrayList<>();
        List<String> imports = new ArrayList<>();
        imports.add("java.util.*");
        imports.add("java.lang.*");
        imports.add(className);

        for (int i = 0; i < rawParams.size(); i++) {
            String paramStr = rawParams.get(i);
            String paramTypeStr = request.getParamTypes().size() > i ? request.getParamTypes().get(i) : "";
            Class<?> paramType;
            try {
                ExpressionParser.ParseResult parseResult;
                if (!paramTypeStr.isEmpty()) {
                    paramType = ClassResolver.findClassWithImportsOrFail(
                            paramTypeStr, context.classLoader(), imports
                    );
                    parseResult = ExpressionParser.parse(paramStr, context.classLoader(), paramType);
                } else {
                    parseResult = ExpressionParser.parse(paramStr, context.classLoader());
                    paramType = parseResult.value() == null ? Void.class : parseResult.value().getClass();
                }

                params.add(parseResult.value());
                paramTypes.add(paramType);

                String typeHint = parseResult.hasTypeHint() ? " (有类型提示)" : "";
                String valueStr = parseResult.value() != null ? parseResult.value().toString() : "null";
                context.logger().info("参数" + (params.size() - 1) +
                        ": (" + parseResult.type().getName() + ")" + valueStr + typeHint);
            } catch (Exception e) {
                context.logger().warn("无法解析参数: " + paramStr);
                Map<String, Object> errContext = new HashMap<>();
                errContext.put(CliMessages.CONTEXT_PARAM_INDEX.text(), i);
                errContext.put(CliMessages.CONTEXT_PARAM_EXPRESSION.text(), paramStr);
                CommandExceptionHandler.handleException("class constructor", e, context.execContext(), errContext,
                        Text.zhEn("解析参数失败", "Failed to parse the argument").text());
                return null;
            }
        }

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());
        InvokeConstructorResult result = new InvokeConstructorResult();

        if (!params.isEmpty()) {
            context.execContext().println(ClassTexts.LABEL_CALL_PARAMS.text(), Colors.CYAN);
            for (int i = 0; i < params.size(); i++) {
                context.execContext().print(ClassTexts.LABEL_PARAM.text(), Colors.YELLOW);
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
            context.execContext().println(Text.zhEn("没有找到匹配的构造函数", "No matching constructor found").text(), Colors.RED);
            context.execContext().print(Text.zhEn("参数类型: ", "Parameter types: ").text(), Colors.CYAN);
            for (int i = 0; i < paramTypes.size(); i++) {
                context.execContext().print(paramTypes.get(i).getName(), Colors.GREEN);
                if (i < paramTypes.size() - 1) {
                    context.execContext().print(", ", Colors.WHITE);
                }
            }
            context.execContext().println("");
            context.execContext().println("");
            context.execContext().println(Text.zhEn("可用的构造函数:", "Available constructors:").text(), Colors.CYAN);
            for (Constructor<?> c : targetClass.getDeclaredConstructors()) {
                context.execContext().print("  ", Colors.GRAY);
                DescriptorColorizer.printColoredDescriptor(context.execContext(), c, true);
                context.execContext().println("");
            }

            result.setSuccess(false);
            return result;
        }

        context.execContext().print(ClassTexts.LABEL_FOUND_CONSTRUCTOR.text(), Colors.CYAN);
        DescriptorColorizer.printColoredDescriptor(context.execContext(), constructor, true);
        context.execContext().println("");
        context.execContext().println("");

        constructor.setAccessible(true);
        Object instance = constructor.newInstance(params.toArray());

        // 映射结果到 InvokeConstructorResult 字段
        result.setResultString(instance.toString());
        result.setResultTypeName(instance.getClass().getName());
        result.setResultHash(System.identityHashCode(instance));

        context.logger().info("创建实例成功: " + instance);
        context.execContext().println(Text.zhEn("创建实例成功", "Instance created").text(), Colors.GREEN);
        context.execContext().println("============================", Colors.CYAN);
        context.execContext().println(String.valueOf(instance), Colors.WHITE);
        context.execContext().println("============================", Colors.CYAN);
        context.execContext().print(ClassTexts.LABEL_TYPE.text(), Colors.CYAN);
        context.execContext().println(instance.getClass().getName(), Colors.YELLOW);
        context.execContext().print("Hash: ", Colors.CYAN);
        context.execContext().println(String.valueOf(System.identityHashCode(instance)), Colors.LIGHT_GREEN);

        result.setSuccess(true);
        return result;
    }
}
