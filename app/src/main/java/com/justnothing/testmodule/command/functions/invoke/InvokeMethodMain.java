package com.justnothing.testmodule.command.functions.invoke;

import static com.justnothing.testmodule.constants.CommandServer.CMD_INVOKE_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class InvokeMethodMain {

    public static class InvokeLogger extends Logger {
        @Override
        public String getTag() {
            return "InvokeExecutor";
        }
    }
    public static final InvokeLogger logger = new InvokeLogger();


    public static String getHelpText() {
        return String.format("""
                命令语法: invoke <class> <method> [params...]
                    提供参数的格式: Type:value (e.g. Integer:114514)
                
                用来调用某个类中的单一方法.
                
                示例:
                    invoke java.lang.Integer parseInt String:"123"
                    invoke android.app.ActivityThread currentActivityThread
                
                (Submodule invoke %s)
                """, CMD_INVOKE_VER);
    }



    public static String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();
        if (args.length < 2) {
            logger.warn("提供的参数不足");
            return getHelpText();
        }

        try {
            String className = args[0];
            String methodName = args[1];

            Class<?> targetClass;
            try {
                targetClass = XposedHelpers.findClass(className, classLoader);
            } catch (Throwable e) {
                logger.warn("没有找到类" + className);
                return "找不到类: " + className +
                        "\n类加载器: " + (targetPackage != null ? targetPackage : "default") +
                        "\n错误信息: " + e.getMessage() +
                        "\n堆栈追踪: " + Log.getStackTraceString(e);
            }

            List<Object> params = new ArrayList<>();
            List<Class<?>> paramTypes = new ArrayList<>();

            for (int i = 2; i < args.length; i++) {
                String paramStr = args[i];
                int colonIndex = paramStr.indexOf(':');
                if (colonIndex <= 0) {
                    logger.warn("参数形式不正确，获取到的: " + paramStr);
                    return "参数形式不正确: " + paramStr +
                            "; 应为Type:value" +
                            "\n\n示例: " +
                            "\n    Integer:123, String:\"hello\", Boolean:true";
                }

                String typeName = paramStr.substring(0, colonIndex);
                String valueExpr = paramStr.substring(colonIndex + 1);

                try {
                    Object parsedValue = TypeParser.parse(typeName, valueExpr, classLoader);
                    params.add(parsedValue);
                    paramTypes.add(parsedValue.getClass());
                    logger.info("参数" + (params.size() - 1) +
                            ": (" + paramTypes.get(paramTypes.size()-1).getName() +")" +
                            params.get(paramTypes.size()-1).toString());
                } catch (Exception e) {
                    logger.warn("无法解析参数" + paramStr);
                    return "解析参数 " + (i-1) + "失败: " + e.getMessage() +
                            "\n参数: " + paramStr +
                            "\n堆栈追踪: " + Log.getStackTraceString(e);
                }
            }

            Method method = findMethod(targetClass, methodName,
                    paramTypes.toArray(new Class<?>[0]), true);

            if (method == null) {
                method = findMethod(targetClass, methodName,
                        paramTypes.toArray(new Class<?>[0]), false);

                if (method == null) {
                    logger.warn("没有找到类" + className + "的方法" + methodName);
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
                            logger.warn("但是找到了类似的方法" + methodDescriptor(method));
                            sb.append(methodDescriptor(method));
                            sb.append("\n");
                            found = true;
                        }
                    }
                    if (!found) sb.append("(暂无)");
                    return sb.toString();
                }
            }

            method.setAccessible(true);
            context.output().println("找到了对应方法, 开始调用...");
            Object result;

            if (Modifier.isStatic(method.getModifiers())) {
                result = method.invoke(null, params.toArray());
            } else {
                result = findSingletonInstance(targetClass);
                if (result == null) {
                    try {
                        result = targetClass.newInstance();
                    } catch (Exception e) {
                        logger.warn("尝试调用非静态方法" + className + "." + methodName + "时创建实例失败", e);
                        return "非静态方法需要一个示例，在创建实例的时候出现错误: " + e.getMessage() +
                                "\n堆栈追踪: " + Log.getStackTraceString(e);
                    }
                }
                result = method.invoke(result, params.toArray());
            }

            if (result == null) {
                logger.info("调用成功，返回: null");
                return "结果: null";
            } else {
                logger.info("调用成功，返回：(" + result.getClass().getName() + result.toString());
                return "结果: " + result.toString() +
                        "\n类型: " + result.getClass().getName() +
                        "\nHash: " + System.identityHashCode(result);
            }

        } catch (Throwable e) {
            StringBuilder sb = new StringBuilder();
            logger.info("调用失败", e);
            sb.append("调用失败: ").append(e.getMessage()).append("\n");
            Throwable cause = e.getCause();
            if (cause != null && cause != e) {
                sb.append("原因: ").append(cause.getMessage()).append("\n");
            }
            sb.append("堆栈追踪:\n");
            sb.append(Log.getStackTraceString(e));
            return sb.toString();
        }
    }


    private static Method findMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes, boolean staticOnly) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            if (!staticOnly || Modifier.isStatic(method.getModifiers())) {
                return method;
            }
            return null;
        } catch (NoSuchMethodException e) {
            // 尝试查找兼容的方法
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    if (staticOnly && !Modifier.isStatic(method.getModifiers())) {
                        continue;
                    }

                    if (method.getParameterCount() == paramTypes.length) {
                        Class<?>[] methodParams = method.getParameterTypes();
                        boolean compatible = true;

                        for (int i = 0; i < paramTypes.length; i++) {
                            if (!methodParams[i].isAssignableFrom(paramTypes[i])) {
                                compatible = false;
                                break;
                            }
                        }

                        if (compatible) {
                            return method;
                        }
                    }
                }
            }
            return null;
        }
    }


    private static Object findSingletonInstance(Class<?> clazz) {
        // 尝试常见的单例字段名
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
                        return instance;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }


    private static String methodDescriptor(Method m) {
        int modifiers = m.getModifiers();
        StringBuilder desc = new StringBuilder();
        if (Modifier.isStatic(modifiers)) {
            desc.append("static ");
        }
        desc.append(m.getReturnType().getSimpleName()).append(" ");
        desc.append(m.getName()).append("(");
        Class<?>[] p = m.getParameterTypes();
        for (int i = 0; i < p.length; i++) {
            desc.append(p[i].getSimpleName());
            if (i < p.length - 1) desc.append(", ");
        }
        desc.append(")");
        return desc.toString();
    }


}
