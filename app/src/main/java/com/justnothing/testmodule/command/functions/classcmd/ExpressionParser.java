package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.javainterpreter.ScriptRunner;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.AppClassFinder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionParser {
    private static final Logger logger = Logger.getLoggerForName("ExpressionParser");
    
    private static final ThreadLocal<Map<ClassLoader, ScriptRunner>> runners = new ThreadLocal<>();

    public record ParseResult(Object value, Class<?> type, boolean hasTypeHint) {
    }
    
    public static ParseResult parse(String expression, ClassLoader classLoader) {
        return parse(expression, classLoader, null, new ArrayList<>());
    }

    public static ParseResult parse(String expression, ClassLoader classLoader, List<String> imports) {
        return parse(expression, classLoader, null, imports);
    }

    public static ParseResult parse(String expression, ClassLoader classLoader, Class<?> expectedType) {
        return parse(expression, classLoader, expectedType, new ArrayList<>());
    }

    @SuppressWarnings("unused")
    public static ParseResult parse(String expression, ClassLoader classLoader, Class<?> expectedType, List<String> imports) {
        if (expression == null || expression.trim().isEmpty()) {
            return new ParseResult(null, Void.class, false);
        }
        
        expression = expression.trim();
        
        int colonIndex = findTypeHintSeparator(expression);
        
        if (colonIndex > 0) {
            String typeHint = expression.substring(0, colonIndex);
            String valueExpr = expression.substring(colonIndex + 1);
            
            try {
                Object value = evaluateExpression(valueExpr, classLoader, imports);
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
            Object value = evaluateExpression(expression, classLoader, imports);
            Class<?> actualType = value != null ? value.getClass() : Void.class;
            
            if (expectedType != null && value != null) {
                Object convertedValue = tryConvertType(value, expectedType);
                if (convertedValue != value) {
                    logger.debug("类型转换: " + actualType.getName() + " -> " + expectedType.getName());
                    return new ParseResult(convertedValue, expectedType, true);
                }
            }
            
            return new ParseResult(value, actualType, false);
        } catch (Exception e) {
            logger.error("表达式解析失败: " + expression, e);
            throw new IllegalArgumentException("无法解析表达式 '" + expression + "': " + e.getMessage(), e);
        }
    }
    
    private static Object tryConvertType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        Class<?> sourceType = value.getClass();
        
        if (targetType.isAssignableFrom(sourceType)) {
            return value;
        }
        
        if (targetType.isPrimitive()) {
            targetType = wrapPrimitive(targetType);
        }
        if (sourceType.isPrimitive()) {
            sourceType = wrapPrimitive(sourceType);
        }
        
        if (value instanceof Number number) {
            if (targetType == Integer.class || targetType == int.class) {
                return number.intValue();
            } else if (targetType == Long.class || targetType == long.class) {
                return number.longValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return number.doubleValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return number.floatValue();
            } else if (targetType == Short.class || targetType == short.class) {
                return number.shortValue();
            } else if (targetType == Byte.class || targetType == byte.class) {
                return number.byteValue();
            } else if (targetType == BigDecimal.class) {
                if (number instanceof BigDecimal) return number;
                return BigDecimal.valueOf(number.doubleValue());
            } else if (targetType == BigInteger.class) {
                if (number instanceof BigInteger) return number;
                if (number instanceof BigDecimal bd) return bd.toBigInteger();
                return BigInteger.valueOf(number.longValue());
            }
        }
        
        if (value instanceof Boolean bool) {
            if (targetType == Boolean.class || targetType == boolean.class) {
                return bool;
            }
        }
        
        if (value instanceof Character c) {
            if (targetType == Character.class || targetType == char.class) {
                return c;
            } else if (targetType == Integer.class || targetType == int.class) {
                return (int) c;
            } else if (targetType == Long.class || targetType == long.class) {
                return (long) c;
            }
        }
        
        if (value instanceof String str) {
            try {
                if (targetType == String.class) {
                    return str;
                } else if (targetType == Integer.class || targetType == int.class) {
                    return Integer.parseInt(str.trim());
                } else if (targetType == Long.class || targetType == long.class) {
                    return Long.parseLong(str.trim());
                } else if (targetType == Double.class || targetType == double.class) {
                    return Double.parseDouble(str.trim());
                } else if (targetType == Float.class || targetType == float.class) {
                    return Float.parseFloat(str.trim());
                } else if (targetType == Boolean.class || targetType == boolean.class) {
                    return Boolean.parseBoolean(str.trim());
                } else if (targetType == Short.class || targetType == short.class) {
                    return Short.parseShort(str.trim());
                } else if (targetType == Byte.class || targetType == byte.class) {
                    return Byte.parseByte(str.trim());
                } else if (targetType == Character.class || targetType == char.class) {
                    if (str.length() == 1) {
                        return str.charAt(0);
                    }
                }
            } catch (NumberFormatException e) {
                logger.warn("字符串转换失败: '" + str + "' -> " + targetType.getName());
            }
        }
        
        return value;
    }
    
    private static Class<?> wrapPrimitive(Class<?> type) {
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        return type;
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
    
    private static Object evaluateExpression(String expression, ClassLoader classLoader, List<String> imports) {
        ScriptRunner runner = getRunner(classLoader);

        runner.clearVariables();
        runner.clearImports();
        for (String importStmt : imports) {
            runner.addImport(importStmt);
        }
        
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
    
    public static void clearVariables(ClassLoader classLoader) {
        Map<ClassLoader, ScriptRunner> map = runners.get();
        if (map != null) {
            ScriptRunner runner = map.get(classLoader);
            if (runner != null) {
                runner.clearVariables();
            }
        }
    }
    
    public static void clearAllVariables() {
        Map<ClassLoader, ScriptRunner> map = runners.get();
        if (map != null) {
            for (ScriptRunner runner : map.values()) {
                runner.clearVariables();
            }
        }
    }
    
}
