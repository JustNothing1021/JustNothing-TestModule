package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.CommandContext;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.Arrays;

public class ClassCommandContext extends CommandContext {
    public ClassCommandContext(String[] args, ClassLoader classLoader, String targetPackage, 
                               CommandExecutor.CmdExecContext context, Logger logger) {
        super(args, classLoader, targetPackage, context, logger);
    }

    public Class<?> loadClass(String className) throws Exception {
        if (getClassLoader() == null) {
            getLogger().debug("使用默认类加载器加载类: " + className);
            return ClassResolver.findClassOrFail(className);
        } else {
            getLogger().debug("使用提供的类加载器加载类: " + className);
            return ClassResolver.findClassOrFail(className, getClassLoader());
        }
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
}
