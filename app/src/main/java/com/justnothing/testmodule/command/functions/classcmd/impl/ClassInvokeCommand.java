package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeMethodRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeMethodResult;
import com.justnothing.testmodule.utils.expr.ExpressionParser;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.format.DescriptorColorizer;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SubCommandInfo(
    description = ClassTexts.SUB_CLASS_INVOKE_DESC,
    usage = "class invoke [options] <class_name> <method_name> [args...]",
    examples = {
        "class invoke java.lang.Integer parseInt \"123\"",
        "class invoke java.lang.Integer parseInt String:\"123\"",
        "class invoke java.lang.Math max 10 20",
        "class invoke java.lang.Math max int:10 int:20",
        "class invoke android.app.ActivityThread currentActivityThread",
        "class invoke com.example.MyClass myMethod \"text\" 123 true"
    },
    optionsDesc = ClassTexts.SUB_CLASS_INVOKE_OPTIONS
)
public class ClassInvokeCommand extends AbstractClassCommand<InvokeMethodRequest, InvokeMethodResult> {

    public ClassInvokeCommand() {
        super("class invoke", InvokeMethodRequest.class, InvokeMethodResult.class);
    }

    @Override
    protected InvokeMethodResult executeClassCommand(ClassCommandContext<InvokeMethodRequest> context) throws Exception {
        InvokeMethodRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();
        String methodName = request.getMethodName();
        List<String> rawParams = request.getParams();
        List<String> rawParamTypes = request.getParamTypes();

        if (className == null || className.isEmpty() || methodName == null || methodName.isEmpty()) {
            throw new IllegalCommandLineArgumentException(Text.zhEn(
                    "参数不足, 需要至少2个参数: class invoke <class> <method> [params...]",
                    "Not enough arguments; at least 2 are required: class invoke <class> <method> [params...]").text());
        }

        boolean accessSuper = request.isAccessSuper();
        boolean accessInterfaces = request.isAccessInterfaces();
        boolean isStaticMode = request.isStatic();

        InvokeMethodResult result = new InvokeMethodResult();

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());

        List<Object> params = new ArrayList<>();
        List<Class<?>> paramTypes = new ArrayList<>();
        List<String> imports = new ArrayList<>();
        imports.add("java.util.*");
        imports.add("java.lang.*");
        imports.add(className);

        for (int i = 0; i < rawParams.size(); i++) {
            String paramStr = rawParams.get(i);
            String paramTypeStr = rawParamTypes.get(i);
            Class<?> paramType;
            try {
                ExpressionParser.ParseResult parseResult;
                if (!paramTypeStr.isEmpty()) {
                    paramType = ClassResolver.findClassWithImportsOrFail(
                            paramTypeStr, context.classLoader(), imports
                    );
                    parseResult = ExpressionParser.parse(paramStr, context.classLoader(), paramType, imports);
                } else {
                    parseResult = ExpressionParser.parse(paramStr, context.classLoader(), imports);
                    paramType = parseResult.value() == null ? Void.class : parseResult.value().getClass();
                }


                params.add(parseResult.value());
                paramTypes.add(paramType);

                String typeHint = parseResult.hasTypeHint() ? " (有类型提示)" : "";
                String valueStr = parseResult.value() != null ? parseResult.value().toString() : "null";
                context.logger().info("参数" + (params.size() - 1) +
                        ": (" + parseResult.type().getName() + ")" + valueStr + typeHint);
            } catch (Exception e) {
                Map<String, Object> errContext = Map.of(
                        CliMessages.CONTEXT_PARAM_INDEX.text(), i,
                        CliMessages.CONTEXT_PARAM_EXPRESSION.text(), paramStr,
                        CliMessages.CONTEXT_ERROR_MESSAGE.text(),
                        e.getMessage() != null ? e.getMessage() : ClassTexts.TEXT_NO_DETAILS.text()
                );
                CommandExceptionHandler.handleException("class invoke", e, context.execContext(), errContext,
                        Text.zhEn("无法解析参数: %s", "Cannot parse the argument: %s").format(paramStr));
                result.setSuccess(false);
                return result;
            }
        }

        if (!params.isEmpty()) {
            context.execContext().println(ClassTexts.LABEL_CALL_PARAMS.text(), Colors.BLUE);
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

        Method method = ClassCommandContext.findMethod(targetClass, methodName,
                paramTypes.toArray(new Class<?>[0]), true, accessSuper, accessInterfaces);

        if (method == null) {
            method = ClassCommandContext.findMethod(targetClass, methodName,
                    paramTypes.toArray(new Class<?>[0]), false, accessSuper, accessInterfaces);

            if (method == null) {
                context.logger().warn("没有找到类" + className + "的方法" + methodName);
                context.execContext().print(Text.zhEn("没有找到方法: ", "Method not found: ").text(), Colors.RED);
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
                context.execContext().print(Text.zhEn("目前找到符合名称 '", "Methods whose name contains '").text(), Colors.CYAN);
                context.execContext().print(methodName, Colors.YELLOW);
                context.execContext().println(Text.zhEn("' 的方法有:", "' :").text(), Colors.CYAN);
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
                    context.execContext().println(Text.zhEn("(暂无)", "(none)").text(), Colors.GRAY);
                }

                result.setSuccess(false);
                return result;
            }
        }

        context.execContext().print(ClassTexts.LABEL_FOUND_METHOD.text(), Colors.CYAN);
        DescriptorColorizer.printColoredDescriptor(context.execContext(), method, true);
        context.execContext().println("");
        context.execContext().println("");

        method.setAccessible(true);
        Object returnValue;

        if (Modifier.isStatic(method.getModifiers())) {
            if (!isStaticMode) {
                context.logger().info("检测到静态方法，自动切换为静态调用模式");
            }
            // 直接用已找到的 Method 对象调用，避免重复查找
            returnValue = ReflectionUtils.callMethod(null, method, params);
        } else {
            if (isStaticMode) {
                CommandExceptionHandler.handleException(
                    "class invoke",
                    new IllegalStateException(Text.zhEn(
                            "方法 %s 不是静态方法，但使用了 -s 选项",
                            "Method %s is not static, but the -s option was given").format(methodName)),
                    context.execContext(),
                    Map.of(CliMessages.CONTEXT_CLASS_NAME.text(), className,
                           CliMessages.CONTEXT_METHOD_NAME.text(), methodName),
                    Text.zhEn("非静态方法不能使用 -s 选项", "A non-static method cannot be invoked with -s").text()
                );
                result.setSuccess(false);
                return result;
            }

            String targetInstanceStr = request.getTargetInstance();
            Object targetInstance;

            if (targetInstanceStr != null && !targetInstanceStr.isEmpty()) {
                targetInstance = context.parseValue(targetInstanceStr, targetClass);
                context.logger().info("使用指定的目标实例: " + targetInstance);
            } else {
                targetInstance = findSingletonInstance(targetClass, context);
                if (targetInstance == null) {
                    // 单例字段为 null，尝试调用 getInstance() 静态方法初始化单例
                    targetInstance = tryInitializeSingleton(targetClass, context);
                }
                if (targetInstance == null) {
                    try {
                        // 单例字段为 null，且无法通过 getInstance() 初始化，尝试通过无参构造创建实例
                        Constructor<?> constructor = targetClass.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        targetInstance = constructor.newInstance();
                        context.logger().info("通过无参构造创建实例: " + targetInstance);
                    } catch (Exception e) {
                        Map<String, Object> errContext = Map.of(
                                CliMessages.CONTEXT_CLASS_NAME.text(), className,
                                CliMessages.CONTEXT_METHOD_NAME.text(), methodName,
                                ClassTexts.LABEL_PARAM.text(), params,
                                CliMessages.CONTEXT_ERROR_MESSAGE.text(),
                                e.getMessage() != null ? e.getMessage() : ClassTexts.TEXT_NO_DETAILS.text()
                        );
                        CommandExceptionHandler.handleException("class invoke", e, context.execContext(), errContext,
                                Text.zhEn("非静态方法需要一个实例，在创建实例的时候出现错误",
                                        "An instance is required for a non-static method, but creating one failed").text());
                        result.setSuccess(false);
                        return result;
                    }
                }
            }
            // 直接用已找到的 Method 对象调用，避免重复查找导致找到错误的方法
            returnValue = ReflectionUtils.callMethod(targetInstance, method, params);

            // 设置实例信息（用于调试）
            result.setInstanceAfterInvocation(targetInstance.getClass().getName() + "@" + System.identityHashCode(targetInstance));
            result.setInstanceHash(System.identityHashCode(targetInstance));
        }

        // 映射结果到 InvokeMethodResult 字段
        if (returnValue == null) {
            context.logger().info("调用成功，返回: null");
            context.execContext().print(ClassTexts.LABEL_RESULT.text(), Colors.CYAN);
            context.execContext().println("null", Colors.LIGHT_BLUE);
            
            result.setResultString("null");
            result.setResultTypeName("void/null");
        } else {
            context.logger().info("调用成功，返回：(" + returnValue.getClass().getName() + returnValue);
            
            result.setResultString(returnValue.toString());
            result.setResultTypeName(returnValue.getClass().getName());
            result.setResultHash(System.identityHashCode(returnValue));

            context.execContext().println(ClassTexts.LABEL_RESULT.text(), Colors.CYAN);
            context.execContext().println("==========================", Colors.CYAN);
            context.execContext().println(String.valueOf(returnValue), Colors.WHITE);
            context.execContext().println("==========================", Colors.CYAN);
            context.execContext().print(ClassTexts.LABEL_TYPE.text(), Colors.CYAN);
            context.execContext().println(returnValue.getClass().getName(), Colors.GREEN);
            context.execContext().print("Hash: ", Colors.CYAN);
            context.execContext().println(String.valueOf(System.identityHashCode(returnValue)), Colors.LIGHT_GREEN);
        }

        result.setSuccess(true);
        return result;
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

    /**
     * 尝试调用 getInstance() 静态方法初始化单例。
     * 当单例字段为 null 时（尚未初始化），调用此方法会触发单例的懒加载。
     */
    private Object tryInitializeSingleton(Class<?> clazz, ClassCommandContext<InvokeMethodRequest> context) {
        String[] factoryMethodNames = {"getInstance", "get", "getDefault", "newInstance"};

        for (String methodName : factoryMethodNames) {
            try {
                Method method = clazz.getDeclaredMethod(methodName);
                if (Modifier.isStatic(method.getModifiers())) {
                    method.setAccessible(true);
                    Object instance = method.invoke(null);
                    context.logger().info("通过 " + methodName + "() 初始化单例: " + instance);
                    return instance;
                }
            } catch (Exception e) {
                context.logger().debug("无法通过 " + methodName + "() 初始化单例");
            }
        }

        return null;
    }

}
