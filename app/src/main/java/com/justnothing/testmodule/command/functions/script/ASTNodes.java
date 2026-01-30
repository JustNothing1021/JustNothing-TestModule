package com.justnothing.testmodule.command.functions.script;

import com.justnothing.testmodule.command.functions.script.ScriptModels.*;
import static com.justnothing.testmodule.command.functions.script.ScriptLogger.*;
import static com.justnothing.testmodule.command.functions.script.ScriptUtils.*;

import androidx.annotation.NonNull;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ASTNodes {
    
    public static abstract class ASTNode {
        /**
         * 评估这个节点。
         *
         * @param context 执行上下文
         * @return 评估结果
         * @throws Exception 如果评估出错
         */
        public abstract Object evaluate(ExecutionContext context) throws Exception;

        public abstract Class<?> getType(ExecutionContext context) throws Exception;
    }

    public static class LiteralNode extends ASTNode {
        final Object value;
        private final Class<?> type;

        public LiteralNode(Object value) {
            this.value = value;
            this.type = value != null ? value.getClass() : Void.class;
        }

        public LiteralNode(Object value, Class<?> type) {
            this.value = value;
            this.type = type;
        }

        @Override
        public Object evaluate(ExecutionContext context) {
            return value;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return type;
        }
    }

    public static class ImportNode extends ASTNode {
        private final String packageName;

        public ImportNode(String packageName) {
            this.packageName = packageName;
        }

        @Override
        public Object evaluate(ExecutionContext context) {
            try {
                context.addImport(packageName);
            } catch (ClassNotFoundException e) {
                logger.warn("导入包失败: " + packageName + ": " + e.getMessage());
            }
            return null;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class ArrayNode extends ASTNode {

        private final List<ASTNode> elements;

        public ArrayNode(List<ASTNode> elements) {
            this.elements = elements;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws RuntimeException {
            try {
                ArrayList<Object> values = new ArrayList<>();
                Class<?> type = getElementType(context);
                for (ASTNode element : elements) {
                    Object value = element.evaluate(context);
                    values.add(value);
                    if (value != null && !type.isAssignableFrom(value.getClass())) {
                        logger.warn("数组元素类型不一致: " + type + " != " + value.getClass() + ", 尝试解析为Object[]");
                        type = Object.class;
                    }
                }
                if (values.isEmpty())
                    return new Object[0];
                Object array = Array.newInstance(type, values.size());
                for (int i = 0; i < values.size(); i++)
                    Array.set(array, i, values.get(i));
                return array;
            } catch (Exception e) {
                logger.error("解析数组时出现错误: " + e);
                throw new RuntimeException("Error parsing array: " + e);
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            if (elements.isEmpty())
                return Object[].class;
            return Array.newInstance(elements.get(0).getType(context), 0).getClass();
        }

        public Class<?> getElementType(ExecutionContext context) throws Exception {
            if (elements.isEmpty())
                return Object.class;
            return elements.get(0).getType(context);
        }
    }

    public static class MapNode extends ASTNode {

        private final Map<String, ASTNode> entries;
        private final Class<?> type;

        public MapNode(Map<String, ASTNode> entries, Class<?> type) {
            this.entries = entries;
            this.type = type;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws RuntimeException {
            try {
                Map<String, Object> map = new HashMap<>();
                for (Map.Entry<String, ASTNode> entry : entries.entrySet()) {
                    map.put(entry.getKey(), entry.getValue().evaluate(context));
                }
                return map;
            } catch (Exception e) {
                logger.error("解析Map时出现错误: " + e);
                throw new RuntimeException("Error parsing map: " + e);
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return type;
        }
    }

    public static class BinaryOperatorNode extends ASTNode {
        private final String operator;
        private final ASTNode left;
        private final ASTNode right;

        public BinaryOperatorNode(String operator, ASTNode left, ASTNode right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object leftVal = left.evaluate(context);
            Object rightVal = null;
            if (right != null)
                rightVal = right.evaluate(context);

            return switch (operator) {
                case "+" -> add(leftVal, rightVal);
                case "-" -> subtract(leftVal, rightVal);
                case "*" -> multiply(leftVal, rightVal);
                case "/" -> divide(leftVal, rightVal);
                case "%" -> modulo(leftVal, rightVal);
                case "==" -> equals(leftVal, rightVal);
                case "!=" -> !equals(leftVal, rightVal);
                case "<" -> compare(leftVal, rightVal) < 0;
                case ">" -> compare(leftVal, rightVal) > 0;
                case "<=" -> compare(leftVal, rightVal) <= 0;
                case ">=" -> compare(leftVal, rightVal) >= 0;
                case "&&" -> logicAnd(leftVal, rightVal);
                case "||" -> logicOr(leftVal, rightVal);
                case "^" -> xor(leftVal, rightVal);
                case ">>" -> rightShift(leftVal, rightVal);
                case "<<" -> leftShift(leftVal, rightVal);
                case "&" -> bitAnd(leftVal, rightVal);
                case "|" -> bitOr(leftVal, rightVal);
                case "!" -> logicNot(leftVal);
                case "~" -> bitRev(leftVal);
                case ">>>" -> unsignedRightShift(leftVal, rightVal);
                case "instanceof" -> leftVal != null && rightVal != null
                        && leftVal.getClass().isAssignableFrom(rightVal.getClass());
                // 不处理= += -=之类的运算符，因为它们归Assignment管
                default -> {
                    logger.error("未知的运算符: " + operator);
                    throw new RuntimeException("Unknown operator: " + operator);
                }
            };
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return switch (operator) {
                case "+", "-", "*", "/", "%", ">>", "<<", "~" -> Number.class;
                case "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!" -> Boolean.class;
                default -> Object.class;
            };
        }

        private Object add(Object a, Object b) {
            if (a != null && a instanceof String) { // 特例，String可以加null
                return a + String.valueOf(b);
            } else if (b != null && b instanceof String) {
                return String.valueOf(a) + b;
            } else if (a == null || b == null) {
                logger.error("无法在null值上执行运算: a = " + a + ", b = " + b);
                throw new RuntimeException("Cannot apply operations on null values");
            }
            if (a instanceof Number num1 && b instanceof Number num2) {
                if (a instanceof Double || b instanceof Double) {
                    return num1.doubleValue() + num2.doubleValue();
                }
                if (a instanceof Float || b instanceof Float) {
                    return num1.floatValue() + num2.floatValue();
                }
                if (a instanceof Long || b instanceof Long) {
                    return num1.longValue() + num2.longValue();
                }
                return num1.intValue() + num2.intValue();
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行加法运算");
            throw new RuntimeException("Cannot add: " + a.getClass() + " and " + b.getClass());
        }

        private Object subtract(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply operations on null values");
            }
            if (a instanceof Number num1 && b instanceof Number num2) {
                if (a instanceof Double || b instanceof Double) {
                    return num1.doubleValue() - num2.doubleValue();
                }
                if (a instanceof Float || b instanceof Float) {
                    return num1.floatValue() - num2.floatValue();
                }
                if (a instanceof Long || b instanceof Long) {
                    return num1.longValue() - num2.longValue();
                }
                return num1.intValue() - num2.intValue();
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行减法运算");
            throw new RuntimeException("Cannot subtract: " + a.getClass() + " and " + b.getClass());
        }

        private Object multiply(Object a, Object b) {
            if (a == null || b == null) {
                String aStr = (a == null) ? "null" : a.toString();
                String bStr = (b == null) ? "null" : b.toString();
                logger.error("无法在null值上执行运算: a = " + aStr + ", b = " + bStr);
                throw new RuntimeException("Cannot apply operations on null values");
            }
            if (a instanceof Number num1 && b instanceof Number num2) {
                if (a instanceof Double || b instanceof Double) {
                    return num1.doubleValue() * num2.doubleValue();
                }
                if (a instanceof Float || b instanceof Float) {
                    return num1.floatValue() * num2.floatValue();
                }
                if (a instanceof Long || b instanceof Long) {
                    return num1.longValue() * num2.longValue();
                }
                return num1.intValue() * num2.intValue();
            }
            if (a instanceof String && b instanceof Number) {
                return repeat((String) a, (Integer) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行乘法运算");
            throw new RuntimeException("Cannot multiply: " + a.getClass() + " and " + b.getClass());
        }

        private Object divide(Object a, Object b) {
            if (a == null || b == null) {
                String aStr = (a == null) ? "null" : a.toString();
                String bStr = (b == null) ? "null" : b.toString();
                logger.error("无法在null值上执行运算: a = " + aStr + ", b = " + bStr);
                throw new RuntimeException("Cannot apply operations on null values");
            }
            if (a instanceof Number num1 && b instanceof Number num2) {
                if (a instanceof Double || b instanceof Double) {
                    return num1.doubleValue() / num2.doubleValue();
                }
                if (a instanceof Float || b instanceof Float) {
                    return num1.floatValue() / num2.floatValue();
                }
                if (a instanceof Long || b instanceof Long) {
                    return num1.longValue() / num2.longValue();
                }
                return num1.intValue() / num2.intValue();
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行除法运算");
            throw new RuntimeException("Cannot divide: " + a.getClass() + " and " + b.getClass());
        }

        private Object modulo(Object a, Object b) {
            if (a == null || b == null) {
                String aStr = (a == null) ? "null" : a.toString();
                String bStr = (b == null) ? "null" : b.toString();
                logger.error("无法在null值上执行运算: a = " + aStr + ", b = " + bStr);
                throw new RuntimeException("Cannot apply operations on null values");
            }
            if (a instanceof Number num1 && b instanceof Number num2) {
                if (a instanceof Double || b instanceof Double) {
                    return num1.doubleValue() % num2.doubleValue();
                }
                if (a instanceof Float || b instanceof Float) {
                    return num1.floatValue() % num2.floatValue();
                }
                if (a instanceof Long || b instanceof Long) {
                    return num1.longValue() % num2.longValue();
                }
                return num1.intValue() % num2.intValue();
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行模运算");
            throw new RuntimeException("Cannot modulo: " + a.getClass() + " and " + b.getClass());
        }

        private boolean equals(Object a, Object b) {
            if (a == null && b == null)
                return true;
            if (a == null || b == null)
                return false;
            return a.equals(b);
        }

        private int compare(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行比较: a = " + a);

                throw new RuntimeException("Cannot apply comparison on null values");
            }
            if (a instanceof Comparable && b instanceof Comparable) {
                @SuppressWarnings("unchecked")
                Comparable<Object> compA = (Comparable<Object>) a;
                return compA.compareTo(b);
            }
            logger.error("无法比较" + a.getClass() + "与" + b.getClass());
            throw new RuntimeException("Cannot compare: " + a.getClass() + " and " + b.getClass());
        }

        private boolean logicAnd(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            return toBoolean(a) && toBoolean(b);
        }

        private boolean logicOr(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            return toBoolean(a) || toBoolean(b);
        }

        private boolean logicXor(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            return toBoolean(a) ^ toBoolean(b);
        }

        private boolean logicNot(Object a) {
            if (a == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + null);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            return !toBoolean(a);
        }

        private Object xor(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            if (a instanceof Boolean && b instanceof Boolean) {
                return logicXor(a, b);
            } else {
                return bitXor(a, b);
            }
        }

        private Object bitAnd(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) & ((Integer) b);
            } else if (a instanceof Long && b instanceof Long) {
                return ((Long) a) & ((Long) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行按位与操作");
            throw new RuntimeException("Cannot bitwise and: " + a.getClass() + " and " + b.getClass());
        }

        private Object bitOr(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) | ((Integer) b);
            } else if (a instanceof Long && b instanceof Long) {
                return ((Long) a) | ((Long) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行按位或操作");
            throw new RuntimeException("Cannot bitwise or: " + a.getClass() + " and " + b.getClass());
        }

        private Object bitXor(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) ^ ((Integer) b);
            } else if (a instanceof Long && b instanceof Long) {
                return ((Long) a) ^ ((Long) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行按位异或操作");
            throw new RuntimeException("Cannot bitwise xor: " + a.getClass() + " and " + b.getClass());
        }

        private Object bitRev(Object a) {
            if (a == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + null);

                throw new RuntimeException("Cannot apply bitwise operations on null values");
            }
            if (a instanceof Integer) {
                return ~((Integer) a);
            } else if (a instanceof Long) {
                return ~((Long) a);
            }
            logger.error("无法在" + a.getClass() + "上进行按位取反操作");
            throw new RuntimeException("Cannot bitwise invert: " + a.getClass());
        }

        private Object rightShift(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行位运算");
                throw new RuntimeException("Cannot apply bitwise operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) >> ((Integer) b);
            } else if (a instanceof Long && b instanceof Integer) {
                return ((Long) a) >> ((Integer) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行右移操作");
            throw new RuntimeException("Cannot right shift: " + a.getClass() + " and " + b.getClass());
        }

        private Object leftShift(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行位运算");
                throw new RuntimeException("Cannot apply bitwise operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) << ((Integer) b);
            } else if (a instanceof Long && b instanceof Integer) {
                return ((Long) a) << ((Integer) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行左移操作");
            throw new RuntimeException("Cannot left shift: " + a.getClass() + " and " + b.getClass());
        }

        private Object unsignedRightShift(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行位运算");
                throw new RuntimeException("Cannot apply bitwise operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) >>> ((Integer) b);
            } else if (a instanceof Long && b instanceof Integer) {
                return ((Long) a) >>> ((Integer) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行无符号右移操作");
            throw new RuntimeException("Cannot unsigned right shift: " + a.getClass() + " and " + b.getClass());
        }

    }

    public static class ClassReferenceNode extends ASTNode {
        private final String className;

        public ClassReferenceNode(String className) {
            this.className = className;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            return getClass(context);
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Class.class;
        }

        public Class<?> getClass(ExecutionContext context) throws ClassNotFoundException {
            return context.findClass(className);
        }

        public String getClassName() {
            return className;
        }
    }

    public static class VariableNode extends ASTNode {
        final String name;

        public VariableNode(String name) {
            this.name = name;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Variable var;
            if (context.hasClass(name))
                return context.findClass(name);
            try {
                var = context.getVariable(name);
                return var.value;
            } catch (RuntimeException ignored) {
            }

            if (context.hasVariable("this")) {
                Object thisValue = context.getVariable("this").value;
                if (thisValue instanceof CustomClassInstance instance) {
                    FieldDefinition fieldDef = instance.getClassDefinition().getField(name);
                    if (fieldDef != null) {
                        return instance.getField(name);
                    }
                }
            }

            // 检查是否是 BuiltInFunction
            if (context.hasBuiltIn(name)) {
                return context.getBuiltIn(name);
            }

            logger.error("未定义变量或类" + name);
            throw new RuntimeException("Undefined variable: " + name);
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            if (context.hasClass(name))
                return Class.class;

            if (context.hasVariable(name)) {
                Variable var = context.getVariable(name);
                return var.type;
            }

            if (context.hasBuiltIn(name)) {
                BuiltInFunction builtIn = context.getBuiltIn(name);
                if (builtIn != null) {
                    return Object.class;
                }
            }

            return Object.class;
        }
    }

    public static class MethodCallNode extends ASTNode {

        private final ASTNode target;
        private final String methodName;
        private final List<ASTNode> arguments;

        public MethodCallNode(ASTNode target, String methodName, List<ASTNode> arguments) {
            this.target = target;
            this.methodName = methodName;
            this.arguments = arguments;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if (target instanceof VariableNode && context.hasBuiltIn(((VariableNode) target).name)) {
                BuiltInFunction builtIn = context.getBuiltIn(((VariableNode) target).name);
                if (builtIn != null) {
                    List<Object> args = new ArrayList<>();
                    for (ASTNode arg : arguments) {
                        Object argValue = arg.evaluate(context);
                        args.add(argValue);
                    }
                    return builtIn.call(args);
                }
            }

            List<Object> argsList = new ArrayList<>();
            List<Class<?>> argTypes = new ArrayList<>();
            for (int i = 0; i < arguments.size(); i++) {
                ASTNode arg = arguments.get(i);
                Object argValue = arg.evaluate(context);
                argsList.add(argValue);
                argTypes.add(argValue != null ? argValue.getClass() : Void.class);
            }

            if (target != null) {
                Object targetObj = target.evaluate(context);

                if (targetObj instanceof Lambda) {
                    return ((Lambda) targetObj).apply(argsList.toArray());
                }

                if (targetObj instanceof CustomClassInstance customInstance) {
                    ClassDefinition classDef = customInstance.getClassDefinition();
                    MethodDefinition methodDef = findMethodInHierarchy(classDef, methodName, context, argTypes);
                    if (methodDef != null) {
                        // 保存当前的控制流状态
                        boolean originalShouldReturn = context.shouldReturn;
                        boolean originalShouldBreak = context.shouldBreak;
                        boolean originalShouldContinue = context.shouldContinue;

                        // 保存当前上下文状态
                        context.enterScope();

                        // 设置this引用
                        context.setVariable("this", customInstance);

                        // 设置方法参数
                        List<Parameter> parameters = methodDef.getParameters();
                        for (int i = 0; i < parameters.size(); i++) {
                            Parameter param = parameters.get(i);
                            Object argValue = argsList.get(i);
                            context.setVariable(param.getName(), argValue);
                        }

                        // 执行方法体
                        Object result = null;
                        if (methodDef.getBody() != null) {
                            // 保存控制流状态的原始值
                            boolean savedShouldReturn = context.shouldReturn;
                            boolean savedShouldBreak = context.shouldBreak;
                            boolean savedShouldContinue = context.shouldContinue;
                            Object savedReturnValue = context.returnValue;
                            String savedMethodReturnType = context.getCurrentMethodReturnType();

                            // 设置当前方法的返回类型
                            context.setCurrentMethodReturnType(methodDef.getReturnType());

                            // 重置控制流状态
                            context.shouldReturn = false;
                            context.shouldBreak = false;
                            context.shouldContinue = false;

                            // 执行方法体
                            result = methodDef.getBody().evaluate(context);

                            // 检查方法体是否设置了return
                            if (context.shouldReturn) {
                                // 获取返回值（从方法体执行结果中获取）
                                Object returnValue = context.returnValue;

                                // 恢复外层的控制流状态
                                context.shouldReturn = savedShouldReturn;
                                context.shouldBreak = savedShouldBreak;
                                context.shouldContinue = savedShouldContinue;
                                context.returnValue = savedReturnValue;
                                context.setCurrentMethodReturnType(savedMethodReturnType);

                                return returnValue;
                            }

                            // 恢复外层的控制流状态
                            context.shouldReturn = savedShouldReturn;
                            context.shouldBreak = savedShouldBreak;
                            context.shouldContinue = savedShouldContinue;
                            context.returnValue = savedReturnValue;
                            context.setCurrentMethodReturnType(savedMethodReturnType);

                            return result;
                        }

                        // 如果没有方法体，返回null
                        return null;
                    } else {
                        throw new RuntimeException(
                                "Method '" + methodName + "' not found in class hierarchy " + classDef.getClassName());
                    }
                }
                if (target instanceof ClassReferenceNode) {
                    ClassReferenceNode classRef = (ClassReferenceNode) target;
                    String className = classRef.className;
                    return context.callStaticMethod(className, methodName, argsList);
                }
                // else if (targetObj instanceof Class) {
                // Class<?> classDef = (Class<?>) targetObj;
                // return context.callStaticMethod(classDef, methodName, argsList);
                // }
                // 这个还是算了，不然有二义性

                return context.callMethod(targetObj, methodName, argsList);
            }

            logger.error("尝试在null上调用方法" + methodName);
            throw new RuntimeException("Method call on null object");
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            if (target instanceof VariableNode && context.hasBuiltIn(((VariableNode) target).name)) {
                return Object.class;
            }

            Object targetObj = target != null ? target.evaluate(context) : null;
            Class<?> targetClass;

            if (targetObj instanceof Class) {
                targetClass = Class.class;
            } else if (targetObj != null) {
                targetClass = targetObj.getClass();
            } else if (target instanceof ClassReferenceNode) {
                targetClass = ((ClassReferenceNode) target).getClass(context);
            } else {
                logger.error("无法确定方法调用的目标: " + methodName);
                throw new RuntimeException("Cannot determine target for method call: " + methodName);
            }

            Class<?>[] argTypes = new Class<?>[arguments.size()];
            for (int i = 0; i < arguments.size(); i++) {
                argTypes[i] = arguments.get(i).getType(context);
            }

            try {
                Method method = context.findMethod(targetClass, methodName, Arrays.asList(argTypes));
                return method.getReturnType();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Method not found: " + methodName + " on " + targetClass.getName());
            }
        }
    }


    public static class ConstructorCallNode extends ASTNode {
        private final String className;
        private final List<ASTNode> arguments;
        private final ASTNode arrInitial;

        public ConstructorCallNode(String className, List<ASTNode> arguments, ASTNode arrInitial) {
            this.className = className;
            this.arguments = arguments;
            this.arrInitial = arrInitial;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {

            if (className.contains("[") || className.contains("]")) {
                if (!className.contains("[") || !className.contains("]")) {
                    logger.error("数组类型的类名中缺少方括号");
                    throw new RuntimeException("Array constructor class name missing brackets");
                }
                String elementType = className.substring(0, className.indexOf("["));
                List<Integer> dimensions = new ArrayList<>();
                String arrayDim = className.substring(className.indexOf("["));
                int idx;
                while ((idx = arrayDim.indexOf("[")) != -1) {
                    int rightBracket = arrayDim.indexOf("]");
                    if (rightBracket == -1) {
                        logger.error("数组类型的类名中缺少右方括号");
                        throw new RuntimeException("Array constructor class name missing right bracket");
                    }
                    String thisDim = arrayDim.substring(idx + 1, rightBracket);
                    dimensions.add(thisDim.isEmpty() ? -1 : Integer.parseInt(thisDim));
                    arrayDim = arrayDim.substring(rightBracket + 1);
                }
                return createArray(context, elementType, dimensions, arrInitial);
            } else {
                Class<?> clazz;
                try {
                    clazz = context.findClass(className);
                } catch (CustomClassException e) {
                    // 这是自定义类，创建自定义类的实例
                    ClassDefinition classDef = context.customClasses.get(className);
                    if (classDef == null) {
                        throw new RuntimeException("Custom class not found: " + className);
                    }

                    // 先创建实例（用于字段初始化）
                    CustomClassInstance instance = new CustomClassInstance(classDef, context);

                    List<ConstructorDefinition> constructors = classDef.getConstructors();
                    if (!constructors.isEmpty()) {
                        Object[] args = new Object[arguments.size()];
                        Class<?>[] argTypes = new Class<?>[arguments.size()];
                        for (int i = 0; i < arguments.size(); i++) {
                            args[i] = arguments.get(i).evaluate(context);
                            argTypes[i] = arguments.get(i).getType(context);
                        }

                        ConstructorDefinition constructorDef = classDef.getConstructor(Arrays.asList(argTypes),
                                context);
                        if (constructorDef != null) {
                            ExecutionContext constructorContext = new ExecutionContext(context.getClassLoader());

                            constructorContext.customClasses.putAll(context.customClasses);

                            constructorContext.setVariable("this", instance);

                            List<Parameter> params = constructorDef.getParameters();
                            for (int i = 0; i < params.size() && i < args.length; i++) {
                                constructorContext.setVariable(params.get(i).getName(), args[i]);
                            }

                            if (constructorDef.getBody() != null) {
                                constructorContext.setCurrentMethodReturnType("void");
                                constructorDef.getBody().evaluate(constructorContext);

                                String constructorOutput = constructorContext.getOutput();
                                if (!constructorOutput.isEmpty()) {
                                    context.print(constructorOutput);
                                }
                            }
                        }
                    }
                    return instance;
                }
                // 让Java自己判断吧
                // if ((clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) && !clazz.isPrimitive()) {
                //     logger.error("尝试实例化一个抽象类或接口" + className);
                //     throw new InstantiationException("Cannot instantiate abstract class or interface: " + className);
                // }
                Object[] args = new Object[arguments.size()];
                Class<?>[] argTypes = new Class<?>[arguments.size()];

                for (int i = 0; i < arguments.size(); i++) {
                    args[i] = arguments.get(i).evaluate(context);
                    argTypes[i] = arguments.get(i).getType(context);
                }

                Constructor<?> constructor = findConstructor(clazz, argTypes);
                return constructor.newInstance(args);
            }

        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            try {
                return context.findClass(className);
            } catch (CustomClassException e) {
                // 对于自定义类，返回Object.class作为类型
                return Object.class;
            }
        }

        private Constructor<?> findConstructor(Class<?> clazz, Class<?>[] argTypes)
                throws NoSuchMethodException {

            try {
                return clazz.getDeclaredConstructor(argTypes);
            } catch (NoSuchMethodException ignored) {
            }

            Constructor<?> bestMatch = null;
            StringBuilder sb = new StringBuilder();
            for (Constructor<?> item : clazz.getConstructors()) {
                if (isApplicableArgs(item.getParameterTypes(), Arrays.asList(argTypes), item.isVarArgs())) {
                    bestMatch = item;
                }
                sb.append(Arrays.toString(item.getParameterTypes())).append("\n");
            }

            if (bestMatch != null) {
                return bestMatch;
            }

            if (argTypes.length == 0) {
                try {
                    return clazz.getDeclaredConstructor();
                } catch (NoSuchMethodException ignored) {
                }
            }
            logger.warn("无法找到与目标参数类型匹配的构造函数: ");
            logger.warn(" -> 访问的类型为 " + clazz.getName());
            logger.warn(" -> 要求的参数类型为 " + Arrays.toString(argTypes));

            throw new NoSuchMethodException("Constructor not found for class: " + clazz.getName() +
                    " with " + argTypes.length + " arguments\n" + "\nAvailable constructors:\n" +
                    sb + "\nProvided:\n" + Arrays.toString(argTypes) + "\n");
        }

        private Object createArray(ExecutionContext context, String elementType,
                List<Integer> dimensions, ASTNode arrInitial) throws Exception {
            Class<?> elementClass = context.findClass(elementType);
            // if (elementClass.isInterface() || Modifier.isAbstract(elementClass.getModifiers()) && !elementClass.isPrimitive()) {
            //     logger.error("尝试实例化一个抽象类或接口" + className);
            //     throw new InstantiationException("Cannot instantiate abstract class or interface: " + className);
            // }

            int[] dimensionsArr = dimensions.stream().mapToInt(i -> i).toArray();
            boolean isSpecificLength = false;
            boolean isNotSpecificLength = false;
            for (int i : dimensionsArr) {
                if (i < 0) {
                    if (isSpecificLength) {
                        logger.error("不允许单个维度不指定长度");
                        throw new IllegalArgumentException(
                                "Array dimensions must be all specified if one of them is specified");
                    }
                    isNotSpecificLength = true;
                } else {
                    if (isNotSpecificLength) {
                        logger.error("不允许单个维度不指定长度");
                        throw new IllegalArgumentException(
                                "Array dimensions must be all specified if one of them is specified");
                    }
                    isSpecificLength = true;
                }
            }
            Object initialValue = arrInitial != null ? arrInitial.evaluate(context) : null;
            if (isSpecificLength) {
                Object array = Array.newInstance(elementClass, dimensionsArr);
                try {
                    if (initialValue != null) {
                        List<List<Integer>> indices = walkArrayDimensions(dimensions);
                        for (List<Integer> index : indices) {
                            Object target = array;
                            for (int i = 0; i < index.size() - 1; i++) {
                                if (target == null)
                                    throw new IndexOutOfBoundsException("Array size mismatch");
                                target = Array.get(target, index.get(i));
                            }
                            if (target == null)
                                throw new IndexOutOfBoundsException("Array size mismatch");
                            Array.set(target, index.get(index.size() - 1), getArrayElement(initialValue, index));
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    logger.error("数组的维度不对应");
                    logger.error("期待的维度：" + dimensions);
                    logger.error("实际维度： " + getArrayDimension(initialValue));
                    throw new IndexOutOfBoundsException(
                            "Array dimensions do not match: " + dimensions + " vs " + getArrayDimension(initialValue));
                }
                return array;
            } else {
                if (initialValue == null) {
                    logger.error("数组既没有指定长度也没有指定初始值");
                    throw new RuntimeException("Creating array with neither length nor initial value");
                }
                List<Integer> origDimensions = getArrayDimension(initialValue);
                if (origDimensions.size() != dimensions.size()) {
                    logger.error("数组的维度不对应");
                    logger.error("期待的维度：" + dimensions);
                    logger.error("实际维度： " + origDimensions);
                    throw new IndexOutOfBoundsException(
                            "Array dimensions do not match: " + dimensions + " vs " + origDimensions);
                }
                Object convertedValue = deepCastArrayElements(context, initialValue, elementClass, 0);
                return convertedValue;
            }
        }

        /**
         * 递归地对数组/列表中的每个元素进行类型转换
         */
        private Object deepCastArrayElements(ExecutionContext context, Object value, Class<?> elementType, int depth) throws Exception {
            if (value == null) {
                return null;
            }
            if (value.getClass().isArray()) {
                List<Integer> dimensions = getArrayDimension(value);
                int len = Array.getLength(value);
                int dim = dimensions.size();
                Class<?> arrayType = elementType;
                int currentDimCount = dim - depth;
                int currentDimSize = dimensions.get(0);
                for (int i = 0; i < currentDimCount - 1; i++) {
                    arrayType = Array.newInstance(arrayType, 1).getClass();
                }
                Object newArray = Array.newInstance(arrayType, currentDimSize);
                for (int i = 0; i < len; i++) {
                    Object elem = Array.get(value, i);
                    Object casted = deepCastArrayElements(context, elem, elementType, depth + 1);
                    Array.set(newArray, i, casted);
                }
                return newArray;
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                List<Object> newList = new ArrayList<>(list.size());
                for (Object elem : list) {
                    Object casted = deepCastArrayElements(context, elem, elementType, depth + 1);
                    newList.add(casted);
                }
                return newList;
            } else {
                // 叶子节点：直接cast
                return context.castObject(value, elementType);
            }
        }

        private List<List<Integer>> walkArrayDimensions(List<Integer> dimensions) {
            if (dimensions.isEmpty()) {
                return Collections.emptyList();
            } else if (dimensions.size() == 1) {
                List<List<Integer>> result = new ArrayList<>();
                for (int i = 0; i < dimensions.get(0); i++) {
                    List<Integer> index = new ArrayList<>();
                    index.add(i);
                    result.add(index);
                }
                return result;
            } else {
                int firstDim = dimensions.get(0);
                dimensions.remove(0);
                List<List<Integer>> subDimensions = walkArrayDimensions(dimensions);
                List<List<Integer>> result = new ArrayList<>();
                for (List<Integer> subDim : subDimensions) {
                    for (int i = 0; i < firstDim; i++) {
                        List<Integer> newDim = new ArrayList<>();
                        newDim.add(i);
                        newDim.addAll(subDim);
                        result.add(newDim);
                    }
                }
                return result;
            }
        }

        private List<Integer> getArrayDimension(Object arr) {
            if (arr.getClass().isArray()) {
                List<Integer> dimensions = new ArrayList<>();
                Object current = arr;
                while (true) {
                    assert current != null;
                    if (!current.getClass().isArray())
                        break;
                    dimensions.add(Array.getLength(current));
                    try {
                        current = Array.get(current, 0);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Class<?> clazz = current.getClass();
                        while (clazz.isArray()) {
                            dimensions.add(0);
                            clazz = clazz.getComponentType();
                        }
                        break;
                    }
                }
                return dimensions;
            } else if (arr instanceof List) {
                return Collections.singletonList(((List<?>) arr).size());
            } else {
                logger.error("尝试访问非数组或列表的维度");
                throw new RuntimeException("Attempt to access dimensions of non-array or list");
            }
        }

        private Object getArrayElement(Object arr, List<Integer> indices) {
            Object target = arr;
            if (target.getClass().isArray()) {
                for (int index : indices) {
                    assert target != null;
                    target = Array.get(target, index);
                }
                return target;
            } else if (arr instanceof List) {
                return ((List<?>) arr).get(indices.get(0));
            } else {
                logger.error("尝试访问非数组或列表的元素");
                throw new RuntimeException("Attempt to access element of non-array or list");
            }
        }
    }

    public static class MemberAccessNode extends ASTNode {
        private final ASTNode target;
        private final String memberName;

        public MemberAccessNode(ASTNode target, String memberName) {
            this.target = target;
            this.memberName = memberName;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object targetObj;
            try {
                targetObj = target.evaluate(context);
            } catch (RuntimeException e) {
                // 如果target评估失败，尝试将整个表达式解析为类名
                String className = buildClassName(target, memberName);
                if (context.hasClass(className)) {
                    return context.findClass(className);
                }
                throw e;
            }

            // 特殊处理：如果字段名是 "class"，直接返回目标类的Class对象
            if (memberName.equals("class")) {
                if (targetObj == null) {
                    logger.error("无法在null上访问.class字段");
                    throw new NullPointerException("Cannot access .class on null");
                }
                return targetObj instanceof Class ? targetObj : targetObj.getClass();
            }

            // 处理自定义类实例的字段访问
            if (targetObj instanceof CustomClassInstance customInstance) {
                ClassDefinition classDef = customInstance.getClassDefinition();
                FieldDefinition fieldDef = findFieldInHierarchy(classDef, memberName, context);
                if (fieldDef != null) {
                    return customInstance.getField(memberName);
                } else {
                    throw new RuntimeException(
                            "Field '" + memberName + "' not found in class hierarchy " + classDef.getClassName());
                }
            }

            if (targetObj instanceof Class) {
                Class<?> clazz = (Class<?>) targetObj;
                try {
                    // 首先尝试作为静态字段访问
                    Field field = findField(clazz, memberName);
                    if (field != null && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        return handleClassMember(clazz, memberName, context);
                    }
                } catch (NoSuchFieldException ignored) {
                }

                try {
                    // 然后尝试作为嵌套类访问
                    String nestedClassName = clazz.getName() + "." + memberName;
                    if (context.hasClass(nestedClassName)) {
                        return context.findClass(nestedClassName);
                    }
                } catch (Exception ignored) {
                }

                // 如果都不是，视为实例成员访问
                return handleInstanceMember(targetObj, memberName, context);
            } else {
                // 实例成员访问：字段
                return handleInstanceMember(targetObj, memberName, context);
            }
        }

        private String buildClassName(ASTNode node, String memberName) {
            StringBuilder sb = new StringBuilder();
            if (node instanceof VariableNode) {
                sb.append(((VariableNode) node).name);
            } else if (node instanceof MemberAccessNode) {
                MemberAccessNode memberNode = (MemberAccessNode) node;
                sb.append(buildClassName(memberNode.target, memberNode.memberName));
            } else if (node instanceof ClassReferenceNode) {
                sb.append(((ClassReferenceNode) node).getClassName());
            }
            sb.append('.').append(memberName);
            return sb.toString();
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            Object targetObj = target != null ? target.evaluate(context) : null;
            Class<?> targetClass;

            if (targetObj instanceof Class) {
                targetClass = (Class<?>) targetObj;
            } else if (targetObj != null) {
                targetClass = targetObj.getClass();
            } else if (target instanceof ClassReferenceNode) {
                targetClass = ((ClassReferenceNode) target).getClass(context);
            } else {
                logger.error("无法解析所访问类成员的类型目标: " + memberName);
                throw new RuntimeException("Cannot determine target for member access: " + memberName);
            }

            // 特殊处理数组的length字段
            if (targetObj != null && targetObj.getClass().isArray() && "length".equals(memberName)) {
                return Integer.class;
            }

            // 尝试查找字段
            try {
                Field field = findField(targetClass, memberName);
                return field.getType();
            } catch (NoSuchFieldException e) {
                // 如果不是字段，可能是嵌套类
                if (targetObj instanceof Class) {
                    String nestedClassName = ((Class<?>) targetObj).getName() + "." + memberName;
                    if (context.hasClass(nestedClassName)) {
                        return Class.class;
                    }
                }
                throw new RuntimeException("Member '" + memberName + "' not found in " + targetClass.getName());
            }
        }

        private Object handleClassMember(Class<?> clazz, String memberName, ExecutionContext context) throws Exception {
            // 首先检查是否是嵌套类
            String nestedClassName = clazz.getName() + "." + memberName;
            if (context.hasClass(nestedClassName)) {
                return context.findClass(nestedClassName);
            }

            // 否则作为静态字段处理
            Field field = findField(clazz, memberName);
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new RuntimeException("Field '" + memberName + "' is not static in class " + clazz.getName());
            }

            if (!Modifier.isPublic(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                } catch (Exception e) {
                    logger.warn("无法设置字段可访问性: " + field.getName() + ", 将尝试直接访问");
                }
            }

            Object result = field.get(null);
            return result;
        }

        private Object handleInstanceMember(Object targetObj, String memberName, ExecutionContext context)
                throws Exception {
            Class<?> targetClass = targetObj.getClass();

            // 特殊处理数组的length字段
            if (targetObj.getClass().isArray() && "length".equals(memberName)) {
                return java.lang.reflect.Array.getLength(targetObj);
            }

            Field field = findField(targetClass, memberName);

            if (!Modifier.isPublic(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                } catch (Exception e) {
                    logger.warn("无法设置字段可访问性: " + field.getName() + ", 将尝试直接访问");
                }
            }

            return field.get(targetObj);
        }

        private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
            // 首先尝试查找公共字段
            try {
                return clazz.getField(fieldName);
            } catch (NoSuchFieldException e) {
                // 如果公共字段不存在，查找所有声明的字段
                try {
                    return clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e2) {
                    // 尝试在父类中查找
                    Class<?> superClass = clazz.getSuperclass();
                    if (superClass != null) {
                        return findField(superClass, fieldName);
                    }
                    throw e2;
                }
            }
        }
    }

    public static class FieldAccessNode extends ASTNode {
        final ASTNode target;
        final String fieldName;

        public FieldAccessNode(ASTNode target, String fieldName) {
            this.target = target;
            this.fieldName = fieldName;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object targetObj = target != null ? target.evaluate(context) : null;
            Class<?> targetClass;

            // 特殊处理：如果字段名是 "class"，直接返回目标类的Class对象
            if (fieldName.equals("class")) {
                if (targetObj == null) {
                    logger.error("无法在null上访问.class字段");
                    throw new NullPointerException("Cannot access .class on null");
                }
                return targetObj instanceof Class ? targetObj : targetObj.getClass();
            }

            // 处理自定义类实例的字段访问
            if (targetObj instanceof CustomClassInstance customInstance) {
                ClassDefinition classDef = customInstance.getClassDefinition();

                // 在类的继承层次结构中查找字段
                FieldDefinition fieldDef = findFieldInHierarchy(classDef, fieldName, context);
                if (fieldDef != null) {
                    // 返回字段值
                    return customInstance.getField(fieldName);
                } else {
                    throw new RuntimeException(
                            "Field '" + fieldName + "' not found in class hierarchy " + classDef.getClassName());
                }
            }

            // 确定目标类
            if (targetObj instanceof Class) {
                // 静态字段访问
                targetClass = (Class<?>) targetObj;
            } else if (targetObj != null) {
                // 实例字段访问
                targetClass = targetObj.getClass();
            } else if (target instanceof ClassReferenceNode) {
                // 静态字段访问: 类引用节点
                targetClass = ((ClassReferenceNode) target).getClass(context);
            } else {
                logger.error("无法解析所访问类成员的类型目标: " + fieldName);
                throw new RuntimeException("Cannot determine target for field access: " + fieldName);
            }

            // 特殊处理数组的length字段
            if (targetObj != null && targetObj.getClass().isArray() && "length".equals(fieldName)) {
                return Array.getLength(targetObj);
            }

            Field field = findField(targetClass, fieldName);
            if (!Modifier.isPublic(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                } catch (Exception e) {
                    logger.warn("无法设置字段可访问性: " + field.getName() + ", 将尝试直接访问");
                }
            }

            // 检查字段是否是静态的
            boolean isStatic = Modifier.isStatic(field.getModifiers());

            // 如果是静态字段，使用 null 作为接收者；否则使用 targetObj
            Object receiver = isStatic ? null : targetObj;

            if (receiver == null && !isStatic) {
                logger.error("尝试在 null 对象上访问实例字段: " + fieldName);
                throw new NullPointerException("Attempt to access instance field '" + fieldName + "' on null object");
            }

            return field.get(receiver);
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            Object targetObj = target != null ? target.evaluate(context) : null;
            Class<?> targetClass;

            if (targetObj instanceof Class) {
                targetClass = (Class<?>) targetObj;
            } else if (targetObj != null) {
                targetClass = targetObj.getClass();
            } else if (target instanceof ClassReferenceNode) {
                targetClass = ((ClassReferenceNode) target).getClass(context);
            } else {
                logger.error("无法解析所访问类成员的类型目标: " + fieldName);
                throw new RuntimeException("Cannot determine target for field access: " + fieldName);
            }

            Field field = findField(targetClass, fieldName);
            return field.getType();
        }

        private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
            // 首先尝试查找公共字段
            try {
                return clazz.getField(fieldName);
            } catch (NoSuchFieldException e) {
                // 如果公共字段不存在，查找所有声明的字段
                try {
                    return clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e2) {
                    // 尝试在父类中查找
                    Class<?> superClass = clazz.getSuperclass();
                    if (superClass != null) {
                        return findField(superClass, fieldName);
                    }
                    throw e2;
                }
            }
        }

        /**
         * 在类的继承层次结构中递归查找字段
         */
        private FieldDefinition findFieldInHierarchy(ClassDefinition classDef, String fieldName,
                ExecutionContext context) {
            // 首先在当前类中查找字段
            FieldDefinition fieldDef = classDef.getField(fieldName);
            if (fieldDef != null) {
                return fieldDef;
            }

            // 如果当前类没有找到，检查父类
            String superClassName = classDef.getSuperClassName();
            if (superClassName != null) {
                ClassDefinition superClassDef = context.customClasses.get(superClassName);
                if (superClassDef != null) {
                    return findFieldInHierarchy(superClassDef, fieldName, context);
                }
            }

            // 如果没有找到，返回null
            return null;
        }
    }

    public static class ArrayAccessNode extends ASTNode {
        final ASTNode target;
        final ASTNode index;

        public ArrayAccessNode(ASTNode target, ASTNode index) {
            this.target = target;
            this.index = index;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object targetObj = target.evaluate(context);
            Object indexObj = index.evaluate(context);

            if (targetObj == null) {
                logger.error("尝试在null数组上访问元素");
                throw new NullPointerException("Cannot access array element on null");
            }

            if (!targetObj.getClass().isArray()) {
                logger.error("目标不是数组类型: " + targetObj.getClass().getName());
                throw new RuntimeException("Target is not an array: " + targetObj.getClass().getName());
            }

            if (!(indexObj instanceof Number)) {
                logger.error("数组索引必须是数字类型: " + indexObj.getClass().getName());
                throw new RuntimeException("Array index must be a number: " + indexObj.getClass().getName());
            }

            int idx = ((Number) indexObj).intValue();
            int length = Array.getLength(targetObj);

            if (idx < 0 || idx >= length) {
                logger.error("数组索引越界: " + idx + ", 数组长度: " + length);
                throw new ArrayIndexOutOfBoundsException(
                        "Array index out of bounds: " + idx + ", array length: " + length);
            }

            return Array.get(targetObj, idx);
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            Object targetObj = target.evaluate(context);
            if (targetObj == null) {
                logger.error("无法解析null数组的元素类型");
                throw new NullPointerException("Cannot determine element type of null array");
            }

            if (!targetObj.getClass().isArray()) {
                logger.error("目标不是数组类型: " + targetObj.getClass().getName());
                throw new RuntimeException("Target is not an array: " + targetObj.getClass().getName());
            }

            return targetObj.getClass().getComponentType();
        }
    }

    public static class VariableAssignmentNode extends ASTNode {
        private final String variableName;
        private final ASTNode value;

        public VariableAssignmentNode(String variableName, ASTNode value) {
            this.variableName = variableName;
            this.value = value;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if (context.builtIns.containsKey(variableName)) {
                logger.error("变量" + variableName + "是内置变量，无法赋值");
                throw new RuntimeException("Shadowing built-in: " + variableName);
            }
            if (context.hasClass(variableName)) {
                logger.error("变量" + variableName + "是类名，无法赋值");
                throw new RuntimeException("Variable name conflicts with class name: " + variableName);
            }

            Object val = value.evaluate(context);
            if (!context.hasVariable(variableName)) {
                logger.error("变量" + variableName + "未声明");
                throw new RuntimeException("Variable not declared: " + variableName);
            }
            try {
                context.setVariable(variableName, context.castObject(val, context.getVariableType(variableName)));
            } catch (ClassCastException e) {
                logger.error("变量" + variableName + "的类型与赋值类型不匹配也不兼容, "
                        + val.getClass().getName() + " != " + context.getVariableType(variableName).getName());
                throw new RuntimeException("Type mismatch: " + variableName +
                        "(" + context.getVariableType(variableName).getName() + " vs " + val.getClass().getName()
                        + ")");
            }
            context.setVariable(variableName, val);
            return val;
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            return value.getType(context);
        }
    }

    /**
     * 字段赋值节点，用于处理自定义类实例的字段赋值
     */
    public static class FieldAssignmentNode extends ASTNode {
        private final ASTNode target;
        private final String fieldName;
        private final ASTNode value;

        public FieldAssignmentNode(ASTNode target, String fieldName, ASTNode value) {
            this.target = target;
            this.fieldName = fieldName;
            this.value = value;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object targetObj = target.evaluate(context);
            Object val = value.evaluate(context);

            if (targetObj instanceof CustomClassInstance customInstance) {
                ClassDefinition classDef = customInstance.getClassDefinition();
                FieldDefinition fieldDef = findFieldInHierarchy(classDef, fieldName, context);
                if (fieldDef == null) {
                    throw new RuntimeException(
                            "Field '" + fieldName + "' not found in class hierarchy " + classDef.getClassName());
                }
                customInstance.setField(fieldName, val);
            } else {
                Field field = targetObj.getClass().getField(fieldName);
                field.set(targetObj, val);
            }
            return val;
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            return value.getType(context);
        }

        /**
         * 在类的继承层次结构中递归查找字段
         */
        private FieldDefinition findFieldInHierarchy(ClassDefinition classDef, String fieldName,
                ExecutionContext context) {
            // 首先在当前类中查找字段
            FieldDefinition fieldDef = classDef.getField(fieldName);
            if (fieldDef != null) {
                return fieldDef;
            }

            // 如果当前类没有找到，检查父类
            String superClassName = classDef.getSuperClassName();
            if (superClassName != null) {
                ClassDefinition superClassDef = context.customClasses.get(superClassName);
                if (superClassDef != null) {
                    return findFieldInHierarchy(superClassDef, fieldName, context);
                }
            }

            // 如果没有找到，返回null
            return null;
        }
    }

    public static class ArrayAssignmentNode extends ASTNode {
        private final ASTNode target;
        private final ASTNode index;
        private final ASTNode value;

        public ArrayAssignmentNode(ASTNode target, ASTNode index, ASTNode value) {
            this.target = target;
            this.index = index;
            this.value = value;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object targetObj = target.evaluate(context);
            Object indexObj = index.evaluate(context);
            Object val = value.evaluate(context);

            if (targetObj == null) {
                logger.error("尝试在null数组上赋值");
                throw new NullPointerException("Cannot assign to null array");
            }

            if (!targetObj.getClass().isArray()) {
                logger.error("目标不是数组类型: " + targetObj.getClass().getName());
                throw new RuntimeException("Target is not an array: " + targetObj.getClass().getName());
            }

            if (!(indexObj instanceof Number)) {
                logger.error("数组索引必须是数字类型: " + indexObj.getClass().getName());
                throw new RuntimeException("Array index must be a number: " + indexObj.getClass().getName());
            }

            int idx = ((Number) indexObj).intValue();
            int length = Array.getLength(targetObj);

            if (idx < 0 || idx >= length) {
                logger.error("数组索引越界: " + idx + ", 数组长度: " + length);
                throw new ArrayIndexOutOfBoundsException(
                        "Array index out of bounds: " + idx + ", array length: " + length);
            }

            Array.set(targetObj, idx, val);
            return val;
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            return value.getType(context);
        }
    }

    public static class ClassDeclarationNode extends ASTNode {
        private final String className;
        private final ClassDefinition classDef;

        public ClassDeclarationNode(String className) {
            this.className = className;
            this.classDef = new ClassDefinition(className);
        }

        public ClassDeclarationNode(String className, ClassDefinition classDef) {
            this.className = className;
            this.classDef = classDef;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if (context.builtIns.containsKey(className)) {
                logger.error("类名" + className + "是内置成员，无法声明");
                throw new RuntimeException("Class name shadowing built-in: " + className);
            } else if (context.customClasses.containsKey(className)) {
                logger.error("类名" + className + "已声明，无法重复声明");
                throw new RuntimeException("Class already declared: " + className);
            } else if (context.hasVariable(className)) {
                logger.error("变量" + className + "已声明，无法重复声明");
                throw new RuntimeException("Variable already declared: " + className);
            } else if (context.hasClass(className)) {
                logger.error("类名" + className + "已声明，无法重复声明");
                throw new RuntimeException("Class already declared: " + className);
            } else if (isKeyword(className)) {
                logger.error(className + "是关键字，不能作为类名");
                throw new RuntimeException("Cannot use a keyword as class name: " + className);
            }
            context.customClasses.put(className, classDef);
            return null;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class VariableDeclarationNode extends ASTNode {
        private final String typeName;
        private final String variableName;
        private final ASTNode initialValue;

        public VariableDeclarationNode(String typeName, String variableName, ASTNode initialValue) {
            this.typeName = typeName;
            this.variableName = variableName;
            this.initialValue = initialValue;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if (context.builtIns.containsKey(variableName)) {
                logger.error("变量" + variableName + "是内置变量，无法声明");
                throw new RuntimeException("Variable name shadowing built-in: " + variableName);
            } else if (context.customClasses.containsKey(variableName)) {
                logger.error("变量" + variableName + "是自定义类名，无法声明");
                throw new RuntimeException("Variable name conflicts with defined class name: " + variableName);
            } else if (context.hasVariable(variableName)) {
                logger.error("变量" + variableName + "已声明，无法重复声明");
                throw new RuntimeException("Variable already declared: " + variableName);
            } else if (context.hasClass(variableName)) {
                logger.error("变量" + variableName + "是类名，无法声明");
                throw new RuntimeException("Variable name conflicts with class name: " + variableName);
            } else if (isKeyword(variableName)) {
                logger.error(variableName + "是关键字，不能作为变量名");
                throw new RuntimeException("Cannot use a keyword as variable name: " + variableName);
            }
            Object value = null;
            Class<?> clazz;
            if (initialValue != null) {
                if (initialValue instanceof LambdaNode lambdaNode) {
                    // 检查变量声明的类型
                    if (!typeName.equals("auto")) {
                        if (context.hasClass(typeName)) {
                            LambdaNode newLambdaNode = new LambdaNode(
                                    lambdaNode.parameters,
                                    lambdaNode.body,
                                    typeName);
                            value = newLambdaNode.evaluate(context);
                        } else {
                            value = initialValue.evaluate(context);
                        }
                    } else {
                        // 提供了auto类型，当作一般值解析
                        value = initialValue.evaluate(context);
                    }
                } else {
                    // 不是lambda表达式，正常解析
                    value = initialValue.evaluate(context);
                }
            }
            if (typeName.equals("auto")) {
                if (value == null) {
                    logger.error("变量" + variableName + "未指定类型且没有初始值");
                    throw new RuntimeException("Variable declaration without type and initial value: " + variableName);
                }
                clazz = value.getClass();
            } else {
                try {
                    clazz = context.findClass(typeName);
                } catch (CustomClassException e) {
                    // 对于自定义类，跳过类型检查，因为自定义类没有对应的Java类
                    clazz = Object.class;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Cannot resolve type for variable declaration: " + typeName);
                }
            }

            try {
                if (!clazz.equals(Object.class)) {
                    value = context.castObject(value, clazz);
                }
            } catch (ClassCastException e) {
                String s = value == null ? "null" : value.getClass().getName();
                throw new RuntimeException("Initial value type mismatch (declared " + clazz.getName() +
                        ", got " + s + ") for variable declaration: " + variableName);
            }

            context.setVariable(variableName, value);
            return value;
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            if (typeName.equals("auto")) {
                return Object.class;
            }
            return context.findClass(getArrayTypeNameWithoutLength(typeName));
        }
    }

    public static class BlockNode extends ASTNode {
        private final List<ASTNode> statements = new ArrayList<>();
        private boolean isIndependent;

        BlockNode() {
            isIndependent = false;
        }

        BlockNode(boolean isIndependent) {
            this.isIndependent = isIndependent;
        }

        public void addStatement(ASTNode statement) {
            statements.add(statement);
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object lastResult = null;
            if (isIndependent) context.enterScope();
            try {
                for (int i = 0; i < statements.size(); i++) {
                    ASTNode statement = statements.get(i);
                    lastResult = statement.evaluate(context);
                    if (context.shouldReturn) {
                        return context.returnValue;
                    }
                    if (context.shouldBreak || context.shouldContinue) {
                        break;
                    }
                }
            } finally {
                if (isIndependent) context.exitScope();
            }
            return lastResult;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class IfNode extends ASTNode {
        private final ASTNode condition;
        private final ASTNode thenBranch;
        private final ASTNode elseBranch;

        public IfNode(ASTNode condition, ASTNode thenBranch, ASTNode elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            context.enterScope();

            Object condValue = condition.evaluate(context);
            boolean boolValue = toBoolean(condValue);

            if (boolValue) {
                Object result = thenBranch != null ? thenBranch.evaluate(context) : null;
                if (context.shouldReturn) {
                    Object returnValue = context.returnValue;
                    context.exitScope();
                    return returnValue;
                }
                if (context.shouldBreak || context.shouldContinue) {
                    context.exitScope();
                    return null;
                }
                context.exitScope();
                return result;
            } else if (elseBranch != null) {
                Object result = elseBranch.evaluate(context);
                if (context.shouldReturn) {
                    Object returnValue = context.returnValue;
                    context.exitScope();
                    return returnValue;
                }
                if (context.shouldBreak || context.shouldContinue) {
                    context.exitScope();
                    return null;
                }
                context.exitScope();
                return result;
            }
            context.exitScope();
            return null;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class WhileNode extends ASTNode {

        private final ASTNode condition;
        private final ASTNode body;
        public static int MAX_LOOPS = 1024;

        public WhileNode(ASTNode condition, ASTNode body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object lastResult = null;
            int loopCount = 0;

            // 保存控制流状态
            boolean originalBreak = context.shouldBreak;
            boolean originalContinue = context.shouldContinue;
            boolean originalReturn = context.shouldReturn;

            try {
                context.shouldBreak = false;
                context.shouldContinue = false;
                context.shouldReturn = false;
                context.enterScope();

                while (loopCount < MAX_LOOPS) {

                    // 评估条件（每次循环都要重新评估）
                    if (!toBoolean(condition.evaluate(context)))
                        break;

                    // 执行循环体
                    lastResult = body.evaluate(context);

                    if (context.shouldBreak) {
                        context.shouldBreak = false;
                        break;
                    } else if (context.shouldContinue) {
                        context.shouldContinue = false; // continue应该已经被处理了，所以这里不需要重置
                    } else if (context.shouldReturn) {
                        return context.returnValue; // 如果遇到return语句，直接返回实际的返回值并退出循环
                    }
                    loopCount++;

                    if (loopCount >= MAX_LOOPS) {
                        logger.warn("警告: while循环达到最大限制 (" + MAX_LOOPS + ")，自动退出");
                        context.printWarn("While loop reached its limit(" + MAX_LOOPS + "), force quited" + "\n");
                        break;
                    }
                }
                return lastResult;
            } finally {
                // 恢复控制流状态
                context.shouldBreak = originalBreak;
                context.shouldContinue = originalContinue;
                context.exitScope();
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class ForNode extends ASTNode {
        private final ASTNode init;
        private final ASTNode condition;
        private final ASTNode update;
        private final ASTNode body;
        public static int MAX_LOOPS = 1024;

        public ForNode(ASTNode init, ASTNode condition, ASTNode update, ASTNode body) {
            this.init = init;
            this.condition = condition;
            this.update = update;
            this.body = body;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object lastResult = null;
            int loopCount = 0;

            boolean origBreak = context.shouldBreak;
            boolean origContinue = context.shouldContinue;
            context.enterScope();

            context.shouldBreak = false;
            context.shouldContinue = false;

            try {
                if (init != null) {
                    init.evaluate(context);
                }

                while (loopCount < MAX_LOOPS) {
                    if (condition != null && !toBoolean(condition.evaluate(context))) {
                        break;
                    }

                    lastResult = body.evaluate(context);

                    if (context.shouldBreak) {
                        context.shouldBreak = false;
                        break;
                    }

                    if (context.shouldReturn) {
                        return context.returnValue;
                    }

                    if (update != null) {
                        update.evaluate(context);
                    }

                    if (context.shouldContinue) {
                        context.shouldContinue = false;
                    }

                    loopCount++;
                }

                if (loopCount >= MAX_LOOPS) {
                    logger.warn("For循环达到最大限制 (" + MAX_LOOPS + ")，可能陷入无限循环，已经强制退出");
                    context.printWarn("For loop reached its limit (" + MAX_LOOPS + "), force quited" + "\n");
                }
                return lastResult;

            } finally {
                context.shouldBreak = origBreak;
                context.shouldContinue = origContinue;
                context.exitScope();
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class ForEachNode extends ASTNode {
        private final String className;
        private final String itemName;
        private final ASTNode collection;
        private final ASTNode body;
        public static int MAX_LOOPS = 1024;

        public ForEachNode(String className, String itemName, ASTNode collection, ASTNode body) {
            this.className = className;
            this.itemName = itemName;
            this.collection = collection;
            this.body = body;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object coll = collection.evaluate(context);
            Object lastResult = null;
            int loopCount = 0;

            boolean origBreak = context.shouldBreak;
            boolean origContinue = context.shouldContinue;

            context.shouldBreak = false;
            context.shouldContinue = false;

            try {
                context.enterScope();
                Class<?> clazz = null;
                if ("auto".equals(className)) {
                    // 使用auto关键字，不进行类型转换
                } else {
                    clazz = context.findClass(className);
                }

                if (coll == null) {
                    logger.error("for-each 的集合为 null");
                    throw new NullPointerException("Cannot iterate over null collection");
                }

                if (coll instanceof Object[]) {
                    for (Object item : (Object[]) coll) {
                        if (loopCount >= MAX_LOOPS)
                            break;

                        if (clazz != null) {
                            item = context.castObject(item, clazz);
                        }

                        context.setVariable(itemName, item);

                        lastResult = body.evaluate(context);

                        if (context.shouldBreak) {
                            context.shouldBreak = false;
                            break;
                        }

                        if (context.shouldContinue) {
                            context.shouldContinue = false;
                        }

                        if (context.shouldReturn) {
                            return context.returnValue;
                        }

                        loopCount++;
                    }
                } else if (coll instanceof Iterable) {
                    for (Object item : (Iterable<?>) coll) {
                        if (loopCount >= MAX_LOOPS)
                            break;

                        item = context.castObject(item, clazz);
                        context.setVariable(itemName, item);
                        lastResult = body.evaluate(context);

                        if (context.shouldBreak) {
                            context.shouldBreak = false;
                            break;
                        }

                        if (context.shouldContinue) {
                            context.shouldContinue = false;
                        }

                        if (context.shouldReturn) {
                            return context.returnValue;
                        }

                        loopCount++;
                    }
                } else if (coll instanceof String) {
                    for (char ch : ((String) coll).toCharArray()) {
                        if (loopCount >= MAX_LOOPS)
                            break;

                        context.setVariable(itemName, ch);
                        lastResult = body.evaluate(context);

                        if (context.shouldBreak) {
                            context.shouldBreak = false;
                            break;
                        }

                        if (context.shouldContinue) {
                            context.shouldContinue = false;
                        }

                        if (context.shouldReturn) {
                            return context.returnValue;
                        }

                        loopCount++;
                    }
                } else {
                    logger.error("无法在类型" + coll.getClass().getName() + "上迭代");
                    throw new RuntimeException("Cannot iterate over: " +
                            coll.getClass().getName());
                }

                if (loopCount >= MAX_LOOPS) {
                    logger.warn("For-each循环达到最大限制 (" + MAX_LOOPS + ")");
                    context.printWarn("For-each loop reached its limit (" + MAX_LOOPS + "), force quited" + "\n");
                }

                return lastResult;
            } finally {
                context.shouldBreak = origBreak;
                context.shouldContinue = origContinue;
                context.exitScope();
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class DoWhileNode extends ASTNode {
        private final ASTNode condition;
        private final ASTNode body;
        public static int MAX_LOOPS = 1024;

        public DoWhileNode(ASTNode condition, ASTNode body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object lastResult = null;
            int loopCount = 0;

            boolean originalBreak = context.shouldBreak;
            boolean originalContinue = context.shouldContinue;
            boolean originalReturn = context.shouldReturn;

            try {
                context.shouldBreak = false;
                context.shouldContinue = false;
                context.shouldReturn = false;

                do {
                    if (loopCount >= MAX_LOOPS) {
                        logger.warn("警告: do-while循环达到最大限制 (" + MAX_LOOPS + ")，自动退出");
                        context.printWarn("Do-while loop reached its limit(" + MAX_LOOPS + "), force quited" + "\n");
                        break;
                    }

                    lastResult = body.evaluate(context);

                    if (context.shouldBreak) {
                        context.shouldBreak = false;
                        break;
                    } else if (context.shouldContinue) {
                        context.shouldContinue = false;
                    } else if (context.shouldReturn) {
                        return context.returnValue;
                    }
                    loopCount++;
                } while (toBoolean(condition.evaluate(context)));

                return lastResult;
            } finally {
                context.shouldBreak = originalBreak;
                context.shouldContinue = originalContinue;
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class SwitchNode extends ASTNode {
        private final ASTNode expression;
        private final List<CaseNode> cases;
        private final ASTNode defaultCase;

        public SwitchNode(ASTNode expression, List<CaseNode> cases, ASTNode defaultCase) {
            this.expression = expression;
            this.cases = cases;
            this.defaultCase = defaultCase;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object value = expression.evaluate(context);
            boolean matched = false;

            for (CaseNode caseNode : cases) {
                if (matched || isValueEqual(value, caseNode.getValue().evaluate(context))) {
                    matched = true;
                    Object result = caseNode.getBody().evaluate(context);
                    if (context.shouldBreak) {
                        context.shouldBreak = false;
                        break;
                    }
                    if (context.shouldReturn) {
                        return context.returnValue;
                    }
                }
            }

            if (!matched && defaultCase != null) {
                Object result = defaultCase.evaluate(context);
                if (context.shouldReturn) {
                    return context.returnValue;
                }
            }

            return null;
        }

        private boolean isValueEqual(Object value1, Object value2) {
            if (value1 == null && value2 == null) {
                return true;
            }
            if (value1 == null || value2 == null) {
                return false;
            }

            if (value1 instanceof Number n1 && value2 instanceof Number n2) {
                return n1.doubleValue() == n2.doubleValue();
            }

            return value1.equals(value2);
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class CaseNode extends ASTNode {
        private final ASTNode value;
        private final ASTNode body;

        public CaseNode(ASTNode value, ASTNode body) {
            this.value = value;
            this.body = body;
        }

        public ASTNode getValue() {
            return value;
        }

        public ASTNode getBody() {
            return body;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            return body.evaluate(context);
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class ControlNode extends ASTNode {
        private final String type; // "break", "continue" 或者 "return"
        private final ASTNode value; // 如果是return类型包含返回值

        public ControlNode(String type, ASTNode value) {
            this.type = type;
            this.value = value;
        }

        public ControlNode(String type) {
            this.type = type;
            this.value = null;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if ("break".equals(type)) {
                context.shouldBreak = true;
                return null;
            } else if ("continue".equals(type)) {
                context.shouldContinue = true;
                return null;
            } else if ("return".equals(type)) {
                context.shouldReturn = true;
                if (value != null) {
                    Object returnValue = value.evaluate(context);
                    context.returnValue = returnValue;
                    String expectedReturnType = context.getCurrentMethodReturnType();
                    if (!isTypeCompatible(returnValue.getClass(), context.findClass(expectedReturnType))) {
                        String actualType = returnValue != null ? returnValue.getClass().getSimpleName() : "null";
                        logger.error("返回值类型不匹配: 期望 '" + expectedReturnType + "', 实际 '" + actualType + "'");
                        throw new RuntimeException(
                                "Return value type mismatch: expected " + expectedReturnType + ", got " + actualType);
                    }

                    return returnValue;
                } else {
                    context.returnValue = null;

                    String expectedReturnType = context.getCurrentMethodReturnType();
                    if (expectedReturnType != null && !"void".equals(expectedReturnType)) {
                        throw new RuntimeException(
                                "Return value type mismatch: expected " + expectedReturnType + ", got void");
                    }

                    return null;
                }
            } else {
                logger.error("未知的控制类型: " + type);
                throw new RuntimeException("Unknown control type: " + type);
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class TryCatchNode extends ASTNode {
        private final ASTNode tryBlock;
        private final List<CatchBlock> catchBlocks;
        private final ASTNode finallyBlock;

        public static class CatchBlock {
            private final String exceptionType;
            private final String exceptionName;
            private final ASTNode catchBlock;

            public CatchBlock(String exceptionType, String exceptionName, ASTNode catchBlock) {
                this.exceptionType = exceptionType;
                this.exceptionName = exceptionName;
                this.catchBlock = catchBlock;
            }

            public String getExceptionType() {
                return exceptionType;
            }

            public String getExceptionName() {
                return exceptionName;
            }

            public ASTNode getCatchBlock() {
                return catchBlock;
            }
        }

        public TryCatchNode(ASTNode tryBlock, List<CatchBlock> catchBlocks, ASTNode finallyBlock) {
            this.tryBlock = tryBlock;
            this.catchBlocks = catchBlocks;
            this.finallyBlock = finallyBlock;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object result = null;
            Throwable caughtException = null;
            boolean shouldExecuteFinally = true;
            boolean matchedCatch = false;

            context.enterScope();

            try {
                result = tryBlock.evaluate(context);

                if (context.shouldReturn) {
                    Object returnValue = context.returnValue;
                    if (finallyBlock != null) {
                        context.shouldReturn = false;
                        finallyBlock.evaluate(context);
                        context.shouldReturn = true;
                    }
                    context.exitScope();
                    return returnValue;
                }

                if (context.shouldBreak || context.shouldContinue) {
                    if (finallyBlock != null) {
                        boolean originalShouldBreak = context.shouldBreak;
                        boolean originalShouldContinue = context.shouldContinue;
                        context.shouldBreak = false;
                        context.shouldContinue = false;
                        finallyBlock.evaluate(context);
                        context.shouldBreak = originalShouldBreak;
                        context.shouldContinue = originalShouldContinue;
                    }
                    context.exitScope();
                    return null;
                }
            } catch (Throwable e) {
                caughtException = e;

                for (CatchBlock catchBlock : catchBlocks) {
                    try {
                        Class<?> exceptionClass = context.findClass(catchBlock.getExceptionType());
                        if (exceptionClass != null && exceptionClass.isAssignableFrom(e.getClass())) {
                            matchedCatch = true;

                            context.exitScope();
                            context.enterScope();

                            context.setVariable(catchBlock.getExceptionName(), e);

                            Object catchResult = catchBlock.getCatchBlock().evaluate(context);

                            if (context.shouldReturn) {
                                Object returnValue = context.returnValue;
                                if (finallyBlock != null) {
                                    context.shouldReturn = false;
                                    finallyBlock.evaluate(context);
                                    context.shouldReturn = true;
                                }
                                context.exitScope();
                                return returnValue;
                            }

                            if (context.shouldBreak || context.shouldContinue) {
                                if (finallyBlock != null) {
                                    boolean originalShouldBreak = context.shouldBreak;
                                    boolean originalShouldContinue = context.shouldContinue;
                                    context.shouldBreak = false;
                                    context.shouldContinue = false;
                                    finallyBlock.evaluate(context);
                                    context.shouldBreak = originalShouldBreak;
                                    context.shouldContinue = originalShouldContinue;
                                }
                                context.exitScope();
                                return null;
                            }

                            result = catchResult;
                            break;
                        }
                    } catch (ClassNotFoundException ex) {
                        logger.error("无法找到异常类: " + catchBlock.getExceptionType());
                        throw new RuntimeException("Exception class not found: " + catchBlock.getExceptionType(), ex);
                    }
                }

                if (!matchedCatch) {
                    shouldExecuteFinally = false;
                }
            }

            if (finallyBlock != null) {
                if (matchedCatch) {
                    context.exitScope();
                    context.enterScope();
                }

                finallyBlock.evaluate(context);

                if (context.shouldReturn) {
                    Object returnValue = context.returnValue;
                    context.exitScope();

                    // 如果没有匹配的 catch 块，需要重新抛出异常
                    if (!matchedCatch && caughtException != null) {
                        if (caughtException instanceof Exception) {
                            throw (Exception) caughtException;
                        } else if (caughtException instanceof Error) {
                            throw (Error) caughtException;
                        } else {
                            throw new RuntimeException(caughtException);
                        }
                    }

                    return returnValue;
                }

                if (context.shouldBreak || context.shouldContinue) {
                    context.exitScope();

                    // 如果没有匹配的 catch 块，需要重新抛出异常
                    if (!matchedCatch && caughtException != null) {
                        if (caughtException instanceof Exception) {
                            throw (Exception) caughtException;
                        } else if (caughtException instanceof Error) {
                            throw (Error) caughtException;
                        } else {
                            throw new RuntimeException(caughtException);
                        }
                    }

                    return null;
                }
            }

            context.exitScope();

            if (!matchedCatch && caughtException != null) {
                if (caughtException instanceof Exception) {
                    throw (Exception) caughtException;
                } else if (caughtException instanceof Error) {
                    throw (Error) caughtException;
                } else {
                    throw new RuntimeException(caughtException);
                }
            }

            return result;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class ThrowNode extends ASTNode {
        private final ASTNode exception;

        public ThrowNode(ASTNode exception) {
            this.exception = exception;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object exceptionObj = exception.evaluate(context);

            if (exceptionObj == null) {
                logger.error("throw 语句不能抛出 null");
                throw new RuntimeException("Cannot throw null");
            }

            if (exceptionObj instanceof Throwable) {
                Throwable t = (Throwable) exceptionObj;
                if (t instanceof Exception) {
                    throw (Exception) t;
                } else if (t instanceof Error) {
                    throw (Error) t;
                } else {
                    throw new RuntimeException(t);
                }
            } else {
                logger.error("throw 语句只能抛出 Throwable 类型，实际类型: " + exceptionObj.getClass().getName());
                throw new RuntimeException("Can only throw Throwable, got: " + exceptionObj.getClass().getName());
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class IncrementNode extends ASTNode {
        private final String variableName;
        private final boolean isPre; // 是前置还是后置
        private final boolean isIncrement; // 是递增还是递减

        public IncrementNode(String variableName, boolean isPre, boolean isIncrement) {
            this.variableName = variableName;
            this.isPre = isPre;
            this.isIncrement = isIncrement;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws RuntimeException {
            Variable var = context.getVariable(variableName);
            if (var == null) {
                logger.error("尝试在一个未定义的变量" + variableName + "上使用自加或者自减运算");
                throw new RuntimeException("Undefined variable: " + variableName);
            }

            if (!(var.value instanceof Number oldValue)) {
                logger.error("变量" + variableName + "的类型是" + var.type.getName() + "，不能进行自加或者自减运算");
                throw new RuntimeException("Cannot increment/decrement non-numeric variable: " + variableName
                        + " (which is " + var.type.getName() + ")");
            }

            Number newValue;

            if (var.value instanceof Integer) {
                int val = oldValue.intValue();
                newValue = isIncrement ? val + 1 : val - 1;
            } else if (var.value instanceof Long) {
                long val = oldValue.longValue();
                newValue = isIncrement ? val + 1L : val - 1L;
            } else if (var.value instanceof Float) {
                float val = oldValue.floatValue();
                newValue = isIncrement ? val + 1.0f : val - 1.0f;
            } else if (var.value instanceof Double) {
                double val = oldValue.doubleValue();
                newValue = isIncrement ? val + 1.0 : val - 1.0;
            } else {
                logger.error("不支持的数字类型: " + var.value.getClass().getName());
                throw new RuntimeException("Unsupported numeric type: " + var.value.getClass());
            }

            context.setVariable(variableName, newValue);
            return isPre ? newValue : oldValue;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            Variable var = context.getVariable(variableName);
            if (var == null) {
                logger.error("尝试在一个未定义的变量" + variableName + "上使用自加或者自减运算");
                throw new RuntimeException("Undefined variable: " + variableName);
            }
            return var.value.getClass();
        }
    }

    public static class FieldIncrementNode extends ASTNode {
        private final ASTNode target;
        private final String fieldName;
        private final boolean isPre; // 是前置还是后置
        private final boolean isIncrement; // 是递增还是递减

        public FieldIncrementNode(ASTNode target, String fieldName, boolean isPre, boolean isIncrement) {
            this.target = target;
            this.fieldName = fieldName;
            this.isPre = isPre;
            this.isIncrement = isIncrement;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object targetObj = target != null ? target.evaluate(context) : null;

            if (targetObj == null) {
                logger.error("尝试在null对象上访问字段" + fieldName + "进行自增/自减操作");
                throw new NullPointerException("Cannot access field " + fieldName + " on null object");
            }

            // 处理自定义类实例的字段访问
            if (targetObj instanceof CustomClassInstance customInstance) {
                ClassDefinition classDef = customInstance.getClassDefinition();

                // 在类的继承层次结构中查找字段
                FieldDefinition fieldDef = findFieldInHierarchy(classDef, fieldName, context);
                if (fieldDef != null) {
                    Object oldValue = customInstance.getField(fieldDef.getFieldName());

                    if (!(oldValue instanceof Number)) {
                        logger.error("字段" + fieldName + "的类型是" + oldValue.getClass().getName() + "，不能进行自加或者自减运算");
                        throw new RuntimeException("Cannot increment/decrement non-numeric field: " + fieldName);
                    }

                    Number newValue;
                    if (oldValue instanceof Integer) {
                        int val = ((Number) oldValue).intValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Long) {
                        long val = ((Number) oldValue).longValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Float) {
                        float val = ((Number) oldValue).floatValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Double) {
                        double val = ((Number) oldValue).doubleValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Byte) {
                        byte val = ((Number) oldValue).byteValue();
                        newValue = isIncrement ? (byte) (val + 1) : (byte) (val - 1);
                    } else if (oldValue instanceof Short) {
                        short val = ((Number) oldValue).shortValue();
                        newValue = isIncrement ? (short) (val + 1) : (short) (val - 1);
                    } else {
                        logger.error("不支持的数值类型: " + oldValue.getClass().getName());
                        throw new RuntimeException("Unsupported numeric type: " + oldValue.getClass().getName());
                    }

                    customInstance.setField(fieldDef.getFieldName(), newValue);
                    return isPre ? newValue : oldValue;
                } else {
                    logger.error("字段" + fieldName + "在类" + classDef.getClassName() + "中未定义");
                    throw new RuntimeException("Field " + fieldName + " not found in class " + classDef.getClassName());
                }
            } else {
                // 处理普通Java对象的字段访问
                try {
                    Field field = targetObj.getClass().getField(fieldName);
                    Object oldValue = field.get(targetObj);

                    if (!(oldValue instanceof Number)) {
                        logger.error("字段" + fieldName + "的类型是" +
                                (oldValue == null ? "null" : oldValue.getClass().getName()) + "，不能进行自加或者自减运算");
                        throw new RuntimeException("Cannot increment/decrement non-numeric field: " + fieldName);
                    }

                    Number newValue;
                    if (oldValue instanceof Integer) {
                        int val = ((Number) oldValue).intValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Long) {
                        long val = ((Number) oldValue).longValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Float) {
                        float val = ((Number) oldValue).floatValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Double) {
                        double val = ((Number) oldValue).doubleValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Byte) {
                        byte val = ((Number) oldValue).byteValue();
                        newValue = isIncrement ? (byte) (val + 1) : (byte) (val - 1);
                    } else if (oldValue instanceof Short) {
                        short val = ((Number) oldValue).shortValue();
                        newValue = isIncrement ? (short) (val + 1) : (short) (val - 1);
                    } else {
                        logger.error("不支持的数值类型: " + oldValue.getClass().getName());
                        throw new RuntimeException("Unsupported numeric type: " + oldValue.getClass().getName());
                    }

                    field.set(targetObj, newValue);
                    return isPre ? newValue : oldValue;
                } catch (NoSuchFieldException e) {
                    logger.error("字段" + fieldName + "在对象" + targetObj.getClass().getName() + "中未找到");
                    throw new RuntimeException("Field " + fieldName + " not found in " + targetObj.getClass().getName(),
                            e);
                } catch (IllegalAccessException e) {
                    logger.error("无法访问字段" + fieldName + "：" + e.getMessage());
                    throw new RuntimeException("Cannot access field " + fieldName, e);
                }
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            Object targetObj = target != null ? target.evaluate(context) : null;

            if (targetObj == null) {
                throw new NullPointerException("Cannot determine type of field " + fieldName + " on null object");
            }

            if (targetObj instanceof CustomClassInstance customInstance) {
                ClassDefinition classDef = customInstance.getClassDefinition();
                FieldDefinition fieldDef = findFieldInHierarchy(classDef, fieldName, context);
                if (fieldDef != null) {
                    return context.findClass(fieldDef.getTypeName());
                } else {
                    throw new RuntimeException("Field " + fieldName + " not found in class " + classDef.getClassName());
                }
            } else {
                try {
                    Field field = targetObj.getClass().getField(fieldName);
                    return field.getType();
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Field " + fieldName + " not found in " + targetObj.getClass().getName(),
                            e);
                }
            }
        }
    }

    public static class TernaryExprNode extends ASTNode {

        private final ASTNode condition;
        private final ASTNode thenExpr;
        private final ASTNode elseExpr;

        public TernaryExprNode(ASTNode condition, ASTNode thenExpr, ASTNode elseExpr) {
            this.condition = condition;
            this.thenExpr = thenExpr;
            this.elseExpr = elseExpr;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if (!isTypeCompatible(thenExpr.getType(context), elseExpr.getType(context))) {
                logger.error(
                        "三元表达式中的两个表达式类型不匹配，类型分别为：" + thenExpr.getType(context) + " 和 " + elseExpr.getType(context));
                throw new RuntimeException("Types of two expressions in ternary expression do not match: "
                        + thenExpr.getType(context) + " and " + elseExpr.getType(context));
            }
            if (toBoolean(condition.evaluate(context))) {
                return thenExpr.evaluate(context);
            } else {
                return elseExpr.evaluate(context);
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            return thenExpr.getType(context);
        }
    }

    public static class ParenthesizedExpressionNode extends ASTNode {
        final ASTNode expression;

        public ParenthesizedExpressionNode(ASTNode expression) {
            this.expression = expression;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            return expression.evaluate(context);
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            return expression.getType(context);
        }

        @NonNull
        @Override
        public String toString() {
            return "(" + expression + ")";
        }
    }

    public static class CastNode extends ASTNode {
        private final String className;
        private final ASTNode expression;

        public CastNode(String className, ASTNode expression) {
            this.className = className;
            this.expression = expression;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object value = expression.evaluate(context);
            if (value == null) {
                return null;
            }
            try {
                Class<?> targetType = context.findClass(className);
                return context.castObject(value, targetType);
            } catch (ClassNotFoundException e) {
                Variable var = context.getVariable(className);
                if (var == null) {
                    logger.error("尝试转换的类型" + className + "既不是类也不是变量");
                    throw e;
                } else {
                    if (var.type == Class.class) {
                        return context.castObject(value, (Class<?>) var.value);
                    } else {
                        logger.error("尝试用不是Class类的变量" + className + "作为转换目标类");
                        throw new Exception("Try to cast with " + className
                                + " which was found as a variable but not instanceof Class.class");
                    }
                }
            }

        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            return context.findClass(className);
        }

    }

    public static class LambdaNode extends ASTNode {
        private final List<String> parameters;
        private final ASTNode body;
        private final String functionalInterfaceName;

        public LambdaNode(List<String> parameters, ASTNode body, String functionalInterfaceName) {
            this.parameters = parameters;
            this.body = body;
            this.functionalInterfaceName = functionalInterfaceName;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Lambda lambda = createLambda(context);

            if (functionalInterfaceName == null || functionalInterfaceName.isEmpty()
                    || functionalInterfaceName.equals("Lambda")) {
                return lambda;
            }

            Class<?> functionalInterfaceClass = context.findClass(functionalInterfaceName);
            return toFunctionalInterface(lambda, functionalInterfaceClass);

        }

        private Object toFunctionalInterface(Lambda lambda, Class<?> functionalInterfaceClass) throws Exception {
            if (!functionalInterfaceClass.isAnnotationPresent(FunctionalInterface.class)) {
                logger.error("不是函数接口"
                        + functionalInterfaceClass.getName());
                throw new RuntimeException(
                        functionalInterfaceClass + " is not annotated with " + FunctionalInterface.class);
            }

            // 对于常见的函数接口，提供直接转换
            if (functionalInterfaceClass == Supplier.class) {
                checkParameterCount(lambda, 0, Supplier.class.getName(), "get");
                return (Supplier<Object>) () -> lambda.call();
            } else if (functionalInterfaceClass == Function.class) {
                checkParameterCount(lambda, 1, Function.class.getName(), "apply");
                return (Function<Object, Object>) (arg) -> lambda.call(arg);
            } else if (functionalInterfaceClass == Consumer.class) {
                checkParameterCount(lambda, 1, Consumer.class.getName(), "accept");
                return (Consumer<Object>) (arg) -> lambda.call(arg);
            } else if (functionalInterfaceClass == Predicate.class) {
                checkParameterCount(lambda, 1, Predicate.class.getName(), "test");
                return (Predicate<Object>) (arg) -> (Boolean) lambda.call(arg);
            }

            // 对于其他函数接口用Proxy
            Method[] methods = functionalInterfaceClass.getMethods();
            Method functionalMethod = null;

            for (Method method : methods) {
                if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (functionalMethod == null) {
                    functionalMethod = method;
                } else {
                    throw new IllegalArgumentException("不是函数接口（有多个抽象方法）: "
                            + functionalInterfaceClass.getName());
                }
            }

            if (functionalMethod == null) {
                throw new IllegalArgumentException("没有找到抽象方法: " + functionalInterfaceClass.getName());
            }

            // 检查参数数量是否匹配
            int expectedParamCount = functionalMethod.getParameterCount();
            checkParameterCount(lambda, expectedParamCount, functionalInterfaceClass.getName(),
                    functionalMethod.getName());

            final Method finalFunctionalMethod = functionalMethod;
            logger.info("当前尝试把Lambda转为" + functionalInterfaceClass.getName()
                    + "，将会识别" + functionalMethod.getName() + "为函数执行接口");
            return Proxy.newProxyInstance(
                    functionalInterfaceClass.getClassLoader(),
                    new Class<?>[] { functionalInterfaceClass },
                    (proxy, method, args) -> {
                        if (method.equals(finalFunctionalMethod)) {
                            return lambda.call(args);
                        } else if (method.getName().equals("toString")) {
                            return lambda.toString();
                        } else if (method.getName().equals("equals")) {
                            return proxy == args[0];
                        } else if (method.getName().equals("hashCode")) {
                            return System.identityHashCode(proxy);
                        } else {
                            throw new UnsupportedOperationException("方法不支持: " + method.getName());
                        }
                    });
        }

        private void checkParameterCount(Lambda lambda, int expectedCount, String interfaceName, String methodName) {
            // 通过LambdaNode获取参数数量
            if (this.parameters.size() != expectedCount) {
                logger.error("Lambda转换失败，表达式需要" + this.parameters.size()
                        + "个参数，但" + interfaceName + "接口的" + methodName + "方法需要" + expectedCount + "个参数");
                throw new RuntimeException("Cannot cast Lambda, parameter count mismatch (" +
                        "expected " + expectedCount + "(for " + interfaceName + "." + methodName + "), got "
                        + this.parameters.size() + ")");
            }
        }

        private Lambda createLambda(ExecutionContext context) {
            return (args) -> {
                try {
                    context.enterScope();
                    Object result = null;
                    if (args == null) {
                        if (parameters.size() > 0) {
                            throw new RuntimeException(
                                    "此Lambda需要 " + parameters.size() + " 个参数，但调用提供了 0 个参数");
                        }
                    } else if (args.length != parameters.size()) {
                        throw new RuntimeException(
                                "This lambda call requires " + parameters.size() + " args, provided " + args.length);
                    } else {
                        for (int i = 0; i < parameters.size(); i++) {
                            context.setVariable(parameters.get(i), args[i]);
                        }
                        result = body.evaluate(context);
                        context.exitScope();
                    }
                    return result;
                } catch (Exception e) {
                    throw new RuntimeException("Lambda execution failed: " + e.getMessage(), e);
                }
            };
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            if (functionalInterfaceName == null || functionalInterfaceName.isEmpty()
                    || functionalInterfaceName.equals("Lambda")) {
                return Lambda.class;
            }
            try {
                return context.findClass(functionalInterfaceName);
            } catch (Exception e) {
                return Lambda.class;
            }
        }
    }

    public static class DirectFunctionCallNode extends ASTNode {
        private final ASTNode function;
        private final List<ASTNode> arguments;

        public DirectFunctionCallNode(ASTNode function, List<ASTNode> arguments) {
            this.function = function;
            this.arguments = arguments;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if (function instanceof VariableNode) {
                String methodName = ((VariableNode) function).name;
                if (context.hasVariable("this")) {
                    Object thisValue = context.getVariable("this").value;
                    if (thisValue instanceof CustomClassInstance thisInstance) {
                        ClassDefinition classDef = thisInstance.getClassDefinition();
                        MethodDefinition methodDef = findMethodInHierarchy(classDef, methodName, context);
                        if (methodDef != null) {
                            return new MethodCallNode(new VariableNode("this"), methodName, arguments)
                                    .evaluate(context);
                        }
                    }
                }
            }

            Object funcObj = function.evaluate(context);
            List<Object> args = new ArrayList<>();
            for (ASTNode arg : arguments) {
                args.add(arg.evaluate(context));
            }

            if (funcObj instanceof Lambda) {
                return ((Lambda) funcObj).call(args.toArray());
            } else if (funcObj instanceof Function) {
                return ((Function<Object[], ?>) funcObj).apply(args.toArray());
            } else if (funcObj instanceof Supplier) {
                return ((Supplier<?>) funcObj).get();
            } else if (funcObj instanceof Callable) {
                return ((Callable<?>) funcObj).call();
            } else if (funcObj instanceof Runnable) {
                ((Runnable) funcObj).run();
                return null;
            } else if (funcObj instanceof Method) {
                return ((Method) funcObj).invoke(null, args.toArray());
            } else if (funcObj instanceof Predicate) {
                return ((Predicate<Object>) funcObj).test(args.get(0));
            } else if (funcObj instanceof Consumer) {
                ((Consumer<Object>) funcObj).accept(args.get(0));
                return null;
            } else {
                logger.error("对象" + funcObj + "无法被调用");
                throw new RuntimeException("Object not callable: " + funcObj);
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            Object funcObj = function.evaluate(context);
            if (funcObj instanceof Function) {
                return Object.class;
            } else if (funcObj instanceof Supplier) {
                return Object.class;
            } else if (funcObj instanceof Predicate) {
                return Boolean.class;
            } else if (funcObj instanceof Lambda) {
                return Object.class;
            } else {
                return Void.class;
            }
        }
    }

    public static class DeleteNode extends ASTNode {
        private final String variableName;

        public DeleteNode(String variableName) {
            this.variableName = variableName;
        }

        public Object evaluate(ExecutionContext context) throws Exception {
            if (variableName == "*") {
                for (Map.Entry<String, Variable> entry : context.getAllVariables().entrySet()) {
                    if (!entry.getKey().equals("this")) {
                        Variable val = context.getVariable(entry.getKey());
                        try {
                            if (val != null && val.value.getClass().getMethod("finalize") != null) {
                                context.callMethod(val.value, "finalize", new ArrayList<>());
                            }
                        } catch (NoSuchMethodException e) {
                        }
                        context.deleteVariable(entry.getKey());
                    }
                }

            } else {
                if (variableName.equals("this")) {
                    logger.error("不允许删除this引用");
                    throw new RuntimeException("Cannot delete 'this' reference");
                } else if (isKeyword(variableName)) {
                    logger.error("不允许删除关键字: " + variableName);
                    throw new RuntimeException("Cannot delete keyword: " + variableName);
                }

                Variable val = context.getVariable(variableName);
                // 如果有析构函数(finalize)就调用
                try {
                    if (val.value.getClass().getMethod("finalize") != null) {
                        context.callMethod(val.value, "finalize", new ArrayList<>());
                    }
                } catch (NoSuchMethodException e) {
                }
                context.deleteVariable(variableName);
            }
            return null;
        }

        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }
}
