package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.CommandContext;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class ClassCommandContext extends CommandContext {
    public ClassCommandContext(String[] args, ClassLoader classLoader, String targetPackage, 
                               CommandExecutor.CmdExecContext context, Logger logger) {
        super(args, classLoader, targetPackage, context, logger);
    }


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
        return ReflectionUtils.parseValue(value, type);
    }

    public String[] parseParams(String paramsStr) {
        return ReflectionUtils.parseParams(paramsStr);
    }

    public Object[] convertParams(String[] params, Class<?>[] paramTypes) {
        return ReflectionUtils.convertParams(params, paramTypes);
    }

     static Method findMethod(@NotNull Class<?> clazz, String methodName, Class<?>[] paramTypes,
                              boolean staticOnly, boolean accessSuper, boolean accessInterfaces) {
        Class<?> currentClass = clazz;

        while (currentClass != null) {
            Method[] methods = clazz.getMethods();

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
