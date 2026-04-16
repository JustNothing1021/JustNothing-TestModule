package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.api.ClassResolver;
import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.GenericType;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.nodes.*;
import com.justnothing.javainterpreter.exception.ErrorCode;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.exception.ReturnException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
    
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AST求值器
 * <p>
 * 执行AST节点并返回结果
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class ASTEvaluator {

    private static final EvaluatorRegistry defaultRegistry = EvaluatorRegistry.getDefault();
    
    public static Object evaluate(ASTNode node, ExecutionContext context) throws EvaluationException {
        return defaultRegistry.evaluate(node, context);
    }

    public static Object evaluateBlock(BlockNode node, ExecutionContext context) throws EvaluationException {
        Object result = null;
        boolean isGlobalScope = context.getScopeManager().getCurrentLevel() == 1;
        
        // 首先编译所有的类声明，确保它们都已经注册到 context 中
        List<ASTNode> nonClassStatements = new ArrayList<>();
        for (ASTNode stmt : node.getStatements()) {
            if (stmt instanceof ClassDeclarationNode) {
                evaluateClassDeclaration((ClassDeclarationNode) stmt, context);
            } else {
                nonClassStatements.add(stmt);
            }
        }
        
        if (!isGlobalScope) {
            context.getScopeManager().enterScope();
        }
        
        try {
            for (ASTNode stmt : nonClassStatements) {
                result = evaluate(stmt, context);
            }
        } catch (ReturnException e) {
            if (isGlobalScope) {
                result = e.getValue();
            } else {
                throw e;
            }
        } finally {
            if (!isGlobalScope) {
                context.getScopeManager().exitScope();
            }
        }
        return result;
    }
    
    public static Object evaluateLiteral(LiteralNode node) {
        return node.getValue();
    }
    
    public static Object evaluateInterpolatedString(InterpolatedStringNode node, ExecutionContext context) throws EvaluationException {
        StringBuilder sb = new StringBuilder();
        for (InterpolatedStringNode.Part part : node.getParts()) {
            if (part.isExpression()) {
                Object value = evaluate(part.getExpression(), context);
                sb.append(value != null ? value.toString() : "null");
            } else {
                sb.append(part.getLiteralText());
            }
        }
        return sb.toString();
    }
    
    public static Object evaluateVariable(VariableNode node, ExecutionContext context) throws EvaluationException {
        String varName = node.getName();
        try {
            ScopeManager.Variable variable = context.getScopeManager().getVariable(varName);
            return variable.getValue();
        } catch (EvaluationException e) {
            // 尝试从 this 对象中解析字段
            if (!varName.equals("this")) {
                try {
                    ScopeManager.Variable thisVar = context.getScopeManager().getVariable("this");
                    if (thisVar != null) {
                        Object instance = thisVar.getValue();
                        if (instance != null) {
                            try {
                                java.lang.reflect.Field field = findField(instance.getClass(), varName);
                                if (field != null) {
                                    field.setAccessible(true);
                                    return field.get(instance);
                                }
                            } catch (Exception ex) {
                                // 字段访问失败，继续抛出未定义变量异常
                            }
                        }
                    }
                } catch (EvaluationException ex) {
                    // this 变量也不存在，继续抛出未定义变量异常
                }
            }
            throw e;
        }
    }
    
    private static java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        try {
            // 首先尝试在当前类中查找字段
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            return field;
        } catch (NoSuchFieldException e) {
            // 如果当前类没有该字段，尝试在父类中查找
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return findField(superClass, fieldName);
            }
            return null;
        }
    }
    
    public static Object evaluateBinaryOp(BinaryOpNode node, ExecutionContext context) throws EvaluationException {
        Object left = evaluate(node.getLeft(), context);
        Object right = evaluate(node.getRight(), context);
        BinaryOpNode.Operator op = node.getOperator();
        
        switch (op) {
            case ADD:
                if (left instanceof String || right instanceof String) {
                    // 拼接字符串
                    return formatValue(left) + formatValue(right);
                }
                if (left instanceof Number && right instanceof Number) {
                    // 单纯的数字运算
                    return addNumbers((Number) left, (Number) right);
                }
                if (left instanceof Object[] leftArr && right instanceof Object[] rightArr) {
                    // 合并数组
                    Object[] result = new Object[leftArr.length + rightArr.length];
                    System.arraycopy(leftArr, 0, result, 0, leftArr.length);
                    System.arraycopy(rightArr, 0, result, leftArr.length, rightArr.length);
                    return result;
                }
                break;
            case SUBTRACT:
                if (left instanceof Number && right instanceof Number) {
                    return subtractNumbers((Number) left, (Number) right);
                }
                if (left instanceof Object[] && right instanceof Object[]) {
                    return arrayDifference((Object[]) left, (Object[]) right);
                }
                break;
            case MULTIPLY:
                if (left instanceof Number && right instanceof Number) {
                    // 单纯的乘法
                    return multiplyNumbers((Number) left, (Number) right);
                } else if (left instanceof String && right instanceof Number) {
                    // 字符串重复
                    return ((String) left).repeat((int) right);
                } else if (left instanceof Object[] && right instanceof Number) {
                    // 数组重复
                    Object[] newArr = new Object[(int) (Array.getLength(left) * (int) right)];
                    for (int i = 0; i < (int) right; i++) {
                        for (int j = 0; j < Array.getLength(left); j++) {
                            newArr[i * Array.getLength(left) + j] = Array.get(left, j);
                        }
                    }
                    return newArr;
                }
                break;
            case DIVIDE:
                if (left instanceof Number && right instanceof Number) {
                    return divideNumbers((Number) left, (Number) right);
                }
                break;
            case MODULO:
                if (left instanceof Number && right instanceof Number) {
                    return moduloNumbers((Number) left, (Number) right);
                }
                break;
            case POWER:
                if (left instanceof Number && right instanceof Number) {
                    double result = Math.pow(((Number) left).doubleValue(), ((Number) right).doubleValue());
                    if (isFloatingType((Number) left) || isFloatingType((Number) right)) {
                        return result;
                    }
                    if (result == Math.floor(result) && result <= Long.MAX_VALUE && result >= Long.MIN_VALUE) {
                        return (long) result;
                    }
                    return result;
                }
                if (left instanceof Object[] && right instanceof Object[]) {
                    return cartesianProduct((Object[]) left, (Object[]) right);
                }
                break;
            case INT_DIVIDE:
                if (left instanceof Number && right instanceof Number) {
                    double a = ((Number) left).doubleValue();
                    double b = ((Number) right).doubleValue();
                    return (long) Math.floor(a / b);
                }
                break;
            case MATH_MODULO:
                if (left instanceof Number && right instanceof Number) {
                    double a = ((Number) left).doubleValue();
                    double b = ((Number) right).doubleValue();
                    return a - b * Math.floor(a / b);
                }
                break;
            case RANGE:
                return createRange(left, right, false);
            case RANGE_EXCLUSIVE:
                return createRange(left, right, true);
            case EQUAL:
                return Objects.equals(left, right);
            case NOT_EQUAL:
                return !Objects.equals(left, right);
            case LESS_THAN:
                if (left instanceof Number && right instanceof Number) {
                    return compareNumbers((Number) left, (Number) right) < 0;
                }
                break;
            case LESS_THAN_OR_EQUAL:
                if (left instanceof Number && right instanceof Number) {
                    return compareNumbers((Number) left, (Number) right) <= 0;
                }
                break;
            case GREATER_THAN:
                if (left instanceof Number && right instanceof Number) {
                    return compareNumbers((Number) left, (Number) right) > 0;
                }
                break;
            case GREATER_THAN_OR_EQUAL:
                if (left instanceof Number && right instanceof Number) {
                    return compareNumbers((Number) left, (Number) right) >= 0;
                }
                break;
            case SPACESHIP:
                if (left instanceof Number && right instanceof Number) {
                    return compareNumbers((Number) left, (Number) right);
                }
                if (left instanceof Comparable && right != null) {
                    return ((Comparable) left).compareTo(right);
                }
                break;
            case LOGICAL_AND:
                return toBoolean(left) && toBoolean(right);
            case LOGICAL_OR:
                return toBoolean(left) || toBoolean(right);
            case BITWISE_AND:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() & ((Number) right).longValue();
                }
                if (left instanceof Object[] && right instanceof Object[]) {
                    return arrayIntersection((Object[]) left, (Object[]) right);
                }
                break;
            case BITWISE_OR:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() | ((Number) right).longValue();
                }
                if (left instanceof Object[] && right instanceof Object[]) {
                    return arrayUnion((Object[]) left, (Object[]) right);
                }
                break;
            case BITWISE_XOR:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() ^ ((Number) right).longValue();
                }
                if (left instanceof Object[] && right instanceof Object[]) {
                    return arraySymmetricDifference((Object[]) left, (Object[]) right);
                }
                break;
            case LEFT_SHIFT:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() << ((Number) right).intValue();
                }
                break;
            case RIGHT_SHIFT:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() >> ((Number) right).intValue();
                }
                break;
            case UNSIGNED_RIGHT_SHIFT:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() >>> ((Number) right).intValue();
                }
                break;
            case NULL_COALESCING:
                return left != null ? left : right;
            case ELVIS:
                if (left == null) {
                    return right;
                }
                if (left instanceof Boolean) {
                    return (Boolean) left ? true : right;
                }
                if (left instanceof String && ((String) left).isEmpty()) {
                    return right;
                }
                return left;
            default:
                break;
        }
        
        throw new EvaluationException(
            "Invalid binary operation: " + op.getSymbol() + " on " + 
            (left != null ? left.getClass().getName() : "null") + " and " +
            (right != null ? right.getClass().getName() : "null"),
            node.getLocation(),
            ErrorCode.EVAL_INVALID_OPERATION
        );
    }
    

    private static boolean isFloatingType(Number n) {
        return n instanceof Double || n instanceof Float;
    }
    
    private static Object createRange(Object start, Object end, boolean exclusive) {
        if (start instanceof Number && end instanceof Number) {
            int startVal = ((Number) start).intValue();
            int endVal = ((Number) end).intValue();
            int actualEnd = exclusive ? endVal - 1 : endVal;
            
            if (startVal <= actualEnd) {
                int size = actualEnd - startVal + 1;
                Object[] result = new Object[size];
                for (int i = 0; i < size; i++) {
                    result[i] = startVal + i;
                }
                return result;
            } else {
                int size = startVal - actualEnd + 1;
                Object[] result = new Object[size];
                for (int i = 0; i < size; i++) {
                    result[i] = startVal - i;
                }
                return result;
            }
        }
        
        if (start instanceof Character && end instanceof Character) {
            char startChar = (Character) start;
            char endChar = (Character) end;
            char actualEnd = exclusive ? (char)(endChar - 1) : endChar;
            
            if (startChar <= actualEnd) {
                int size = actualEnd - startChar + 1;
                Object[] result = new Object[size];
                for (int i = 0; i < size; i++) {
                    result[i] = (char)(startChar + i);
                }
                return result;
            } else {
                int size = startChar - actualEnd + 1;
                Object[] result = new Object[size];
                for (int i = 0; i < size; i++) {
                    result[i] = (char)(startChar - i);
                }
                return result;
            }
        }
        
        throw new EvaluationException(
            "Range operator requires numeric or character operands",
            null,
            ErrorCode.EVAL_TYPE_MISMATCH
        );
    }
    
    private static Object[] cartesianProduct(Object[] arr1, Object[] arr2) {
        int len1 = arr1.length;
        int len2 = arr2.length;
        Object[] result = new Object[len1 * len2];
        
        int index = 0;
        for (Object o : arr1) {
            for (Object object : arr2) {
                Object[] pair = new Object[2];
                pair[0] = o;
                pair[1] = object;
                result[index++] = pair;
            }
        }
        
        return result;
    }
    
    private static Number addNumbers(Number a, Number b) {
        if (isFloatingType(a) || isFloatingType(b)) {
            return a.doubleValue() + b.doubleValue();
        }
        if (a instanceof Long || b instanceof Long) {
            return a.longValue() + b.longValue();
        }
        return a.intValue() + b.intValue();
    }
    
    private static Number subtractNumbers(Number a, Number b) {
        if (isFloatingType(a) || isFloatingType(b)) {
            return a.doubleValue() - b.doubleValue();
        }
        if (a instanceof Long || b instanceof Long) {
            return a.longValue() - b.longValue();
        }
        return a.intValue() - b.intValue();
    }
    
    private static Number multiplyNumbers(Number a, Number b) {
        if (isFloatingType(a) || isFloatingType(b)) {
            return a.doubleValue() * b.doubleValue();
        }
        if (a instanceof Long || b instanceof Long) {
            return a.longValue() * b.longValue();
        }
        return a.intValue() * b.intValue();
    }
    
    private static Number divideNumbers(Number a, Number b) {
        if (isFloatingType(a) || isFloatingType(b)) {
            return a.doubleValue() / b.doubleValue();
        }
        if (a instanceof Long || b instanceof Long) {
            return a.longValue() / b.longValue();
        }
        return a.intValue() / b.intValue();
    }
    
    private static Number moduloNumbers(Number a, Number b) {
        if (isFloatingType(a) || isFloatingType(b)) {
            return a.doubleValue() % b.doubleValue();
        }
        if (a instanceof Long || b instanceof Long) {
            return a.longValue() % b.longValue();
        }
        return a.intValue() % b.intValue();
    }
    
    private static int compareNumbers(Number a, Number b) {
        return Double.compare(a.doubleValue(), b.doubleValue());
    }
    
    public static Object evaluateUnaryOp(UnaryOpNode node, ExecutionContext context) throws EvaluationException {
        UnaryOpNode.Operator op = node.getOperator();
        
        switch (op) {
            case NEGATIVE: {
                Object operand = evaluate(node.getOperand(), context);
                if (operand instanceof Number n) {
                    if (isFloatingType(n)) {
                        return -n.doubleValue();
                    }
                    if (n instanceof Long) {
                        return -n.longValue();
                    }
                    return -n.intValue();
                }
                break;
            }
            case LOGICAL_NOT: {
                Object operand = evaluate(node.getOperand(), context);
                return !toBoolean(operand);
            }
            case NOT_NULL: {
                Object operand = evaluate(node.getOperand(), context);
                if (operand == null) {
                    throw new EvaluationException(
                        "Non-null assertion failed: value is null",
                        node.getLocation(),
                        ErrorCode.EVAL_NULL_POINTER
                    );
                }
                // 其实双重否定都不用管了, 因为直接返回原来的值
                return operand;
            }
            case BITWISE_NOT: {
                Object operand = evaluate(node.getOperand(), context);
                if (operand instanceof Number n) {
                    if (n instanceof Long) {
                        return ~n.longValue();
                    }
                    return ~n.intValue();
                }
                if (operand instanceof Object[]) {
                    return arrayReverse((Object[]) operand);
                }
                break;
            }
            case PRE_INCREMENT:
            case PRE_DECREMENT:
            case POST_INCREMENT:
            case POST_DECREMENT: {
                return evaluateIncrementDecrement(node, context);
            }
            default:
                break;
        }
        
        throw new EvaluationException(
            "Invalid unary operation: " + op.getSymbol(),
            node.getLocation(),
            ErrorCode.EVAL_INVALID_OPERATION
        );
    }
    
    private static Object evaluateIncrementDecrement(UnaryOpNode node, ExecutionContext context) throws EvaluationException {
        UnaryOpNode.Operator op = node.getOperator();
        ASTNode operand = node.getOperand();
        
        if (!(operand instanceof VariableNode)) {
            throw new EvaluationException(
                "Increment/decrement operator requires a variable",
                node.getLocation(),
                ErrorCode.EVAL_INVALID_OPERATION
            );
        }
        
        String varName = ((VariableNode) operand).getName();
        Object currentValue = context.getScopeManager().getVariable(varName).getValue();
        
        if (!(currentValue instanceof Number numValue)) {
            throw new EvaluationException(
                "Increment/decrement operator requires a numeric variable",
                node.getLocation(),
                ErrorCode.EVAL_INVALID_OPERATION
            );
        }

        Number newValue;
        
        if (op == UnaryOpNode.Operator.PRE_INCREMENT || op == UnaryOpNode.Operator.POST_INCREMENT) {
            if (currentValue instanceof Integer) {
                newValue = numValue.intValue() + 1;
            } else if (currentValue instanceof Long) {
                newValue = numValue.longValue() + 1;
            } else if (currentValue instanceof Double) {
                newValue = numValue.doubleValue() + 1;
            } else if (currentValue instanceof Float) {
                newValue = numValue.floatValue() + 1;
            } else {
                newValue = numValue.intValue() + 1;
            }
        } else {
            if (currentValue instanceof Integer) {
                newValue = numValue.intValue() - 1;
            } else if (currentValue instanceof Long) {
                newValue = numValue.longValue() - 1;
            } else if (currentValue instanceof Double) {
                newValue = numValue.doubleValue() - 1;
            } else if (currentValue instanceof Float) {
                newValue = numValue.floatValue() - 1;
            } else {
                newValue = numValue.intValue() - 1;
            }
        }
        
        context.getScopeManager().setVariable(varName, newValue);
        
        if (op == UnaryOpNode.Operator.PRE_INCREMENT || op == UnaryOpNode.Operator.PRE_DECREMENT) {
            return newValue;
        } else {
            return currentValue;
        }
    }
    
    public static Object evaluateAssignment(AssignmentNode node, ExecutionContext context) throws EvaluationException {
        ASTNode valueNode = node.getValue();
        
        if (node.isDeclaration() && valueNode instanceof LambdaNode && node.getDeclaredClass() != null) {
            Class<?> declaredClass = node.getDeclaredClass();
            if (declaredClass.isInterface()) {
                ((LambdaNode) valueNode).setFunctionalInterfaceType(declaredClass);
            }
        }
        
        if (node.isDeclaration() && valueNode instanceof MethodReferenceNode && node.getDeclaredClass() != null) {
            Class<?> declaredClass = node.getDeclaredClass();
            if (declaredClass.isInterface()) {
                // TODO
            }
        }
        
        Object value;
        if (valueNode != null) {
            if (node.isDeclaration() && valueNode instanceof LambdaNode) {
                Class<?> declaredType = node.getDeclaredClass() != null ? node.getDeclaredClass() : Object.class;
                if (AutoClass.isAutoType(declaredType)) {
                    declaredType = Object.class;
                }
                context.getScopeManager().declareVariable(
                    node.getVariableName(), 
                    declaredType,
                    null,
                    node.isFinal()
                );
                value = evaluate(valueNode, context);
                context.getScopeManager().setVariable(node.getVariableName(), value);
                return value;
            }
            if (node.isDeclaration() && valueNode instanceof MethodReferenceNode) {
                Class<?> declaredType = node.getDeclaredClass() != null ? node.getDeclaredClass() : Object.class;
                if (AutoClass.isAutoType(declaredType)) {
                    declaredType = Object.class;
                }
                context.getScopeManager().declareVariable(
                    node.getVariableName(), 
                    declaredType,
                    null,
                    node.isFinal()
                );
                value = evaluate(valueNode, context);
                if (declaredType.isInterface() && value instanceof MethodReference) {
                    value = ((MethodReference) value).asInterface(declaredType);
                }
                context.getScopeManager().setVariable(node.getVariableName(), value);
                return value;
            }
            value = evaluate(valueNode, context);
        } else if (node.isDeclaration()) {
            value = getDefaultValue(node.getDeclaredClass());
        } else {
            value = null;
        }
        
        if (node.isDeclaration()) {
            Class<?> declaredType = node.getDeclaredClass() != null ? node.getDeclaredClass() : Object.class;
            
            if (AutoClass.isAutoType(declaredType)) {
                declaredType = AutoClass.inferStrictType(value);
            }
            
            value = convertValue(value, declaredType, node.getLocation());
            
            context.getScopeManager().declareVariable(
                node.getVariableName(), 
                declaredType,
                value, 
                node.isFinal()
            );
        } else {
            ScopeManager.Variable variable = context.getScopeManager().getVariable(node.getVariableName());
            if (variable != null && variable.getType() != null) {
                value = convertValue(value, variable.getType(), node.getLocation());
            }
            context.getScopeManager().setVariable(node.getVariableName(), value);
        }
        
        return value;
    }
    
    public static Object evaluateConditionalAssign(ConditionalAssignNode node, ExecutionContext context) throws EvaluationException {
        String varName = node.getVariableName();
        
        if (!context.getScopeManager().hasVariable(varName)) {
            Object value = evaluate(node.getValue(), context);
            context.getScopeManager().declareVariable(varName, Object.class, value, false);
            return value;
        }
        
        ScopeManager.Variable existing = context.getScopeManager().getVariable(varName);
        if (existing.getValue() == null) {
            Object value = evaluate(node.getValue(), context);
            context.getScopeManager().setVariable(varName, value);
            return value;
        }
        
        return existing.getValue();
    }
    
    public static Object evaluateNullCoalescingAssign(NullCoalescingAssignNode node, ExecutionContext context) throws EvaluationException {
        String varName = node.getVariableName();
        
        if (!context.getScopeManager().hasVariable(varName)) {
            Object value = evaluate(node.getValue(), context);
            context.getScopeManager().declareVariable(varName, Object.class, value, false);
            return value;
        }
        
        ScopeManager.Variable existing = context.getScopeManager().getVariable(varName);
        if (existing.getValue() == null) {
            Object value = evaluate(node.getValue(), context);
            context.getScopeManager().setVariable(varName, value);
            return value;
        }
        
        return existing.getValue();
    }
    
    private static Object convertValue(Object value, Class<?> targetType, SourceLocation location) throws EvaluationException {
        if (value == null) {
            if (targetType.isPrimitive()) {
                throw new EvaluationException(
                    "Cannot assign null to primitive type: " + targetType.getName(),
                    location,
                    ErrorCode.EVAL_TYPE_MISMATCH
                );
            }
            return null;
        }
        
        if (targetType == null || targetType == Object.class) {
            return value;
        }
        
        Class<?> sourceType = value.getClass();
        
        if (targetType.isAssignableFrom(sourceType)) {
            return value;
        }
        
        if (targetType.isPrimitive() && sourceType.isAssignableFrom(getWrapperType(targetType))) {
            return value;
        }
        
        if (value instanceof Number num) {

            if (targetType == int.class || targetType == Integer.class) {
                return num.intValue();
            }
            if (targetType == long.class || targetType == Long.class) {
                return num.longValue();
            }
            if (targetType == float.class || targetType == Float.class) {
                return num.floatValue();
            }
            if (targetType == double.class || targetType == Double.class) {
                return num.doubleValue();
            }
            if (targetType == byte.class || targetType == Byte.class) {
                return num.byteValue();
            }
            if (targetType == short.class || targetType == Short.class) {
                return num.shortValue();
            }
        }
        
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
        }
        
        if (targetType == char.class || targetType == Character.class) {
            if (value instanceof Character) {
                return value;
            }
            if (value instanceof Number) {
                return (char) ((Number) value).intValue();
            }
        }
        
        throw new EvaluationException(
            "Cannot convert " + sourceType.getName() + " to " + targetType.getName(),
            location,
            ErrorCode.EVAL_TYPE_MISMATCH
        );
    }
    
    private static Object getDefaultValue(Class<?> type) {
        if (type == null) return null;
        
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        
        return null;
    }
    
    public static Object evaluateFieldAssignment(FieldAssignmentNode node, ExecutionContext context) throws EvaluationException {
        Object target = evaluate(node.getTarget(), context);
        Object value = evaluate(node.getValue(), context);
        
        try {
            Field field;
            if (target instanceof Class) {
                // 处理静态字段赋值
                Class<?> targetClass = (Class<?>) target;
                field = findField(targetClass, node.getFieldName());
                if (field == null) {
                    throw new EvaluationException(
                        "Field not found: " + node.getFieldName(),
                        node.getLocation(),
                        ErrorCode.EVAL_FIELD_ACCESS_FAILED
                    );
                }
                field.setAccessible(true);
                field.set(null, value);
            } else {
                // 处理实例字段赋值
                field = findField(target.getClass(), node.getFieldName());
                if (field == null) {
                    throw new EvaluationException(
                        "Field not found: " + node.getFieldName(),
                        node.getLocation(),
                        ErrorCode.EVAL_FIELD_ACCESS_FAILED
                    );
                }
                field.setAccessible(true);
                field.set(target, value);
            }
            return value;
        } catch (EvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to set field: " + node.getFieldName() + " - " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_FIELD_ACCESS_FAILED
            );
        }
    }
    
    public static Object evaluateArrayAssignment(ArrayAssignmentNode node, ExecutionContext context) throws EvaluationException {
        Object array = evaluate(node.getArray(), context);
        Object index = evaluate(node.getIndex(), context);
        Object value = evaluate(node.getValue(), context);
        
        int idx = toInt(index);
        Array.set(array, idx, value);
        return value;
    }
    
    public static Object evaluateFunctionCall(FunctionCallNode node, ExecutionContext context) throws EvaluationException {
        String functionName = node.getFunctionName();
        
        // 处理 super() 构造函数调用
        if (functionName.equals("super")) {
            // 构造函数中的 super() 调用不需要实际执行，因为我们已经在字节码中生成了对父类构造函数的调用
            return null;
        }
        
        Object[] args = new Object[node.getArguments().size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = evaluate(node.getArguments().get(i), context);
        }
        
        if (context.hasBuiltin(functionName)) {
            List<Object> argList = new ArrayList<>(Arrays.asList(args));
            return context.callBuiltin(functionName, argList);
        }
        
        if (context.getScopeManager().hasVariable(functionName)) {
            Object func = context.getScopeManager().getVariable(functionName).getValue();
            if (func instanceof Lambda) {
                return ((Lambda) func).invoke(args);
            }
            if (func instanceof MethodReference) {
                return ((MethodReference) func).invoke(args);
            }
            if (func instanceof Function) {
                return ((Function) func).apply(args.length == 1 ? args[0] : args);
            }
            if (func instanceof Supplier) {
                return ((Supplier<?>) func).get();
            }
            if (func instanceof Consumer) {
                ((Consumer) func).accept(args.length > 0 ? args[0] : null);
                return null;
            }
            if (func instanceof BiFunction) {
                return ((BiFunction) func).apply(args.length > 0 ? args[0] : null, args.length > 1 ? args[1] : null);
            }
            if (func != null && Proxy.isProxyClass(func.getClass())) {
                try {
                    InvocationHandler handler = Proxy.getInvocationHandler(func);
                    if (handler instanceof Lambda.LambdaInvocationHandler) {
                        Lambda lambda = ((Lambda.LambdaInvocationHandler) handler).getLambda();
                        return lambda.invoke(args);
                    }
                    if (handler instanceof MethodReference.MethodReferenceInvocationHandler) {
                        MethodReference mr = ((MethodReference.MethodReferenceInvocationHandler) handler).getMethodReference();
                        return mr.invoke(args);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        
        Object currentInstance = MethodBodyExecutor.getCurrentInstanceContext();
        if (currentInstance != null) {
            try {
                String instanceClassName = currentInstance.getClass().getSimpleName();
                return MethodBodyExecutor.executeMethod(instanceClassName, functionName, currentInstance, args);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("Method not found")) {
                    try {
                        java.lang.reflect.Method method = findMethod(currentInstance.getClass(), functionName, args);
                        if (method != null) {
                            method.setAccessible(true);
                            return method.invoke(currentInstance, args);
                        }
                    } catch (Exception ex) {
                        // fall through
                    }
                } else {
                    throw e;
                }
            }
        }
        
        // 检查当前类的静态方法
        String currentClass = MethodBodyExecutor.getCurrentClassContext();
        if (currentClass != null) {
            try {
                return MethodBodyExecutor.executeMethod(currentClass, functionName, null, args);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("Method not found")) {
                    Class<?> currentClassObj = ClassResolver.findClassWithImports(
                        currentClass, context.getClassLoader(), context.getImports());
                    if (currentClassObj != null) {
                        java.lang.reflect.Method method = findMethod(currentClassObj, functionName, args);
                        if (method != null && Modifier.isStatic(method.getModifiers())) {
                            try {
                                method.setAccessible(true);
                                return method.invoke(null, args);
                            } catch (Exception ex) {
                            }
                        }
                    }
                } else {
                    throw e;
                }
            }
        }
        
        throw new EvaluationException(
            "Unknown function: " + functionName,
            node.getLocation(),
            ErrorCode.EVAL_UNDEFINED_VARIABLE
        );
    }
    

    
    public static Object evaluateMethodCall(MethodCallNode node, ExecutionContext context) throws EvaluationException {
        Object target = evaluate(node.getTarget(), context);
        String methodName = node.getMethodName();

        // 修复：当 methodName 为 null 时，尝试从 FieldAccessNode 中获取
        if (methodName == null && node.getTarget() instanceof FieldAccessNode fieldAccess) {
            methodName = fieldAccess.getFieldName();
        }

        // 确保 methodName 不为 null
        if (methodName == null) {
            throw new EvaluationException(
                "Method name not found",
                node.getLocation(),
                ErrorCode.METHOD_NOT_FOUND
            );
        }

        Object[] args = new Object[node.getArguments().size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = evaluate(node.getArguments().get(i), context);
        }

        try {
            if (target instanceof Lambda) {
                return ((Lambda) target).invoke(args);
            }

            if (target instanceof MethodReference) {
                return ((MethodReference) target).invoke(args);
            }

            Class<?> targetClass;
            Object targetInstance;

            if (target instanceof Class<?> targetClassObj) {
                Method classMethod = findMethod(Class.class, methodName, args);
                if (classMethod != null) {
                    classMethod.setAccessible(true);
                    Object[] invokeArgs = prepareInvokeArguments(classMethod, args);
                    return classMethod.invoke(targetClassObj, invokeArgs);
                }

                targetClass = targetClassObj;
                targetInstance = null;
            } else {
                targetClass = target.getClass();
                targetInstance = target;
            }

            context.checkMethodAccess(targetClass.getName(), methodName, null);

            Method method = findMethod(targetClass, methodName, args);
            if (method != null) {
                try {
                    method.setAccessible(true);
                } catch (Exception ignored) {
                }
                Object[] invokeArgs = prepareInvokeArguments(method, args);
                return method.invoke(targetInstance, invokeArgs);
            }

            if (target instanceof Class<?> targetClassObj) {
                try {
                    // 尝试通过 MethodBodyExecutor 执行动态类的方法
                    String className = targetClassObj.getName();
                    String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
                    return MethodBodyExecutor.executeMethod(simpleClassName, methodName, null, args);
                } catch (Exception e) {
                    // 如果失败，继续尝试其他方法
                }
            }

            throw new EvaluationException(
                "Method not found: " + methodName,
                node.getLocation(),
                ErrorCode.METHOD_NOT_FOUND
            );
        } catch (EvaluationException e) {
            throw e;
        } catch (SecurityException e) {
            throw new EvaluationException(
                "Permission denied for method call: " + methodName + " - " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_PERMISSION_DENIED,
                e
            );
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new EvaluationException(
                "Failed to call method: " + methodName + " - " + cause.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_METHOD_INVOCATION_FAILED,
                cause
            );
        }
    }
    
    public static Object evaluateSafeMethodCall(SafeMethodCallNode node, ExecutionContext context) throws EvaluationException {
        Object target = evaluate(node.getTarget(), context);
        
        if (target == null) {
            return null;
        }
        
        String methodName = node.getMethodName();
        
        Object[] args = new Object[node.getArguments().size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = evaluate(node.getArguments().get(i), context);
        }
        
        try {
            if (target instanceof Lambda) {
                return ((Lambda) target).invoke(args);
            }
            
            if (target instanceof MethodReference) {
                return ((MethodReference) target).invoke(args);
            }
            
            Class<?> targetClass;
            Object targetInstance;
            
            if (target instanceof Class<?> targetClassObj) {
                Method classMethod = findMethod(Class.class, methodName, args);
                if (classMethod != null) {
                    classMethod.setAccessible(true);
                    Object[] invokeArgs = prepareInvokeArguments(classMethod, args);
                    return classMethod.invoke(targetClassObj, invokeArgs);
                }
                
                targetClass = targetClassObj;
                targetInstance = null;
            } else {
                targetClass = target.getClass();
                targetInstance = target;
            }
            
            Method method = findMethod(targetClass, methodName, args);
            if (method != null) {
                method.setAccessible(true);
                Object[] invokeArgs = prepareInvokeArguments(method, args);
                return method.invoke(targetInstance, invokeArgs);
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static Object[] prepareInvokeArguments(Method method, Object[] args) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        if (!method.isVarArgs() || paramTypes.length == 0) {
            return args;
        }
        
        int fixedParamCount = paramTypes.length - 1;
        Class<?> varArgsType = paramTypes[fixedParamCount];
        Class<?> varArgsComponentType = varArgsType.getComponentType();
        
        if (args.length < fixedParamCount) {
            return args;
        }
        
        Object[] invokeArgs = new Object[paramTypes.length];

        System.arraycopy(args, 0, invokeArgs, 0, fixedParamCount);
        
        int varArgCount = args.length - fixedParamCount;
        
        if (varArgCount == 1) {
            Object lastArg = args[fixedParamCount];
            
            if (lastArg != null && lastArg.getClass().isArray() &&
                    Objects.requireNonNull(varArgsComponentType).isAssignableFrom(Objects.requireNonNull(lastArg.getClass().getComponentType()))) {
                invokeArgs[fixedParamCount] = lastArg;
            } else {
                assert varArgsComponentType != null;
                Object varArgArray = Array.newInstance(varArgsComponentType, 1);
                Array.set(varArgArray, 0, lastArg);
                invokeArgs[fixedParamCount] = varArgArray;
            }
        } else if (varArgCount == 0) {
            assert varArgsComponentType != null;
            invokeArgs[fixedParamCount] = Array.newInstance(varArgsComponentType, 0);
        } else {
            assert varArgsComponentType != null;
            Object varArgArray = Array.newInstance(varArgsComponentType, varArgCount);
            for (int i = 0; i < varArgCount; i++) {
                Array.set(varArgArray, i, args[fixedParamCount + i]);
            }
            invokeArgs[fixedParamCount] = varArgArray;
        }
        
        return invokeArgs;
    }
    
    public static Object evaluateSuperMethodCall(SuperMethodCallNode node, ExecutionContext context) throws EvaluationException {
        try {
            Object thisInstance = context.getScopeManager().getVariable("this").getValue();
            if (thisInstance == null) {
                throw new EvaluationException(
                    "Cannot call super method: 'this' is null",
                    node.getLocation(),
                    ErrorCode.EVAL_INVALID_OPERATION
                );
            }
            
            String methodName = node.getMethodName();
            List<ASTNode> argNodes = node.getArguments();
            
            Object[] args = new Object[argNodes.size()];
            for (int i = 0; i < argNodes.size(); i++) {
                args[i] = evaluate(argNodes.get(i), context);
            }
            
            String currentClass = MethodBodyExecutor.getCurrentClassContext();
            if (currentClass != null) {
                Class<?> currentClazz = null;
                if (context.hasCustomClass(currentClass)) {
                    currentClazz = context.getCustomClass(currentClass);
                } else {
                    currentClazz = ClassResolver.findClassWithImports(
                        currentClass, context.getClassLoader(), context.getImports());
                }
                
                if (currentClazz != null) {
                    Class<?> superClass = currentClazz.getSuperclass();
                    if (superClass != null) {
                        String superClassName = superClass.getSimpleName();
                        try {
                            return MethodBodyExecutor.executeMethod(superClassName, methodName, thisInstance, args);
                        } catch (RuntimeException e) {
                            if (e.getMessage() != null && e.getMessage().startsWith("Method not found")) {
                                Method method = findMethod(superClass, methodName, args);
                                if (method != null) {
                                    method.setAccessible(true);
                                    Object[] invokeArgs = prepareInvokeArguments(method, args);
                                    return method.invoke(thisInstance, invokeArgs);
                                }
                            }
                            throw e;
                        }
                    }
                }
            }
            
            Class<?> superClass = thisInstance.getClass().getSuperclass();
            if (superClass == null) {
                throw new EvaluationException(
                    "Cannot call super method: no superclass",
                    node.getLocation(),
                    ErrorCode.EVAL_INVALID_OPERATION
                );
            }
            
            try {
                String superClassName = superClass.getSimpleName();
                return MethodBodyExecutor.executeMethod(superClassName, methodName, thisInstance, args);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("Method not found")) {
                    Method method = findMethod(superClass, methodName, args);
                    if (method == null) {
                        throw new EvaluationException(
                            "Method not found in superclass: " + methodName,
                            node.getLocation(),
                            ErrorCode.EVAL_METHOD_INVOCATION_FAILED
                        );
                    }
                    method.setAccessible(true);
                    Object[] invokeArgs = prepareInvokeArguments(method, args);
                    return method.invoke(thisInstance, invokeArgs);
                }
                throw e;
            }
            
        } catch (EvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to call super method: " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_INVALID_OPERATION
            );
        }
    }
    
    public static Object evaluateFieldAccess(FieldAccessNode node, ExecutionContext context) throws EvaluationException {
        Object target = evaluate(node.getTarget(), context);
        
        try {
            Class<?> targetClass;
            if (target instanceof Class) {
                targetClass = (Class<?>) target;
            } else {
                targetClass = target.getClass();
            }

            if (node.getFieldName().equals("length") && targetClass.isArray()) {
                return Array.getLength(target);
            }
            
            context.checkFieldAccess(targetClass.getName(), node.getFieldName());
            
            if (target instanceof Class) {
                java.lang.reflect.Field field = MethodBodyExecutor.findField(targetClass, node.getFieldName());
                if (field != null) {
                    field.setAccessible(true);
                    return field.get(null);
                }
                throw new NoSuchFieldException("Field " + node.getFieldName() + " not found in " + targetClass.getName());
            }
            
            Object fieldValue = MethodBodyExecutor.resolveField(node.getFieldName(), target);
            if (fieldValue != null) {
                return fieldValue;
            }
            
            // 如果字段解析失败，抛出异常
            throw new NoSuchFieldException("Field " + node.getFieldName() + " not found in " + targetClass.getName());
        } catch (SecurityException e) {
            throw new EvaluationException(
                "Permission denied for field access: " + node.getFieldName() + " - " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_PERMISSION_DENIED,
                e
            );
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to access field: " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_FIELD_ACCESS_FAILED
            );
        }
    }
    
    public static Object evaluateSafeFieldAccess(SafeFieldAccessNode node, ExecutionContext context) throws EvaluationException {
        Object target = evaluate(node.getTarget(), context);
        
        if (target == null) {
            return null;
        }
        
        try {
            Class<?> targetClass;
            if (target instanceof Class) {
                targetClass = (Class<?>) target;
            } else {
                targetClass = target.getClass();
            }
            
            Field field = targetClass.getDeclaredField(node.getFieldName());
            field.setAccessible(true);
            if (target instanceof Class) {
                return field.get(null);
            }
            return field.get(target);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Object evaluateArrayAccess(ArrayAccessNode node, ExecutionContext context) throws EvaluationException {
        Object array = evaluate(node.getArray(), context);
        Object index = evaluate(node.getIndex(), context);
        
        return Array.get(array, toInt(index));
    }
    
    public static Object evaluateClassReference(ClassReferenceNode node, ExecutionContext context) throws EvaluationException {
        try {
            String className = node.getTypeName();
            
            // 首先尝试从自定义类中查找
            if (context.hasCustomClass(className)) {
                return context.getCustomClass(className);
            }
            
            // 使用 ClassResolver 查找类（支持 imports）
            Class<?> clazz = ClassResolver.findClassWithImports(
                className, context.getClassLoader(), context.getImports());
            if (clazz != null) {
                context.checkClassAccess(clazz.getName());
                return clazz;
            }
            
            throw new EvaluationException(
                "Class not found: " + node.getTypeName(),
                node.getLocation(),
                ErrorCode.EVAL_TYPE_MISMATCH
            );
        } catch (SecurityException e) {
            throw new EvaluationException(
                "Permission denied for class access: " + node.getTypeName() + " - " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_PERMISSION_DENIED,
                e
            );
        } catch (Exception e) {
            throw new EvaluationException(
                "Class not found: " + node.getTypeName(),
                node.getLocation(),
                ErrorCode.EVAL_TYPE_MISMATCH
            );
        }
    }
    
    public static Object evaluateNewArray(NewArrayNode node, ExecutionContext context) throws EvaluationException {
        try {
            Class<?> componentType = node.getElementType();
            Object size = evaluate(node.getSize(), context);
            
            return Array.newInstance(componentType, toInt(size));
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to create array: " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_ARRAY_ACCESS_FAILED
            );
        }
    }
    
    public static Object evaluateConstructorCall(ConstructorCallNode node, ExecutionContext context) throws EvaluationException {
        try {
            GenericType type = node.getType();
            List<ASTNode> argNodes = node.getArguments();
            
            Object[] args = new Object[argNodes.size()];
            Class<?>[] argTypes = new Class<?>[argNodes.size()];
            
            for (int i = 0; i < argNodes.size(); i++) {
                args[i] = evaluate(argNodes.get(i), context);
                argTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            
            Class<?> clazz;
            
            if (node.isAnonymousClass()) {
                ClassDeclarationNode anonymousClass = node.getAnonymousClass();
                DynamicClassGenerator generator = new DynamicClassGenerator(context);
                // 生成类时传递构造函数参数类型
                clazz = generator.generateClass(anonymousClass, argTypes);
                context.registerCustomClass(anonymousClass.getClassName(), clazz);
            } else {
                // 首先尝试使用类名从上下文获取自定义类
                String className = type.getTypeName();
                if (context.hasCustomClass(className)) {
                    clazz = context.getCustomClass(className);
                } else {
                    // 然后尝试使用原始类型名
                    String originalTypeName = type.getOriginalTypeName();
                    if (originalTypeName != null && context.hasCustomClass(originalTypeName)) {
                        clazz = context.getCustomClass(originalTypeName);
                    } else {
                        // 最后使用原始类型
                        clazz = type.getRawType();
                    }
                }
            }
            
            context.checkNewInstance(clazz.getName());
            
            Constructor<?> constructor = findConstructor(clazz, argTypes);
            return constructor.newInstance(args);
        } catch (EvaluationException e) {
            throw e;
        } catch (SecurityException e) {
            throw new EvaluationException(
                "Permission denied for new instance: " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_PERMISSION_DENIED,
                e
            );
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to create instance: " + e.getMessage() + "\n" + java.util.Arrays.toString(e.getStackTrace()),
                node.getLocation(),
                ErrorCode.EVAL_INVALID_OPERATION,
                e
            );
        }
    }
    
    private static Constructor<?> findConstructor(Class<?> clazz, Class<?>[] argTypes) throws NoSuchMethodException {
        // 首先尝试精确匹配
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(argTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignored) {
        }
        
        // 然后尝试公共构造函数
        try {
            Constructor<?> constructor = clazz.getConstructor(argTypes);
            return constructor;
        } catch (NoSuchMethodException ignored) {
        }
        
        // 最后尝试找到最匹配的构造函数
        Constructor<?> bestMatch = null;
        int bestScore = -1;
        
        // 检查所有声明的构造函数
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (isApplicableArgs(paramTypes, argTypes, constructor.isVarArgs())) {
                int score = computeMatchScore(paramTypes, argTypes);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = constructor;
                }
            }
        }
        
        // 如果没有找到，检查公共构造函数
        if (bestMatch == null) {
            for (Constructor<?> constructor : clazz.getConstructors()) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (isApplicableArgs(paramTypes, argTypes, constructor.isVarArgs())) {
                    int score = computeMatchScore(paramTypes, argTypes);
                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = constructor;
                    }
                }
            }
        }
        
        if (bestMatch != null) {
            bestMatch.setAccessible(true);
            return bestMatch;
        }
        
        throw new NoSuchMethodException("No suitable constructor found for arguments: " + java.util.Arrays.toString(argTypes));
    }
    

    
    private static boolean isApplicableArgs(Class<?>[] paramTypes, Class<?>[] argTypes, boolean isVarArgs) {
        if (isVarArgs) {
            if (argTypes.length < paramTypes.length - 1) {
                return false;
            }
            for (int i = 0; i < paramTypes.length - 1; i++) {
                if (!isAssignable(paramTypes[i], argTypes[i])) {
                    return false;
                }
            }
            Class<?> varArgType = paramTypes[paramTypes.length - 1].getComponentType();
            for (int i = paramTypes.length - 1; i < argTypes.length; i++) {
                if (!isAssignable(varArgType, argTypes[i])) {
                    return false;
                }
            }
        } else {
            if (paramTypes.length != argTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!isAssignable(paramTypes[i], argTypes[i])) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isAssignable(Class<?> targetType, Class<?> sourceType) {
        if (sourceType == null) {
            return !targetType.isPrimitive();
        }
        if (targetType.isPrimitive()) {
            if (sourceType.isPrimitive()) {
                return isPrimitiveAssignable(targetType, sourceType);
            }
            // 检查源类型是否是目标类型的包装类
            Class<?> wrapper = getWrapperClass(targetType);
            if (wrapper != null && wrapper.isAssignableFrom(sourceType)) {
                return true;
            }
            // 检查源类型是否是其他基本类型的包装类，并且可以转换为目标类型
            if (sourceType == Integer.class) {
                return isPrimitiveAssignable(targetType, int.class);
            }
            if (sourceType == Long.class) {
                return isPrimitiveAssignable(targetType, long.class);
            }
            if (sourceType == Float.class) {
                return isPrimitiveAssignable(targetType, float.class);
            }
            if (sourceType == Double.class) {
                return isPrimitiveAssignable(targetType, double.class);
            }
            if (sourceType == Byte.class) {
                return isPrimitiveAssignable(targetType, byte.class);
            }
            if (sourceType == Short.class) {
                return isPrimitiveAssignable(targetType, short.class);
            }
            if (sourceType == Character.class) {
                return isPrimitiveAssignable(targetType, char.class);
            }
            if (sourceType == Boolean.class) {
                return isPrimitiveAssignable(targetType, boolean.class);
            }
            return false;
        }
        if (sourceType.isPrimitive()) {
            // 检查目标类型是否是源类型的包装类
            Class<?> wrapper = getWrapperClass(sourceType);
            if (wrapper != null && targetType.isAssignableFrom(wrapper)) {
                return true;
            }
        }
        return targetType.isAssignableFrom(sourceType);
    }
    
    private static boolean isPrimitiveAssignable(Class<?> targetType, Class<?> sourceType) {
        if (targetType == sourceType) {
            return true;
        }
        if (targetType == byte.class) {
            return sourceType == short.class || sourceType == int.class || sourceType == long.class || sourceType == float.class || sourceType == double.class;
        }
        if (targetType == short.class) {
            return sourceType == int.class || sourceType == long.class || sourceType == float.class || sourceType == double.class;
        }
        if (targetType == int.class) {
            return sourceType == long.class || sourceType == float.class || sourceType == double.class;
        }
        if (targetType == long.class) {
            return sourceType == float.class || sourceType == double.class;
        }
        if (targetType == float.class) {
            return sourceType == double.class;
        }
        if (targetType == double.class) {
            return sourceType == byte.class || sourceType == short.class || sourceType == int.class || sourceType == long.class || sourceType == float.class;
        }
        if (targetType == char.class) {
            return sourceType == int.class || sourceType == long.class || sourceType == float.class || sourceType == double.class;
        }
        if (targetType == boolean.class) {
            return false;
        }
        return false;
    }
    
    private static Class<?> getWrapperClass(Class<?> primitiveType) {
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == double.class) return Double.class;
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == char.class) return Character.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == short.class) return Short.class;
        if (primitiveType == void.class) return Void.class;
        return null;
    }
    
    public static Object evaluateArrayLiteral(ArrayLiteralNode node, ExecutionContext context) throws EvaluationException {
        List<ASTNode> elements = node.getElements();
        Class<?> expectedType = node.getExpectedElementType();
        
        if (expectedType != null) {
            Class<?> componentType = expectedType;
            Object array = java.lang.reflect.Array.newInstance(componentType, elements.size());
            
            for (int i = 0; i < elements.size(); i++) {
                Object value = evaluate(elements.get(i), context);
                
                if (value != null && !componentType.isAssignableFrom(value.getClass())) {
                    if (componentType.isPrimitive()) {
                        value = convertToPrimitive(value, componentType);
                    } else {
                        throw new EvaluationException(
                            "Type mismatch in array initializer: expected " + componentType.getSimpleName() + 
                            ", but got " + value.getClass().getSimpleName() + " at index " + i,
                            node.getLocation().getLine(),
                            node.getLocation().getColumn(),
                            ErrorCode.EVAL_TYPE_MISMATCH
                        );
                    }
                }
                
                java.lang.reflect.Array.set(array, i, value);
            }
            
            return array;
        }
        
        Object[] array = new Object[elements.size()];
        
        for (int i = 0; i < elements.size(); i++) {
            array[i] = evaluate(elements.get(i), context);
        }
        
        return array;
    }
    
    private static Object convertToPrimitive(Object value, Class<?> targetType) throws EvaluationException {
        if (value == null) {
            return getDefaultValue(targetType);
        }
        
        if (targetType == int.class) {
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof Character) return (int) (Character) value;
        } else if (targetType == long.class) {
            if (value instanceof Number) return ((Number) value).longValue();
            if (value instanceof Character) return (long) (Character) value;
        } else if (targetType == double.class) {
            if (value instanceof Number) return ((Number) value).doubleValue();
            if (value instanceof Character) return (double) (Character) value;
        } else if (targetType == float.class) {
            if (value instanceof Number) return ((Number) value).floatValue();
            if (value instanceof Character) return (float) (Character) value;
        } else if (targetType == short.class) {
            if (value instanceof Number) return ((Number) value).shortValue();
            if (value instanceof Character) return (short) ((Character) value).charValue();
        } else if (targetType == byte.class) {
            if (value instanceof Number) return ((Number) value).byteValue();
            if (value instanceof Character) return (byte) ((Character) value).charValue();
        } else if (targetType == char.class) {
            if (value instanceof Number) return (char) ((Number) value).intValue();
            if (value instanceof Character) return value;
            if (value instanceof String && ((String) value).length() == 1) return ((String) value).charAt(0);
        } else if (targetType == boolean.class) {
            if (value instanceof Boolean) return value;
        }
        
        throw new EvaluationException(
            "Cannot convert " + value.getClass().getSimpleName() + " to " + targetType.getSimpleName(),
            -1,
            -1,
            ErrorCode.EVAL_TYPE_MISMATCH
        );
    }

    
    public static Object evaluateMapLiteral(MapLiteralNode node, ExecutionContext context) throws EvaluationException {
        Map<Object, Object> map = new java.util.LinkedHashMap<>();
        
        for (Map.Entry<ASTNode, ASTNode> entry : node.getEntries().entrySet()) {
            Object key = evaluate(entry.getKey(), context);
            Object value = evaluate(entry.getValue(), context);
            map.put(key, value);
        }
        
        return map;
    }
    
    public static Object evaluateIf(IfNode node, ExecutionContext context) throws EvaluationException {
        Object condition = evaluate(node.getCondition(), context);
        
        if (toBoolean(condition)) {
            return evaluate(node.getThenBlock(), context);
        } else if (node.getElseBlock() != null) {
            return evaluate(node.getElseBlock(), context);
        }
        
        return null;
    }
    
    public static Object evaluateWhile(WhileNode node, ExecutionContext context) throws EvaluationException {
        context.incrementLoopDepth();
        try {
            while (toBoolean(evaluate(node.getCondition(), context))) {
                context.getScopeManager().enterScope();
                try {
                    evaluate(node.getBody(), context);
                } finally {
                    context.getScopeManager().exitScope();
                }
            }
        } finally {
            context.decrementLoopDepth();
        }
        return null;
    }
    
    public static Object evaluateDoWhile(DoWhileNode node, ExecutionContext context) throws EvaluationException {
        context.incrementLoopDepth();
        try {
            do {
                context.getScopeManager().enterScope();
                try {
                    evaluate(node.getBody(), context);
                } finally {
                    context.getScopeManager().exitScope();
                }
            } while (toBoolean(evaluate(node.getCondition(), context)));
        } finally {
            context.decrementLoopDepth();
        }
        return null;
    }
    
    public static Object evaluateFor(ForNode node, ExecutionContext context) throws EvaluationException {
        context.getScopeManager().enterScope();
        context.incrementLoopDepth();
        
        try {
            if (node.getInitialization() != null) {
                evaluate(node.getInitialization(), context);
            }
            
            while (node.getCondition() == null || toBoolean(evaluate(node.getCondition(), context))) {
                evaluate(node.getBody(), context);
                
                if (node.getUpdate() != null) {
                    evaluate(node.getUpdate(), context);
                }
            }
        } finally {
            context.decrementLoopDepth();
            context.getScopeManager().exitScope();
        }
        
        return null;
    }
    
    public static Object evaluateForEach(ForEachNode node, ExecutionContext context) throws EvaluationException {
        Object iterable = evaluate(node.getCollection(), context);
        
        context.getScopeManager().enterScope();
        context.incrementLoopDepth();
        
        try {
            context.getScopeManager().declareVariable(
                node.getItemName(), 
                Object.class,
                null, 
                false
            );
            
            if (iterable instanceof Iterable) {
                for (Object item : (Iterable<?>) iterable) {
                    context.getScopeManager().setVariable(node.getItemName(), item);
                    evaluate(node.getBody(), context);
                }
            } else if (iterable != null && iterable.getClass().isArray()) {
                int length = Array.getLength(iterable);
                for (int i = 0; i < length; i++) {
                    Object item = Array.get(iterable, i);
                    context.getScopeManager().setVariable(node.getItemName(), item);
                    evaluate(node.getBody(), context);
                }
            }
        } finally {
            context.decrementLoopDepth();
            context.getScopeManager().exitScope();
        }
        
        return null;
    }
    
    public static Object evaluateTernary(TernaryNode node, ExecutionContext context) throws EvaluationException {
        Object condition = evaluate(node.getCondition(), context);
        
        if (toBoolean(condition)) {
            return evaluate(node.getThenExpr(), context);
        } else {
            return evaluate(node.getElseExpr(), context);
        }
    }
    
    public static Object evaluatePipeline(PipelineNode node, ExecutionContext context) throws EvaluationException {
        Object input = evaluate(node.getInput(), context);
        ASTNode functionNode = node.getFunction();
        
        if (functionNode instanceof VariableNode) {
            String funcName = ((VariableNode) functionNode).getName();
            ScopeManager.Variable funcVar = context.getScopeManager().getVariable(funcName);
            
            if (funcVar != null && funcVar.getValue() != null) {
                Object func = funcVar.getValue();
                
                if (func instanceof Lambda) {
                    return ((Lambda) func).invoke(new Object[]{input});
                }
                
                if (func instanceof MethodReference) {
                    return ((MethodReference) func).invoke(input);
                }
            }
        }
        
        if (functionNode instanceof MethodReferenceNode methodRef) {
            Object target = evaluate(methodRef.getTarget(), context);
            String methodName = methodRef.getMethodName();
            
            if (target instanceof Class<?> clazz) {
                try {
                    for (Method method : clazz.getMethods()) {
                        if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                            method.setAccessible(true);
                            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                                return method.invoke(null);
                            } else {
                                return method.invoke(input);
                            }
                        }
                    }
                    
                    Method method = findMethod(clazz, methodName, new Object[]{input});
                    if (method != null) {
                        method.setAccessible(true);
                        if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                            return method.invoke(null, input);
                        } else {
                            return method.invoke(input);
                        }
                    }
                    
                    throw new EvaluationException(
                        "Method not found for pipeline: " + clazz.getSimpleName() + "::" + methodName,
                        node.getLocation(),
                        ErrorCode.METHOD_NOT_FOUND
                    );
                } catch (EvaluationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new EvaluationException(
                        "Failed to invoke method in pipeline: " + methodName + " - " + e.getMessage(),
                        node.getLocation(),
                        ErrorCode.EVAL_METHOD_INVOCATION_FAILED
                    );
                }
            }
        }
        
        if (functionNode instanceof MethodCallNode methodCall) {
            List<ASTNode> args = new ArrayList<>();
            args.add(new LiteralNode(input, input != null ? input.getClass() : Object.class, node.getLocation()));
            args.addAll(methodCall.getArguments());
            
            MethodCallNode newCall = new MethodCallNode(
                methodCall.getTarget(),
                methodCall.getMethodName(),
                args,
                methodCall.getLocation()
            );
            return evaluateMethodCall(newCall, context);
        }
        
        if (functionNode instanceof FunctionCallNode funcCall) {
            List<ASTNode> args = new ArrayList<>();
            args.add(new LiteralNode(input, input != null ? input.getClass() : Object.class, node.getLocation()));
            args.addAll(funcCall.getArguments());
            
            FunctionCallNode newCall = new FunctionCallNode(funcCall.getFunctionName(), args, funcCall.getLocation());
            return evaluateFunctionCall(newCall, context);
        }
        
        if (functionNode instanceof LambdaNode) {
            Lambda lambda = (Lambda) evaluate(functionNode, context);
            return lambda.invoke(new Object[]{input});
        }
        
        throw new EvaluationException(
            "Pipeline requires a function, got: " + functionNode.getClass().getSimpleName(),
            node.getLocation(),
            ErrorCode.EVAL_TYPE_MISMATCH
        );
    }
    
    private static final ExecutorService asyncExecutor = Executors.newCachedThreadPool();
    
    public static Object evaluateAsync(AsyncNode node, ExecutionContext context) throws EvaluationException {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return evaluate(node.getExpression(), context);
            } catch (EvaluationException e) {
                throw new RuntimeException(e);
            }
        }, asyncExecutor);
    }
    
    public static Object evaluateAwait(AwaitNode node, ExecutionContext context) throws EvaluationException {
        Object value = evaluate(node.getExpression(), context);
        
        if (value instanceof CompletableFuture) {
            try {
                return ((CompletableFuture<?>) value).join();
            } catch (RuntimeException e) {
                if (e.getCause() instanceof EvaluationException) {
                    throw (EvaluationException) e.getCause();
                }
                throw new EvaluationException(
                    "Error awaiting future: " + e.getMessage(),
                    node.getLocation(),
                    ErrorCode.EVAL_INVALID_OPERATION
                );
            }
        }
        
        return value;
    }
    
    public static Object evaluateInstanceof(InstanceofNode node, ExecutionContext context) throws EvaluationException {
        Object value = evaluate(node.getExpression(), context);
        
        if (value == null) {
            return false;
        }
        
        try {
            Class<?> type = resolveType(node.getTypeName(), context);
            
            if (type.isPrimitive()) {
                Class<?> wrapperType = getWrapperType(type);
                return wrapperType.isInstance(value);
            }
            
            return type.isInstance(value);
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to resolve type: " + node.getTypeName(),
                node.getLocation(),
                ErrorCode.EVAL_TYPE_MISMATCH
            );
        }
    }
    
    private static Class<?> getWrapperType(Class<?> primitiveType) {
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == double.class) return Double.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == char.class) return Character.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == short.class) return Short.class;
        return primitiveType;
    }
    
    public static Object evaluateCast(CastNode node, ExecutionContext context) throws EvaluationException {
        Object value = evaluate(node.getExpression(), context);
        
        Class<?> type = node.getTargetType();
        
        try {
            if (type == int.class) return toInt(value);
            if (type == long.class) return toLong(value);
            if (type == float.class) return toFloat(value);
            if (type == double.class) return toDouble(value);
            if (type == boolean.class) return toBoolean(value);
            if (type == char.class) return toChar(value);
            if (type == byte.class) return toByte(value);
            if (type == short.class) return toShort(value);
            
            return type.cast(value);
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to cast to: " + type.getName(),
                node.getLocation(),
                ErrorCode.EVAL_TYPE_MISMATCH
            );
        }
    }
    
    public static Object evaluateLambda(LambdaNode node, ExecutionContext context) {
        Lambda func = new Lambda(node, context);
        
        Class<?> targetInterface = node.getFunctionalInterfaceType();
        if (targetInterface != null) {
            return func.asInterface(targetInterface);
        }
        
        return func;
    }
    
    public static Object evaluateMethodReference(MethodReferenceNode node, ExecutionContext context) throws EvaluationException {
        ASTNode targetNode = node.getTarget();
        String methodName = node.getMethodName();
        
        Class<?> targetClass;
        Object targetInstance = null;
        boolean isStatic = false;
        
        if (targetNode instanceof ClassReferenceNode classRef) {
            try {
                String className = classRef.getResolvedClass().getName();
                targetClass = ClassResolver.findClassWithImportsOrFail(className, context.getClassLoader(), context.getImports());
                isStatic = true;
            } catch (ClassNotFoundException e) {
                throw new EvaluationException(
                    "Class not found: " + classRef.getTypeName(),
                    node.getLocation(),
                    ErrorCode.EVAL_CLASS_NOT_FOUND
                );
            }
        } else if (targetNode instanceof VariableNode) {
            String varName = ((VariableNode) targetNode).getName();
            
            if (context.getScopeManager().hasVariable(varName)) {
                targetInstance = context.getScopeManager().getVariable(varName).getValue();
                targetClass = targetInstance != null ? targetInstance.getClass() : Object.class;
            } else {
                targetClass = context.getClassFinder().findClassWithImports(varName, context.getClassLoader(), context.getImports());
                if (targetClass != null) {
                    isStatic = true;
                } else {
                    throw new EvaluationException(
                        "Cannot resolve method reference target: " + varName,
                        node.getLocation(),
                        ErrorCode.EVAL_UNDEFINED_VARIABLE
                    );
                }
            }
        } else {
            targetInstance = evaluate(targetNode, context);
            targetClass = targetInstance != null ? targetInstance.getClass() : Object.class;
        }
        
        if (isStatic && targetClass != null) {
            java.lang.reflect.Method actualMethod = findMethodInClass(targetClass, methodName);
            if (actualMethod != null && !java.lang.reflect.Modifier.isStatic(actualMethod.getModifiers())) {
                return MethodReference.createUnboundInstanceMethod(targetClass, methodName, node.getLocation());
            }
        }
        
        return new MethodReference(targetClass, targetInstance, methodName, isStatic, node.getLocation());
    }
    
    private static java.lang.reflect.Method findMethodInClass(Class<?> clazz, String methodName) {
        for (java.lang.reflect.Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
    
    public static Object evaluateTry(TryNode node, ExecutionContext context) throws EvaluationException {
        List<Object> resources = new ArrayList<>();
        List<Throwable> suppressedExceptions = new ArrayList<>();
        
        try {
            for (ResourceDeclaration resource : node.getResources()) {
                Object value = evaluate(resource.getInitializer(), context);
                resources.add(value);
                context.getScopeManager().declareVariable(
                    resource.getVariableName(),
                    resource.getType() != null ? resource.getType() : Object.class,
                    value,
                    false
                );
            }
            
            return evaluate(node.getTryBlock(), context);
        } catch (EvaluationException e) {
            Throwable actualException = e.getCause() != null ? e.getCause() : e;
            
            for (CatchClause catchClause : node.getCatchClauses()) {
                for (Class<?> exceptionType : catchClause.getExceptionTypes()) {
                    if (exceptionType.isInstance(actualException)) {
                        context.getScopeManager().enterScope();
                        context.getScopeManager().declareVariable(
                            catchClause.getVariableName(),
                            exceptionType,
                            actualException,
                            false
                        );
                        try {
                            return evaluate(catchClause.getBody(), context);
                        } finally {
                            context.getScopeManager().exitScope();
                        }
                    }
                }
            }
            throw e;
        } finally {
            for (int i = resources.size() - 1; i >= 0; i--) {
                Object resource = resources.get(i);
                if (resource != null) {
                    try {
                        if (resource instanceof AutoCloseable) {
                            ((AutoCloseable) resource).close();
                        } else {
                            Method closeMethod = resource.getClass().getMethod("close");
                            closeMethod.invoke(resource);
                        }
                    } catch (Exception closeException) {
                        suppressedExceptions.add(closeException);
                    }
                }
            }
            
            if (node.getFinallyBlock() != null) {
                evaluate(node.getFinallyBlock(), context);
            }
        }
    }
    
    public static Object evaluateThrow(ThrowNode node, ExecutionContext context) throws EvaluationException {
        Object exception = evaluate(node.getExpression(), context);
        
        if (exception instanceof Throwable t) {
            String message = t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
            throw new EvaluationException(
                message, 
                node.getLocation(), 
                ErrorCode.EVAL_EXCEPTION_THROWN,
                t
            );
        }
        
        throw new EvaluationException("Cannot throw non-throwable: " + exception, node.getLocation(), ErrorCode.EVAL_INVALID_OPERATION);
    }
    
    public static Object evaluateSwitch(SwitchNode node, ExecutionContext context) throws EvaluationException {
        Object value = evaluate(node.getExpression(), context);
        
        for (CaseNode caseNode : node.getCases()) {
            if (caseNode.getValue() == null) {
                continue;
            }
            Object caseValue = evaluate(caseNode.getValue(), context);
            if (Objects.equals(value, caseValue)) {
                Object result = null;
                for (ASTNode stmt : caseNode.getStatements()) {
                    result = evaluate(stmt, context);
                }
                return result;
            }
        }
        
        if (node.getDefaultCase() != null) {
            return evaluate(node.getDefaultCase(), context);
        }
        
        return null;
    }
    
    public static Object evaluateDelete(DeleteNode node, ExecutionContext context) throws EvaluationException {
        if (node.isDeleteAll()) {
            context.getScopeManager().clearCurrentScope();
        } else {
            String varName = node.getVariableName();
            if (!context.getScopeManager().deleteVariable(varName)) {
                throw new EvaluationException(
                    "Variable not found: " + varName,
                    node.getLocation(),
                    ErrorCode.SCOPE_VARIABLE_NOT_FOUND
                );
            }
        }
        return null;
    }
    
    public static Object evaluateClassDeclaration(ClassDeclarationNode node, ExecutionContext context) throws EvaluationException {
        DynamicClassGenerator generator = new DynamicClassGenerator(context);
        Class<?> generatedClass = generator.generateClass(node);
        
        String className = node.getClassName();
        context.registerCustomClass(className, generatedClass);
        context.getScopeManager().declareVariable(className, Class.class, generatedClass, false);
        
        return generatedClass;
    }
    
    public static Method findMethod(Class<?> clazz, String name, Object[] args) {
        List<Method> candidates = new ArrayList<>();
        
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
                candidates.add(method);
            }
        }
        
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name) && !candidates.contains(method)) {
                candidates.add(method);
            }
        }
        
        Method bestMatch = null;
        int bestScore = -1;
        Method varArgsCandidate = null;
        
        for (Method method : candidates) {
            Class<?>[] paramTypes = method.getParameterTypes();
            boolean isVarArgs = method.isVarArgs();
            
            if (isApplicableArgs(paramTypes, args, isVarArgs)) {
                if (!isVarArgs && paramTypes.length == (args != null ? args.length : 0)) {
                    int matchScore = computeMatchScore(paramTypes, args);
                    
                    if (matchScore > bestScore) {
                        bestScore = matchScore;
                        bestMatch = method;
                    }
                }
                
                if (isVarArgs && varArgsCandidate == null) {
                    varArgsCandidate = method;
                }
            }
        }
        
        if (bestMatch != null) {
            return bestMatch;
        }
        
        return varArgsCandidate;
    }
    
    private static int computeMatchScore(Class<?>[] paramTypes, Object[] args) {
        int score = 0;
        for (int i = 0; i < paramTypes.length; i++) {
            if (args == null || i >= args.length) {
                return 0;
            }
            if (args[i] == null) {
                if (paramTypes[i].isPrimitive()) {
                    return 0;
                }
                score += 1;
            } else {
                Class<?> argType = args[i].getClass();
                
                if (paramTypes[i].equals(argType)) {
                    score += 3;
                } else if (paramTypes[i].isPrimitive() && isExactWrapperFor(argType, paramTypes[i])) {
                    score += 2;
                } else if (paramTypes[i].isAssignableFrom(argType)) {
                    score += 1;
                } else if (paramTypes[i].isPrimitive() && isWrapperFor(argType, paramTypes[i])) {
                    score += 1;
                } else {
                    return 0;
                }
            }
        }
        return score;
    }
    
    private static boolean isExactWrapperFor(Class<?> wrapperType, Class<?> primitiveType) {
        if (primitiveType == int.class) return wrapperType == Integer.class;
        if (primitiveType == long.class) return wrapperType == Long.class;
        if (primitiveType == double.class) return wrapperType == Double.class;
        if (primitiveType == float.class) return wrapperType == Float.class;
        if (primitiveType == boolean.class) return wrapperType == Boolean.class;
        if (primitiveType == char.class) return wrapperType == Character.class;
        if (primitiveType == byte.class) return wrapperType == Byte.class;
        if (primitiveType == short.class) return wrapperType == Short.class;
        return false;
    }
    
    private static boolean isApplicableArgs(Class<?>[] paramTypes, Object[] args, boolean isVarArgs) {
        int paramCount = paramTypes.length;
        int argCount = args != null ? args.length : 0;
        
        if (isVarArgs) {
            if (argCount < paramCount - 1) {
                return false;
            }
            
            for (int i = 0; i < paramCount - 1; i++) {
                if (!isAssignable(paramTypes[i], args[i])) {
                    return false;
                }
            }
            
            if (argCount >= paramCount) {
                Class<?> varArgType = paramTypes[paramCount - 1].getComponentType();
                for (int i = paramCount - 1; i < argCount; i++) {
                    if (!isAssignable(varArgType, args[i])) {
                        return false;
                    }
                }
            }

        } else {
            if (paramCount != argCount) {
                return false;
            }
            
            for (int i = 0; i < paramCount; i++) {
                if (!isAssignable(paramTypes[i], args[i])) {
                    return false;
                }
            }

        }
        return true;
    }
    
    private static boolean isAssignable(Class<?> targetType, Object arg) {
        if (arg == null) {
            return !targetType.isPrimitive();
        }
        
        Class<?> argType = arg.getClass();
        
        if (targetType.isPrimitive()) {
            return isWrapperFor(argType, targetType);
        }
        
        return targetType.isAssignableFrom(argType);
    }
    
    private static boolean isWrapperFor(Class<?> wrapperType, Class<?> primitiveType) {
        if (primitiveType == int.class) return wrapperType == Integer.class;
        if (primitiveType == long.class) return wrapperType == Long.class || wrapperType == Integer.class;
        if (primitiveType == double.class) return wrapperType == Double.class || wrapperType == Float.class || wrapperType == Long.class || wrapperType == Integer.class;
        if (primitiveType == float.class) return wrapperType == Float.class || wrapperType == Integer.class;
        if (primitiveType == boolean.class) return wrapperType == Boolean.class;
        if (primitiveType == char.class) return wrapperType == Character.class;
        if (primitiveType == byte.class) return wrapperType == Byte.class;
        if (primitiveType == short.class) return wrapperType == Short.class || wrapperType == Byte.class;
        return false;
    }

    private static Class<?> resolveType(String typeName, ExecutionContext context) throws ClassNotFoundException {
        if (typeName == null || typeName.isEmpty()) {
            return void.class;
        }
        
        String baseType = typeName;
        int arrayDepth = 0;
        
        while (baseType.endsWith("[]")) {
            baseType = baseType.substring(0, baseType.length() - 2);
            arrayDepth++;
        }
        
        int genericIndex = baseType.indexOf('<');
        if (genericIndex > 0) {
            baseType = baseType.substring(0, genericIndex);
        }
        
        Class<?> componentType;
        switch (baseType) {
            case "int": componentType = int.class; break;
            case "long": componentType = long.class; break;
            case "float": componentType = float.class; break;
            case "double": componentType = double.class; break;
            case "boolean": componentType = boolean.class; break;
            case "char": componentType = char.class; break;
            case "byte": componentType = byte.class; break;
            case "short": componentType = short.class; break;
            case "void": componentType = void.class; break;
            default:
                componentType = context.getClassFinder().findClassWithImports(baseType, context.getClassLoader(), context.getImports());
                if (componentType == null) {
                    throw new ClassNotFoundException("Class not found: " + baseType);
                }
        }
        
        if (arrayDepth > 0) {
            for (int i = 0; i < arrayDepth; i++) {
                componentType = Array.newInstance(componentType, 0).getClass();
            }
        }
        
        return componentType;
    }
    
    
    private static boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        return true;
    }
    
    private static int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        if (value instanceof Character) return (Character) value;
        return 0;
    }
    
    private static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
    
    private static float toFloat(Object value) {
        if (value == null) return 0.0f;
        if (value instanceof Number) return ((Number) value).floatValue();
        return 0.0f;
    }
    
    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }
    
    private static char toChar(Object value) {
        if (value == null) return '\0';
        if (value instanceof Character) return (Character) value;
        if (value instanceof Number) return (char) ((Number) value).intValue();
        return '\0';
    }
    
    private static byte toByte(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).byteValue();
        return 0;
    }
    
    private static short toShort(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).shortValue();
        return 0;
    }
    
    public static Object evaluateReturn(ReturnNode node, ExecutionContext context) throws EvaluationException {
        Object value = null;
        if (node.getValue() != null) {
            value = evaluate(node.getValue(), context);
        }
        throw new ReturnException(value);
    }

    private static Object[] arrayDifference(Object[] left, Object[] right) {
        Set<Object> rightSet = new HashSet<>(Arrays.asList(right));
        List<Object> result = new ArrayList<>();
        for (Object obj : left) {
            if (!rightSet.contains(obj)) {
                result.add(obj);
            }
        }
        return result.toArray();
    }
    
    private static Object[] arrayIntersection(Object[] left, Object[] right) {
        Set<Object> rightSet = new HashSet<>(Arrays.asList(right));
        List<Object> result = new ArrayList<>();
        Set<Object> seen = new HashSet<>();
        for (Object obj : left) {
            if (rightSet.contains(obj) && !seen.contains(obj)) {
                result.add(obj);
                seen.add(obj);
            }
        }
        return result.toArray();
    }
    
    private static Object[] arrayUnion(Object[] left, Object[] right) {
        Set<Object> resultSet = new LinkedHashSet<>();
        resultSet.addAll(Arrays.asList(left));
        resultSet.addAll(Arrays.asList(right));
        return resultSet.toArray();
    }
    
    private static Object[] arraySymmetricDifference(Object[] left, Object[] right) {
        Set<Object> leftSet = new HashSet<>(Arrays.asList(left));
        Set<Object> rightSet = new HashSet<>(Arrays.asList(right));
        List<Object> result = new ArrayList<>();
        for (Object obj : left) {
            if (!rightSet.contains(obj)) {
                result.add(obj);
            }
        }
        for (Object obj : right) {
            if (!leftSet.contains(obj)) {
                result.add(obj);
            }
        }
        return result.toArray();
    }
    
    private static Object[] arrayReverse(Object[] arr) {
        Object[] result = new Object[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[arr.length - 1 - i];
        }
        return result;
    }
    
    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value.getClass().isArray()) {
            StringBuilder sb = new StringBuilder("[");
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValue(Array.get(value, i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return String.valueOf(value);
    }
}
