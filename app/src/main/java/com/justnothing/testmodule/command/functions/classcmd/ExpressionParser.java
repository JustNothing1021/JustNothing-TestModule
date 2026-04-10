package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.javainterpreter.ScriptRunner;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.reflect.AppClassFinder;

import java.util.HashMap;
import java.util.Map;

public class ExpressionParser {
    private static final Logger logger = Logger.getLoggerForName("ExpressionParser");
    
    private static final ThreadLocal<Map<ClassLoader, ScriptRunner>> runners = new ThreadLocal<>();

    public record ParseResult(Object value, Class<?> type, boolean hasTypeHint) {
    }
    
    public static ParseResult parse(String expression, ClassLoader classLoader) {
        return parse(expression, classLoader, null);
    }

    @SuppressWarnings("unused")
    public static ParseResult parse(String expression, ClassLoader classLoader, Class<?> expectedType) {
        if (expression == null || expression.trim().isEmpty()) {
            return new ParseResult(null, Void.class, false);
        }
        
        expression = expression.trim();
        
        int colonIndex = findTypeHintSeparator(expression);
        
        if (colonIndex > 0) {
            String typeHint = expression.substring(0, colonIndex);
            String valueExpr = expression.substring(colonIndex + 1);
            
            try {
                Object value = evaluateExpression(valueExpr, classLoader);
                Class<?> actualType = value != null ? value.getClass() : Void.class;
                
                if (!isTypeCompatible(typeHint, actualType, classLoader)) {
                    logger.warn("类型提示不匹配: 期望 " + typeHint + ", 实际 " + actualType.getName());
                }
                
                return new ParseResult(value, actualType, true);
            } catch (Exception e) {
                logger.error("表达式解析失败: " + valueExpr, e);
                throw new IllegalArgumentException("无法解析表达式 '" + valueExpr + "': " + e.getMessage(), e);
            }
        }
        
        try {
            Object value = evaluateExpression(expression, classLoader);
            Class<?> type = value != null ? value.getClass() : Void.class;
            return new ParseResult(value, type, false);
        } catch (Exception e) {
            logger.error("表达式解析失败: " + expression, e);
            throw new IllegalArgumentException("无法解析表达式 '" + expression + "': " + e.getMessage(), e);
        }
    }
    
    private static int findTypeHintSeparator(String expression) {
        int colonIndex = expression.indexOf(':');
        
        if (colonIndex <= 0) {
            return -1;
        }
        
        String potentialType = expression.substring(0, colonIndex);
        
        if (potentialType.contains("\"") || potentialType.contains("'") ||
            potentialType.contains(" ") || potentialType.contains("(") ||
            potentialType.contains(".") || potentialType.contains("+") ||
            potentialType.contains("-") || potentialType.contains("*") ||
            potentialType.contains("/")) {
            return -1;
        }
        
        if (Character.isDigit(potentialType.charAt(0))) {
            return -1;
        }
        
        for (char c : potentialType.toCharArray()) {
            if (!Character.isJavaIdentifierPart(c) && c != '.' && c != '[' && c != ']') {
                return -1;
            }
        }
        
        return colonIndex;
    }
    
    private static boolean isTypeCompatible(String typeHint, Class<?> actualType, ClassLoader classLoader) {
        if (actualType == Void.class) {
            return typeHint.equalsIgnoreCase("null") || typeHint.equalsIgnoreCase("void");
        }
        
        String normalizedHint = typeHint.toLowerCase();
        
        return switch (normalizedHint) {
            case "int", "integer" -> actualType == Integer.class || actualType == int.class;
            case "long" -> actualType == Long.class || actualType == long.class;
            case "float" -> actualType == Float.class || actualType == float.class;
            case "double" -> actualType == Double.class || actualType == double.class;
            case "boolean" -> actualType == Boolean.class || actualType == boolean.class;
            case "byte" -> actualType == Byte.class || actualType == byte.class;
            case "short" -> actualType == Short.class || actualType == short.class;
            case "char", "character" -> actualType == Character.class || actualType == char.class;
            case "string" -> actualType == String.class;
            default -> {
                try {
                    Class<?> hintClass = Class.forName(typeHint, false, classLoader);
                    yield hintClass.isAssignableFrom(actualType);
                } catch (ClassNotFoundException e) {
                    yield true;
                }
            }
        };
    }
    
    private static Object evaluateExpression(String expression, ClassLoader classLoader) {
        ScriptRunner runner = getRunner(classLoader);
        
        String code = expression.trim();
        if (!code.endsWith(";") && !code.endsWith("}")) {
            code = code + ";";
        }
        
        try {
            return runner.executeWithResult(code);
        } catch (Exception e) {
            throw new IllegalArgumentException("表达式求值失败: " + e.getMessage(), e);
        }
    }
    
    private static ScriptRunner getRunner(ClassLoader classLoader) {
        Map<ClassLoader, ScriptRunner> map = runners.get();
        if (map == null) {
            map = new HashMap<>();
            runners.set(map);
        }
        
        ScriptRunner runner = map.get(classLoader);
        if (runner == null) {
            runner = new ScriptRunner(classLoader);
            runner.setClassFinder(new AppClassFinder());
            map.put(classLoader, runner);
        }
        
        return runner;
    }
    
}
