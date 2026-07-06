package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.utils.expr.ExpressionParser;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public record ClassCommandContext<Req extends ClassCommandRequest>
         (String[] args, ClassLoader classLoader,
          String targetPackage,
          CommandExecutor.CmdExecContext<Req> execContext,
          Logger logger) {

    public String formatValue(Object value, boolean rawOutput) {
        if (value == null) {
            return "null";
        }
        if (rawOutput) {
            return value.toString();
        }
        if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        }
        return value.toString();
    }

    public Object parseValue(String value, Class<?> type) {
        return ExpressionParser.parse(value, classLoader, type).value();
    }

    public String[] parseParams(String paramsStr) {
        return ExpressionParser.parseParams(paramsStr);
    }

    public Object[] convertParams(String[] params, Class<?>[] paramTypes) {
        return ExpressionParser.convertParams(params, paramTypes, classLoader);
    }

    public static Method findMethod(@NotNull Class<?> clazz, String methodName, Class<?>[] paramTypes,
                             boolean staticOnly, boolean accessSuper, boolean accessInterfaces) {
        Class<?> currentClass = clazz;

        while (currentClass != null) {
            // 使用 getDeclaredMethods() 而非 getMethods()，以便能找到 private/protected/包级私有方法
            Method[] methods = currentClass.getDeclaredMethods();

            for (Method m : methods) {
                if (!m.getName().equals(methodName)) continue;
                if (ReflectionUtils.isApplicableArgs(m.getParameterTypes(), paramTypes, m.isVarArgs())) {
                    if (staticOnly && !Modifier.isStatic(m.getModifiers())) continue;
                    return m;
                }
            }

            if (accessSuper) {
                currentClass = currentClass.getSuperclass();
            } else {
                break;
            }
        }

        if (accessInterfaces) {
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> _interface : interfaces) {
                Method[] methods = _interface.getDeclaredMethods();
                for (Method m : methods) {
                    if (!m.getName().equals(methodName)) continue;
                    if (ReflectionUtils.isApplicableArgs(m.getParameterTypes(), paramTypes, m.isVarArgs())) {
                        if (staticOnly && !Modifier.isStatic(m.getModifiers())) continue;
                        return m;
                    }
                }
            }
        }

        return null;
    }
}
