package com.justnothing.testmodule.command.functions.classcmd;

import de.robv.android.xposed.XposedHelpers;
import com.justnothing.testmodule.utils.functions.Logger;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class TypeParser {

    public static class TypeParserLogger extends Logger {
        @Override
        public String getTag() {
            return "TypeParser";
        }
    }

    public static final TypeParserLogger logger = new TypeParserLogger();

    public static Object parse(String typeName, String expression, ClassLoader classLoader) throws Exception {
        if (typeName == null || typeName.trim().isEmpty()) {
            throw new IllegalArgumentException("类型名不能为空");
        }
        if (expression == null) {
            throw new IllegalArgumentException("表达式不能为null");
        }

        typeName = typeName.trim();
        expression = expression.trim();

        logger.debug("解析类型: " + typeName + ", 表达式: " + expression);

        try {
            return switch (typeName.toLowerCase()) {
                case "int", "integer" -> Integer.parseInt(expression);
                case "long" -> {
                    if (expression.endsWith("L") || expression.endsWith("l")) {
                        expression = expression.substring(0, expression.length() - 1);
                    }
                    yield Long.parseLong(expression);
                }
                case "float" -> {
                    if (expression.endsWith("F") || expression.endsWith("f")) {
                        expression = expression.substring(0, expression.length() - 1);
                    }
                    yield Float.parseFloat(expression);
                }
                case "double" -> {
                    if (expression.endsWith("D") || expression.endsWith("d")) {
                        expression = expression.substring(0, expression.length() - 1);
                    }
                    yield Double.parseDouble(expression);
                }
                case "boolean" -> Boolean.parseBoolean(expression);
                case "byte" -> Byte.parseByte(expression);
                case "short" -> Short.parseShort(expression);
                case "char", "character" -> {
                    if (expression.length() == 1) {
                        yield expression.charAt(0);
                    } else if (expression.length() == 3 && expression.startsWith("'") && expression.endsWith("'")) {
                        yield expression.charAt(1);
                    }
                    throw new IllegalArgumentException("Char需要单个字符或字符字面量如'a'");
                }
                case "string" -> {
                    if (expression.startsWith("\"") && expression.endsWith("\"")) {
                        yield expression.substring(1, expression.length() - 1);
                    }
                    yield expression;
                }
                case "null" -> null;
                default ->
                    parseCustomType(typeName, expression, classLoader);
            };
        } catch (NumberFormatException e) {
            logger.error("数值解析失败: 类型=" + typeName + ", 值=" + expression, e);
            throw new IllegalArgumentException("无法将 '" + expression + "' 解析为 " + typeName + " 类型", e);
        } catch (IllegalArgumentException e) {
            logger.error("参数解析失败: " + e.getMessage(), e);
            throw e;
        }
    }

    private static Object parseCustomType(String typeName, String expression, ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException("ClassLoader不能为null");
        }

        logger.debug("解析自定义类型: " + typeName);

        Class<?> targetClass;
        try {
            targetClass = XposedHelpers.findClass(typeName, classLoader);
        } catch (Throwable e) {
            logger.error("找不到类: " + typeName, e);
            throw new IllegalArgumentException("Class not found: " + typeName + ", 原因: " + e.getMessage());
        }

        try {
            if (expression.contains(".") && expression.contains("(") && expression.contains(")")) {
                return parseFactoryMethod(typeName, expression, targetClass, classLoader);
            }

            if (expression.startsWith("new ")) {
                return parseConstructor(typeName, expression.substring(4), targetClass, classLoader);
            }

            if (expression.startsWith("FIELD:")) {
                String fieldName = expression.substring(6);
                try {
                    return XposedHelpers.getStaticObjectField(targetClass, fieldName);
                } catch (Throwable e) {
                    logger.error("获取静态字段失败: " + typeName + "." + fieldName, e);
                    throw new IllegalArgumentException("无法获取静态字段: " + typeName + "." + fieldName);
                }
            }

            try {
                Object[] enumConstants = targetClass.getEnumConstants();
                if (enumConstants != null) {
                    for (Object enumConstant : enumConstants) {
                        if (enumConstant.toString().equals(expression)) {
                            logger.debug("找到枚举值: " + expression);
                            return enumConstant;
                        }
                    }
                }
            } catch (Throwable e) {
                logger.debug("枚举解析失败，尝试其他方式", e);
            }

            try {
                Constructor<?> constructor = targetClass.getConstructor(String.class);
                Object result = constructor.newInstance(expression);
                logger.debug("使用String构造函数创建实例: " + typeName);
                return result;
            } catch (NoSuchMethodException e) {
                logger.error("找不到String构造函数: " + typeName, e);
                throw new IllegalArgumentException("Cannot parse value for type " + typeName +
                        ": " + expression + " (找不到String构造函数)");
            } catch (Exception e) {
                logger.error("使用String构造函数创建实例失败: " + typeName, e);
                throw new IllegalArgumentException("无法使用String构造函数创建 " + typeName + " 实例: " + e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("解析自定义类型失败: " + typeName, e);
            throw new IllegalArgumentException("解析 " + typeName + " 失败: " + e.getMessage(), e);
        }
    }

    private static Object parseFactoryMethod(String typeName, String expression,
                                             Class<?> targetClass, ClassLoader classLoader) throws Exception {
        logger.debug("解析工厂方法: " + expression);

        int dotIndex = expression.indexOf('.');
        int parenIndex = expression.indexOf('(');
        int endParenIndex = expression.lastIndexOf(')');

        if (dotIndex < 0 || parenIndex < 0 || endParenIndex < parenIndex) {
            throw new IllegalArgumentException("Invalid factory method expression: " + expression);
        }

        String methodName = expression.substring(dotIndex + 1, parenIndex);
        String paramStr = expression.substring(parenIndex + 1, endParenIndex);

        List<Object> params = new ArrayList<>();
        List<Class<?>> paramTypes = new ArrayList<>();

        if (!paramStr.trim().isEmpty()) {
            String[] paramStrings = splitParams(paramStr);
            for (String param : paramStrings) {
                param = param.trim();
                if (param.isEmpty()) continue;

                try {
                    if (param.contains(":")) {
                        int colonIdx = param.indexOf(':');
                        String pType = param.substring(0, colonIdx);
                        String pValue = param.substring(colonIdx + 1);
                        Object parsed = parse(pType, pValue, classLoader);
                        params.add(parsed);
                        assert parsed != null;
                        paramTypes.add(parsed.getClass());
                    } else {
                        Object parsed = inferType(param);
                        params.add(parsed);
                        paramTypes.add(parsed.getClass());
                    }
                } catch (Exception e) {
                    logger.error("解析参数失败: " + param, e);
                    throw new IllegalArgumentException("无法解析参数 '" + param + "': " + e.getMessage(), e);
                }
            }
        }

        Method method = findStaticMethod(targetClass, methodName,
                paramTypes.toArray(new Class<?>[0]));

        if (method == null) {
            logger.error("找不到静态方法: " + typeName + "." + methodName);
            throw new NoSuchMethodException("Static method not found: " +
                    methodName + " in " + typeName);
        }

        try {
            Object result = method.invoke(null, params.toArray());
            logger.debug("工厂方法调用成功: " + typeName + "." + methodName);
            return result;
        } catch (Exception e) {
            logger.error("工厂方法调用失败: " + typeName + "." + methodName, e);
            throw e;
        }
    }

    private static Object parseConstructor(String typeName, String expression,
                                           Class<?> targetClass, ClassLoader classLoader) throws Exception {
        // 解析构造函数参数
        if (!expression.startsWith("(") || !expression.endsWith(")")) {
            throw new IllegalArgumentException("Constructor should be like: new Type(param1, param2)");
        }

        String paramStr = expression.substring(1, expression.length() - 1);
        List<Object> params = new ArrayList<>();
        List<Class<?>> paramTypes = new ArrayList<>();

        if (!paramStr.trim().isEmpty()) {
            String[] paramStrings = splitParams(paramStr);
            for (String param : paramStrings) {
                param = param.trim();
                if (param.isEmpty()) continue;

                if (param.contains(":")) {
                    int colonIdx = param.indexOf(':');
                    String pType = param.substring(0, colonIdx);
                    String pValue = param.substring(colonIdx + 1);
                    Object parsed = parse(pType, pValue, classLoader);
                    params.add(parsed);
                    assert parsed != null;
                    paramTypes.add(parsed.getClass());
                } else {
                    Object parsed = inferType(param);
                    params.add(parsed);
                    paramTypes.add(parsed.getClass());
                }
            }
        }

        // 查找构造函数
        Constructor<?> constructor = findConstructor(targetClass,
                paramTypes.toArray(new Class<?>[0]));

        if (constructor == null) {
            throw new NoSuchMethodException("Constructor not found for " + typeName +
                    " with given parameters");
        }

        return constructor.newInstance(params.toArray());
    }

    private static Method findStaticMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) &&
                    Modifier.isStatic(method.getModifiers()) &&
                    method.getParameterCount() == paramTypes.length) {

                Class<?>[] methodParams = method.getParameterTypes();
                boolean compatible = true;

                for (int i = 0; i < paramTypes.length; i++) {
                    if (!methodParams[i].isAssignableFrom(paramTypes[i])) {
                        compatible = false;
                        break;
                    }
                }

                if (compatible) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

    private static Constructor<?> findConstructor(Class<?> clazz, Class<?>[] paramTypes) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == paramTypes.length) {
                Class<?>[] constructorParams = constructor.getParameterTypes();
                boolean compatible = true;

                for (int i = 0; i < paramTypes.length; i++) {
                    if (!constructorParams[i].isAssignableFrom(paramTypes[i])) {
                        compatible = false;
                        break;
                    }
                }

                if (compatible) {
                    constructor.setAccessible(true);
                    return constructor;
                }
            }
        }
        return null;
    }

    private static String[] splitParams(String paramStr) {
        List<String> params = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inQuotes = false;

        for (char c : paramStr.toCharArray()) {
            if (c == '"' && (current.length() == 0 || current.charAt(current.length() - 1) != '\\')) {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == '(' && !inQuotes) {
                parenDepth++;
                current.append(c);
            } else if (c == ')' && !inQuotes) {
                parenDepth--;
                current.append(c);
            } else if (c == ',' && parenDepth == 0 && !inQuotes) {
                params.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // 使用 length() > 0 替代 isEmpty()
        if (current.length() > 0) {
            params.add(current.toString());
        }

        return params.toArray(new String[0]);
    }

    private static Object inferType(String value) {
        value = value.trim();

        try {
            // 尝试整数
            if (!value.contains(".") && !value.toLowerCase().contains("e")) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    try {
                        return Long.parseLong(value.replace("L", "").replace("l", ""));
                    } catch (NumberFormatException e2) {
                        // 继续尝试
                    }
                }
            }

            // 尝试浮点数
            try {
                if (value.endsWith("F") || value.endsWith("f")) {
                    return Float.parseFloat(value.substring(0, value.length() - 1));
                }
                if (value.endsWith("D") || value.endsWith("d")) {
                    return Double.parseDouble(value.substring(0, value.length() - 1));
                }
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // 继续尝试
            }

            // 布尔值
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(value);
            }

            // 字符串（带引号或不带）
            if (value.startsWith("\"") && value.endsWith("\"")) {
                return value.substring(1, value.length() - 1);
            }

            // 字符
            if (value.startsWith("'") && value.endsWith("'") && value.length() == 3) {
                return value.charAt(1);
            }

            // 默认作为字符串
            return value;

        } catch (Exception e) {
            return value; // 回退到字符串
        }
    }
}