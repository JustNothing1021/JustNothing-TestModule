package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InvokeCommand extends AbstractClassCommand {

    public InvokeCommand() {
        super("class invoke");
    }

    @Override
    protected String executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        
        if (args.length < 2) {
            context.getLogger().warn("提供的参数不足, 需要至少2个参数");
            return getHelpText();
        }

        boolean accessSuper = false;
        boolean accessInterfaces = false;
        
        String className;
        String methodName;
        
        int argIndex = 0;
        while (argIndex < args.length) {
            String arg = args[argIndex];
            if (arg.equals("--super")) {
                accessSuper = true;
                argIndex++;
            } else if (arg.equals("--interfaces")) {
                accessInterfaces = true;
                argIndex++;
            } else {
                break;
            }
        }
        
        if (argIndex + 2 > args.length) {
            context.getLogger().warn("提供的参数不足");
            return getHelpText();
        }
        
        className = args[argIndex];
        methodName = args[argIndex + 1];
        argIndex += 2;

        Class<?> targetClass = context.loadClass(className);

        List<Object> params = new ArrayList<>();
        List<Class<?>> paramTypes = new ArrayList<>();

        for (int i = argIndex; i < args.length; i++) {
            String paramStr = args[i];
            int colonIndex = paramStr.indexOf(':');
            if (colonIndex <= 0) {
                context.getLogger().warn("参数形式不正确，获取到的: " + paramStr);
                return "参数形式不正确: " + paramStr +
                        "; 应为Type:value" +
                        "\n\n示例: " +
                        "\n    Integer:123, String:\"hello\", Boolean:true";
            }

            String typeName = paramStr.substring(0, colonIndex);
            String valueExpr = paramStr.substring(colonIndex + 1);

            try {
                Object parsedValue = TypeParser.parse(typeName, valueExpr, context.getClassLoader());
                params.add(parsedValue);
                paramTypes.add(parsedValue == null ? Void.class : parsedValue.getClass());
                context.getLogger().info("参数" + (params.size() - 1) +
                        ": (" + paramTypes.get(paramTypes.size()-1).getName() +")" +
                        params.get(paramTypes.size()-1).toString());
            } catch (Exception e) {
                Map<String, Object> errContext = Map.of(
                        "参数索引", i-1,
                        "参数类型", typeName,
                        "参数值表达式", valueExpr,
                        "错误信息", e.getMessage() == null ? e.getMessage() : "没有详细信息"
                );
                return CommandExceptionHandler.handleException("class invoke", e, context.getLogger(), errContext, "无法解析参数" + paramStr);
            }
        }

        Method method = ClassCommandContext.findMethod(targetClass, methodName,
                paramTypes.toArray(new Class<?>[0]), true, accessSuper, accessInterfaces);

        if (method == null) {
            method = ClassCommandContext.findMethod(targetClass, methodName,
                    paramTypes.toArray(new Class<?>[0]), false, accessSuper, accessInterfaces);

            if (method == null) {
                context.getLogger().warn("没有找到类" + className + "的方法" + methodName);
                StringBuilder sb = new StringBuilder();
                sb.append("没有找到方法: ").append(methodName).append("(");
                for (int i = 0; i < paramTypes.size(); i++) {
                    sb.append(paramTypes.get(i).getSimpleName());
                    if (i < paramTypes.size() - 1) sb.append(", ");
                }
                sb.append(")\n");

                sb.append("目前找到符合名称 '").append(methodName).append("' 的方法有:\n");
                boolean found = false;
                for (Method m : targetClass.getDeclaredMethods()) {
                    if (m.getName().contains(methodName)) {
                        sb.append("  ");
                        context.getLogger().warn("但是找到了类似的方法" + ReflectionUtils.getDescriptor(m));
                        sb.append(ReflectionUtils.getDescriptor(m));
                        sb.append("\n");
                        found = true;
                    }
                }
                if (!found) sb.append("(暂无)");
                return sb.toString();
            }
        }

        method.setAccessible(true);
        context.getExecContext().output().println("找到了对应方法, 开始调用...");
        Object result;

        if (Modifier.isStatic(method.getModifiers())) {
            result = ReflectionUtils.callStaticMethod(className, methodName, params);
        } else {
            result = findSingletonInstance(targetClass, context);
            if (result == null) {
                try {
                    result = targetClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    Map<String, Object> errContext = Map.of(
                            "类名", className,
                            "方法名", methodName,
                            "错误信息", e.getMessage() == null ? e.getMessage() : "没有详细信息"
                    );
                    return CommandExceptionHandler.handleException("class invoke", e, context.getLogger(), errContext, "非静态方法需要一个示例，在创建实例的时候出现错误");
                }
            }
            result = ReflectionUtils.callMethod(result, methodName, params);
        }

        if (result == null) {
            context.getLogger().info("调用成功，返回: null");
            return "结果: null";
        } else {
            context.getLogger().info("调用成功，返回：(" + result.getClass().getName() + result);
            return "结果: \n==========================\n" +
                    result +
                    "\n==========================" +
                    "\n类型: " + result.getClass().getName() +
                    "\nHash: " + System.identityHashCode(result);
        }
    }

    @Override
    public String getHelpText() {
        return """
            语法: class invoke <class> <method> [params...]
            
            调用某个类中的单一方法.
            提供参数的格式: Type:value (e.g. Integer:114514)
            
            示例:
                class invoke java.lang.Integer parseInt String:"123"
                class invoke android.app.ActivityThread currentActivityThread
            """;
    }



    private Object findSingletonInstance(Class<?> clazz, ClassCommandContext context) {
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
                        context.getLogger().debug("找到单例实例: " + fieldName);
                        return instance;
                    }
                }
            } catch (Exception e) {
                context.getLogger().debug("未找到单例字段: " + fieldName);
            }
        }
        
        return null;
    }

}
