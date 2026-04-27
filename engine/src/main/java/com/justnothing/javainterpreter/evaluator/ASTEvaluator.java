package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.api.ClassResolver;
import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.GenericType;
import com.justnothing.javainterpreter.ast.nodes.*;
import com.justnothing.javainterpreter.builtins.Lambda;
import com.justnothing.javainterpreter.builtins.MethodReference;
import com.justnothing.javainterpreter.exception.ErrorCode;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.exception.ReturnException;
import com.justnothing.javainterpreter.exception.BreakException;
import com.justnothing.javainterpreter.exception.ContinueException;
import com.justnothing.javainterpreter.utils.ArrayUtils;
import com.justnothing.javainterpreter.utils.NumberUtils;
import com.justnothing.javainterpreter.utils.TypeUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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

    public static Object evaluateInterpolatedString(InterpolatedStringNode node, ExecutionContext context)
            throws EvaluationException {
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
            ScopeManager.Variable variable = context.getScopeManager().getVariable(varName, node);
            return variable.getValue();
        } catch (EvaluationException e) {
            // 尝试从 this 对象中解析字段
            if (!varName.equals("this")) {
                try {
                    ScopeManager.Variable thisVar = context.getScopeManager().getVariable("this", node);
                    if (thisVar != null) {
                        Object instance = thisVar.getValue();
                        if (instance != null) {
                            try {
                                Field field = TypeUtils.findField(instance.getClass(), varName);
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

    public static Object evaluateBinaryOp(BinaryOpNode node, ExecutionContext context) throws EvaluationException {
        Object left = evaluate(node.getLeft(), context);
        Object right = evaluate(node.getRight(), context);
        BinaryOpNode.Operator op = node.getOperator();

        boolean isBothArray = left != null && right != null && left.getClass().isArray() && right.getClass().isArray();
        switch (op) {
            case ADD:
                if (left instanceof String || right instanceof String) {
                    // 拼接字符串，包括数组
                    return formatValue(left) + formatValue(right);
                }
                if (left instanceof Number && right instanceof Number) {
                    // 单纯的数字运算
                    return NumberUtils.addNumbers((Number) left, (Number) right);
                }
                if (isBothArray) {
                    // 数组拼接
                    return ArrayUtils.arrayConcat(left, right, node);
                }
                break;
            case SUBTRACT:
                if (left instanceof Number && right instanceof Number) {
                    return NumberUtils.subtractNumbers((Number) left, (Number) right);
                }
                if (isBothArray) {
                    // 差集
                    return ArrayUtils.arrayDifference(left, right, node);
                }
                break;
            case MULTIPLY:
                if (left instanceof Number && right instanceof Number) {
                    // 单纯的乘法
                    return NumberUtils.multiplyNumbers((Number) left, (Number) right);
                } else if (left instanceof String && right instanceof Number) {
                    // 字符串重复
                    return ((String) left).repeat((int) right);
                } else if (left != null && left.getClass().isArray() && right instanceof Number) {
                    // 数组重复
                    return ArrayUtils.arrayRepeat(left, (int) right, node);
                }
                break;
            case DIVIDE:
                if (left instanceof Number && right instanceof Number) {
                    return NumberUtils.divideNumbers((Number) left, (Number) right);
                }
                break;
            case MODULO:
                if (left instanceof Number && right instanceof Number) {
                    return NumberUtils.moduloNumbers((Number) left, (Number) right);
                }
                break;
            case POWER:
                if (left instanceof Number && right instanceof Number) {
                    double result = Math.pow(((Number) left).doubleValue(), ((Number) right).doubleValue());
                    if (TypeUtils.isFloatingType((Number) left) || TypeUtils.isFloatingType((Number) right)) {
                        return result;
                    }
                    if (result == Math.floor(result) && result <= Long.MAX_VALUE && result >= Long.MIN_VALUE) {
                        return (long) result;
                    }
                    return result;
                }
                if (isBothArray) {
                    return ArrayUtils.arrayCartesianProduct(left, right, node);
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
                return NumberUtils.createRange(left, right, false, node);
            case RANGE_EXCLUSIVE:
                return NumberUtils.createRange(left, right, true, node);
            case EQUAL:
                return Objects.equals(left, right);
            case NOT_EQUAL:
                return !Objects.equals(left, right);
            case LESS_THAN:
                if (left instanceof Number && right instanceof Number) {
                    return NumberUtils.compareNumbers((Number) left, (Number) right) < 0;
                }
                break;
            case LESS_THAN_OR_EQUAL:
                if (left instanceof Number && right instanceof Number) {
                    return NumberUtils.compareNumbers((Number) left, (Number) right) <= 0;
                }
                break;
            case GREATER_THAN:
                if (left instanceof Number && right instanceof Number) {
                    return NumberUtils.compareNumbers((Number) left, (Number) right) > 0;
                }
                break;
            case GREATER_THAN_OR_EQUAL:
                if (left instanceof Number && right instanceof Number) {
                    return NumberUtils.compareNumbers((Number) left, (Number) right) >= 0;
                }
                break;
            case SPACESHIP:
                if (left instanceof Number && right instanceof Number) {
                    return NumberUtils.compareNumbers((Number) left, (Number) right);
                }
                if (left instanceof Comparable && right != null) {
                    return ((Comparable) left).compareTo(right);
                }
                break;
            case LOGICAL_AND:
                return TypeUtils.toBoolean(left) && TypeUtils.toBoolean(right);
            case LOGICAL_OR:
                return TypeUtils.toBoolean(left) || TypeUtils.toBoolean(right);
            case BITWISE_AND:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() & ((Number) right).longValue();
                }
                if (isBothArray) {
                    // 交集
                    return ArrayUtils.arrayIntersection(left, right, node);
                }
                break;
            case BITWISE_OR:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() | ((Number) right).longValue();
                }
                if (isBothArray) {
                    // 并集
                    return ArrayUtils.arrayUnion(left, right, node);
                }
                break;
            case BITWISE_XOR:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() ^ ((Number) right).longValue();
                }
                if (isBothArray) {
                    // 对称差集 (异或集)
                    return ArrayUtils.arraySymmetricDifference(left, right, node);
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
                        left.getClass().getName() + " and " +
                        (right != null ? right.getClass().getName() : "null"),
                ErrorCode.EVAL_INVALID_OPERATION,
                node);
    }

    public static Object evaluateUnaryOp(UnaryOpNode node, ExecutionContext context) throws EvaluationException {
        UnaryOpNode.Operator op = node.getOperator();

        switch (op) {
            case NEGATIVE: {
                Object operand = evaluate(node.getOperand(), context);
                if (operand instanceof Number n) {
                    if (TypeUtils.isFloatingType(n)) {
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
                return !TypeUtils.toBoolean(operand);
            }
            case NOT_NULL: {
                Object operand = evaluate(node.getOperand(), context);
                if (operand == null) {
                    throw new EvaluationException(
                            "Non-null assertion failed: value is null",
                            ErrorCode.EVAL_NULL_POINTER,
                            node);
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
                if (operand.getClass().isArray()) {
                    return ArrayUtils.arrayReverse(operand, node);
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
                ErrorCode.EVAL_INVALID_OPERATION,
                node);
    }

    private static Object evaluateIncrementDecrement(UnaryOpNode node, ExecutionContext context)
            throws EvaluationException {
        UnaryOpNode.Operator op = node.getOperator();
        ASTNode operand = node.getOperand();

        if (!(operand instanceof VariableNode)) {
            throw new EvaluationException(
                    "Increment/decrement operator requires a variable",
                    ErrorCode.EVAL_INVALID_OPERATION,
                    node);
        }

        String varName = ((VariableNode) operand).getName();
        Object currentValue = context.getScopeManager().getVariable(varName, node).getValue();

        if (!(currentValue instanceof Number numValue)) {
            throw new EvaluationException(
                    "Increment/decrement operator requires a numeric variable",
                    ErrorCode.EVAL_INVALID_OPERATION,
                    node);
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

        context.getScopeManager().setVariable(varName, newValue, node);

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
                        node.isFinal(),
                        node);
                value = evaluate(valueNode, context);
                context.getScopeManager().setVariable(node.getVariableName(), value, node);
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
                        node.isFinal(),
                        node);
                value = evaluate(valueNode, context);
                if (declaredType.isInterface() && value instanceof MethodReference) {
                    value = ((MethodReference) value).asInterface(declaredType);
                }
                context.getScopeManager().setVariable(node.getVariableName(), value, node);
                return value;
            }
            value = evaluate(valueNode, context);
        } else if (node.isDeclaration()) {
            value = TypeUtils.getDefaultValue(node.getDeclaredClass());
        } else {
            value = null;
        }

        if (node.isDeclaration()) {
            Class<?> declaredType = node.getDeclaredClass() != null ? node.getDeclaredClass() : Object.class;

            if (AutoClass.isAutoType(declaredType)) {
                declaredType = AutoClass.inferStrictType(value);
            }

            value = TypeUtils.convertValue(value, declaredType, node);

            context.getScopeManager().declareVariable(
                    node.getVariableName(),
                    declaredType,
                    value,
                    node.isFinal(),
                    node);
        } else {
            ScopeManager.Variable variable = context.getScopeManager().getVariable(node.getVariableName(), node);
            if (variable != null && variable.getType() != null) {
                value = TypeUtils.convertValue(value, variable.getType(), node);
            }
            context.getScopeManager().setVariable(node.getVariableName(), value, node);
        }

        return value;
    }

    public static Object evaluateConditionalAssign(ConditionalAssignNode node, ExecutionContext context)
            throws EvaluationException {
        String varName = node.getVariableName();

        if (!context.getScopeManager().hasVariable(varName)) {
            Object value = evaluate(node.getValue(), context);
            context.getScopeManager().declareVariable(varName, Object.class, value, false, node);
            return value;
        }

        ScopeManager.Variable existing = context.getScopeManager().getVariable(varName, node);
        if (existing.getValue() == null) {
            Object value = evaluate(node.getValue(), context);
            context.getScopeManager().setVariable(varName, value, node);
            return value;
        }

        return existing.getValue();
    }

    public static Object evaluateNullCoalescingAssign(NullCoalescingAssignNode node, ExecutionContext context)
            throws EvaluationException {
        String varName = node.getVariableName();

        if (!context.getScopeManager().hasVariable(varName)) {
            Object value = evaluate(node.getValue(), context);
            context.getScopeManager().declareVariable(varName, Object.class, value, false, node);
            return value;
        }

        ScopeManager.Variable existing = context.getScopeManager().getVariable(varName, node);
        if (existing.getValue() == null) {
            Object value = evaluate(node.getValue(), context);
            context.getScopeManager().setVariable(varName, value, node);
            return value;
        }

        return existing.getValue();
    }

    public static Object evaluateFieldAssignment(FieldAssignmentNode node, ExecutionContext context)
            throws EvaluationException {
        Object target = evaluate(node.getTarget(), context);
        Object value = evaluate(node.getValue(), context);

        try {
            Field field;
            if (target instanceof Class<?> targetClass) {
                // 处理静态字段赋值
                field = TypeUtils.findField(targetClass, node.getFieldName());
                if (field == null) {
                    throw new EvaluationException(
                            "Field not found: " + node.getFieldName(),
                            ErrorCode.EVAL_FIELD_ACCESS_FAILED,
                            node);
                }
                field.setAccessible(true);
                field.set(null, value);
            } else {
                // 处理实例字段赋值
                field = TypeUtils.findField(target.getClass(), node.getFieldName());
                if (field == null) {
                    throw new EvaluationException(
                            "Field not found: " + node.getFieldName(),
                            ErrorCode.EVAL_FIELD_ACCESS_FAILED,
                            node);
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
                    ErrorCode.EVAL_FIELD_ACCESS_FAILED,
                    node);
        }
    }

    public static Object evaluateArrayAssignment(ArrayAssignmentNode node, ExecutionContext context)
            throws EvaluationException {
        Object array = evaluate(node.getArray(), context);
        Object index = evaluate(node.getIndex(), context);
        Object value = evaluate(node.getValue(), context);

        int idx = TypeUtils.toInt(index);
        Array.set(array, idx, value);
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Object evaluateFunctionCall(FunctionCallNode node, ExecutionContext context)
            throws EvaluationException {
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
            Object func = context.getScopeManager().getVariable(functionName, node).getValue();
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
                        MethodReference mr = ((MethodReference.MethodReferenceInvocationHandler) handler)
                                .getMethodReference();
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
                        Method method = TypeUtils.findMethod(currentInstance.getClass(), functionName, args);
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
                        Method method = TypeUtils.findMethod(currentClassObj, functionName, args);
                        if (method != null && Modifier.isStatic(method.getModifiers())) {
                            try {
                                method.setAccessible(true);
                                return method.invoke(null, args);
                            } catch (Exception ignored) {
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
                ErrorCode.EVAL_UNDEFINED_VARIABLE,
                node);
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
                    ErrorCode.METHOD_NOT_FOUND,
                    node);
        }

        Object[] args = new Object[node.getArguments().size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = evaluate(node.getArguments().get(i), context);
        }

        try {
            Class<?> targetClass;
            Object targetInstance;

            if (target instanceof Lambda) {
                // 检查是否是 Object 类的方法
                if ("getClass".equals(methodName) || "toString".equals(methodName) ||
                        "hashCode".equals(methodName) || "equals".equals(methodName)) {
                    // 按照普通对象处理
                    targetClass = target.getClass();
                    targetInstance = target;
                } else {
                    // 调用 Lambda 函数
                    return ((Lambda) target).invoke(args);
                }
            } else if (target instanceof MethodReference) {
                return ((MethodReference) target).invoke(args);
            } else if (target instanceof Class<?> targetClassObj) {
                Method classMethod = TypeUtils.findMethod(Class.class, methodName, args);
                if (classMethod != null) {
                    classMethod.setAccessible(true);
                    Object[] invokeArgs = TypeUtils.prepareInvokeArguments(classMethod, args);
                    return classMethod.invoke(targetClassObj, invokeArgs);
                }
                targetClass = targetClassObj;
                targetInstance = null;
            } else {
                if (target == null) {
                    throw new EvaluationException(
                            "Target is null",
                            ErrorCode.METHOD_INVOCATION_TARGET_NULL,
                            node);
                }
                targetClass = target.getClass();
                targetInstance = target;
            }

            context.checkMethodAccess(targetClass.getName(), methodName, null);

            Method method = TypeUtils.findMethod(targetClass, methodName, args);
            if (method != null) {
                if (!Modifier.isStatic(method.getModifiers()) && targetInstance == null) {
                    throw new EvaluationException(
                            "Method " + methodName + " is a instance method and requires an instance",
                            ErrorCode.METHOD_NOT_FOUND,
                            node);
                }
                try {
                    method.setAccessible(true);
                } catch (Exception ignored) {
                }
                Object[] invokeArgs = TypeUtils.prepareInvokeArguments(method, args);
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

            // 如果方法实际存在，是因为方法签名对不上而返回的null，就告诉用户所有的候选项
            if (Arrays.stream(targetClass.getMethods())
                    .map(Method::getName)
                    .anyMatch(methodName::equals)) {

                String finalMethodName = methodName;
                throw new EvaluationException(
                        "Can't find method " + methodName + " with signature " + Arrays.toString(args) + "\n" +
                                "Possible candidates are: " + "\n" + Arrays.stream(targetClass.getMethods())
                                        .filter(m -> m.getName().equals(finalMethodName))
                                        .map(Method::toString)
                                        .map(s -> s.substring(s.indexOf('(') + 1, s.indexOf(')')))
                                        .map(s -> s.replace(",", ", "))
                                        .map(s -> s.isEmpty() ? "[No arguments]" : s)
                                        .map(s -> "  " + s)
                                        .collect(Collectors.joining("\n"))
                                + "\n",
                        ErrorCode.METHOD_NOT_FOUND,
                        node);
            }

            throw new EvaluationException(
                    "Method not found: " + methodName,
                    ErrorCode.METHOD_NOT_FOUND,
                    node);
        } catch (EvaluationException e) {
            throw e;
        } catch (SecurityException e) {
            throw new EvaluationException(
                    "Permission denied for method call: " + methodName + " - " + e.getMessage(),
                    ErrorCode.EVAL_PERMISSION_DENIED,
                    e,
                    node);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new EvaluationException(
                    "Failed to call method: " + methodName + " - " + cause.getMessage(),
                    ErrorCode.EVAL_METHOD_INVOCATION_FAILED,
                    cause,
                    node);
        }
    }

    public static Object evaluateSafeMethodCall(SafeMethodCallNode node, ExecutionContext context)
            throws EvaluationException {
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
                Method classMethod = TypeUtils.findMethod(Class.class, methodName, args);
                if (classMethod != null) {
                    classMethod.setAccessible(true);
                    Object[] invokeArgs = TypeUtils.prepareInvokeArguments(classMethod, args);
                    return classMethod.invoke(targetClassObj, invokeArgs);
                }

                targetClass = targetClassObj;
                targetInstance = null;
            } else {
                targetClass = target.getClass();
                targetInstance = target;
            }

            Method method = TypeUtils.findMethod(targetClass, methodName, args);
            if (method != null) {
                method.setAccessible(true);
                Object[] invokeArgs = TypeUtils.prepareInvokeArguments(method, args);
                return method.invoke(targetInstance, invokeArgs);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Object evaluateSuperMethodCall(SuperMethodCallNode node, ExecutionContext context)
            throws EvaluationException {
        try {
            Object thisInstance = context.getScopeManager().getVariable("this", node).getValue();
            if (thisInstance == null) {
                throw new EvaluationException(
                        "Cannot call super method: 'this' is null",
                        ErrorCode.EVAL_INVALID_OPERATION,
                        node);
            }

            String methodName = node.getMethodName();
            List<ASTNode> argNodes = node.getArguments();

            Object[] args = new Object[argNodes.size()];
            for (int i = 0; i < argNodes.size(); i++) {
                args[i] = evaluate(argNodes.get(i), context);
            }

            String currentClass = MethodBodyExecutor.getCurrentClassContext();
            if (currentClass != null) {
                Class<?> currentClazz;
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
                                Method method = TypeUtils.findMethod(superClass, methodName, args);
                                if (method != null) {
                                    method.setAccessible(true);
                                    Object[] invokeArgs = TypeUtils.prepareInvokeArguments(method, args);
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
                        ErrorCode.EVAL_INVALID_OPERATION,
                        node);
            }

            try {
                String superClassName = superClass.getSimpleName();
                return MethodBodyExecutor.executeMethod(superClassName, methodName, thisInstance, args);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("Method not found")) {
                    Method method = TypeUtils.findMethod(superClass, methodName, args);
                    if (method == null) {
                        throw new EvaluationException(
                                "Method not found in superclass: " + methodName,
                                ErrorCode.EVAL_METHOD_INVOCATION_FAILED,
                                node);
                    }
                    method.setAccessible(true);
                    Object[] invokeArgs = TypeUtils.prepareInvokeArguments(method, args);
                    return method.invoke(thisInstance, invokeArgs);
                }
                throw e;
            }

        } catch (EvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException(
                    "Failed to call super method: " + e.getMessage(),
                    ErrorCode.EVAL_INVALID_OPERATION,
                    e,
                    node);
        }
    }

    public static Object evaluateFieldAccess(FieldAccessNode node, ExecutionContext context)
            throws EvaluationException {
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
                Field field = TypeUtils.findField(targetClass, node.getFieldName());
                if (field != null) {
                    field.setAccessible(true);
                    return field.get(null);
                }

                Method[] methods = targetClass.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getName().equals(node.getFieldName()) && Modifier.isStatic(method.getModifiers())) {
                        method.setAccessible(true);
                        return new MethodReference(targetClass, null, node.getFieldName(), true, node);
                    }
                }

                throw new NoSuchFieldException(
                        "Field " + node.getFieldName() + " not found in " + targetClass.getName());
            }

            Field field = TypeUtils.findField(targetClass, node.getFieldName());
            if (field != null) {
                field.setAccessible(true);
                return field.get(target);
            }

            throw new NoSuchFieldException("Field " + node.getFieldName() + " not found in " + targetClass.getName());
        } catch (SecurityException e) {
            throw new EvaluationException(
                    "Permission denied for field access: " + node.getFieldName() + " - " + e.getMessage(),
                    ErrorCode.EVAL_PERMISSION_DENIED,
                    e,
                    node);
        } catch (Exception e) {
            throw new EvaluationException(
                    "Failed to access field: " + e.getMessage(),
                    ErrorCode.EVAL_FIELD_ACCESS_FAILED,
                    e,
                    node);
        }
    }

    public static Object evaluateSafeFieldAccess(SafeFieldAccessNode node, ExecutionContext context)
            throws EvaluationException {
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

    public static Object evaluateArrayAccess(ArrayAccessNode node, ExecutionContext context)
            throws EvaluationException {
        Object array = evaluate(node.getArray(), context);
        Object index = evaluate(node.getIndex(), context);

        return Array.get(array, TypeUtils.toInt(index));
    }

    public static Object evaluateClassReference(ClassReferenceNode node, ExecutionContext context)
            throws EvaluationException {
        try {
            // 优先使用 AST 解析阶段已经解析好的类
            Class<?> resolvedClass = node.getResolvedClass();
            if (resolvedClass != null) {
                context.checkClassAccess(resolvedClass.getName());
                return resolvedClass;
            }

            // 如果解析失败，再尝试其他方法
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
                    ErrorCode.EVAL_CLASS_NOT_FOUND,
                    node);
        } catch (SecurityException e) {
            throw new EvaluationException(
                    "Permission denied for class access: " + node.getTypeName() + " - " + e.getMessage(),
                    ErrorCode.EVAL_PERMISSION_DENIED,
                    e,
                    node);
        } catch (Exception e) {
            throw new EvaluationException(
                    "Class not found: " + node.getTypeName(),
                    ErrorCode.EVAL_CLASS_NOT_FOUND,
                    e,
                    node);
        }
    }

    public static Object evaluateNewArray(NewArrayNode node, ExecutionContext context) throws EvaluationException {
        try {
            Class<?> componentType = node.getElementType();
            Object size = evaluate(node.getSize(), context);
            return Array.newInstance(componentType, TypeUtils.toInt(size));
        } catch (Exception e) {
            throw new EvaluationException(
                    "Failed to create array: " + e.getMessage(),
                    ErrorCode.EVAL_ARRAY_ACCESS_FAILED,
                    e,
                    node);
        }
    }

    public static Object evaluateConstructorCall(ConstructorCallNode node, ExecutionContext context)
            throws EvaluationException {
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

                Class<?> superRawType = type.getRawType();
                if (superRawType != null && argTypes.length > 0) {
                    Class<?>[] superArgTypes = TypeUtils.resolveSuperConstructorArgTypes(superRawType, argTypes);
                    if (superArgTypes != null) {
                        for (int i = 0; i < args.length; i++) {
                            if (args[i] instanceof Lambda lambda &&
                                    superArgTypes[i].isInterface()) {
                                args[i] = lambda.asInterface(superArgTypes[i]);
                            }
                        }
                        argTypes = superArgTypes;
                    }
                }

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

            Constructor<?> constructor = TypeUtils.findConstructor(clazz, argTypes);
            return constructor.newInstance(args);
        } catch (EvaluationException e) {
            throw e;
        } catch (SecurityException e) {
            throw new EvaluationException(
                    "Permission denied for new instance: " + e.getMessage(),
                    ErrorCode.EVAL_PERMISSION_DENIED,
                    e,
                    node);
        } catch (Exception e) {
            throw new EvaluationException(
                    "Failed to create instance: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()),
                    ErrorCode.EVAL_INVALID_OPERATION,
                    e,
                    node);
        }
    }

    public static Object evaluateArrayLiteral(ArrayLiteralNode node, ExecutionContext context)
            throws EvaluationException {
        List<ASTNode> elements = node.getElements();
        Class<?> expectedType = node.getExpectedElementType();
        ASTNode arrayLengthNode = node.getArrayLength();

        if (expectedType != null) {
            // 对于 new int[3] {1, 2} 形式的数组初始化，
            // 我们需要创建指定长度的数组，并在元素不足时用默认值填充
            int arrayLength;
            if (arrayLengthNode != null) {
                // 解析并计算数组长度
                Object lengthValue = evaluate(arrayLengthNode, context);
                if (!(lengthValue instanceof Number)) {
                    throw new EvaluationException(
                            "Array size must be a number",
                            ErrorCode.EVAL_TYPE_MISMATCH,
                            node);
                }
                arrayLength = ((Number) lengthValue).intValue();
            } else {
                // 没有指定长度，使用元素的实际长度
                arrayLength = elements.size();
            }

            Object array = Array.newInstance(expectedType, arrayLength);

            for (int i = 0; i < arrayLength; i++) {
                if (i < elements.size()) {
                    Object value = evaluate(elements.get(i), context);

                    if (value != null && !TypeUtils.isAssignable(expectedType, value.getClass())) {
                        if (expectedType.isPrimitive()) {
                            value = TypeUtils.convertToPrimitive(value, expectedType, node);
                        } else {
                            throw new EvaluationException(
                                    "Type mismatch in array initializer: expected " + expectedType.getSimpleName() +
                                            ", but got " + value.getClass().getSimpleName() + " at index " + i,
                                    ErrorCode.EVAL_TYPE_MISMATCH,
                                    node);
                        }
                    }

                    Array.set(array, i, value);
                } else {
                    // 元素不足时，用默认值填充
                    Array.set(array, i, TypeUtils.getDefaultValue(expectedType));
                }
            }

            return array;
        }

        Object[] evaluated = new Object[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            evaluated[i] = evaluate(elements.get(i), context);
        }

        Class<?> inferredType = TypeUtils.inferArrayType(evaluated);
        if (inferredType != null && inferredType != Object.class) {
            Object array = Array.newInstance(inferredType, evaluated.length);
            for (int i = 0; i < evaluated.length; i++) {
                Object converted = TypeUtils.convertValue(evaluated[i], inferredType, node);
                Array.set(array, i, converted);
            }
            return array;
        }

        return evaluated;
    }

    public static Object evaluateMapLiteral(MapLiteralNode node, ExecutionContext context) throws EvaluationException {
        Map<Object, Object> map = new LinkedHashMap<>();

        for (Map.Entry<ASTNode, ASTNode> entry : node.getEntries().entrySet()) {
            Object key = evaluate(entry.getKey(), context);
            Object value = evaluate(entry.getValue(), context);
            map.put(key, value);
        }

        return map;
    }

    public static Object evaluateIf(IfNode node, ExecutionContext context) throws EvaluationException {
        Object condition = evaluate(node.getCondition(), context);

        if (TypeUtils.toBoolean(condition)) {
            return evaluate(node.getThenBlock(), context);
        } else if (node.getElseBlock() != null) {
            return evaluate(node.getElseBlock(), context);
        }

        return null;
    }

    public static Object evaluateWhile(WhileNode node, ExecutionContext context) throws EvaluationException {
        context.incrementLoopDepth();
        try {
            while (TypeUtils.toBoolean(evaluate(node.getCondition(), context))) {
                context.getScopeManager().enterScope();
                try {
                    evaluate(node.getBody(), context);
                } catch (ContinueException e) {
                } finally {
                    context.getScopeManager().exitScope();
                }
            }
        } catch (BreakException e) {
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
                } catch (ContinueException e) {
                } finally {
                    context.getScopeManager().exitScope();
                }
            } while (TypeUtils.toBoolean(evaluate(node.getCondition(), context)));
        } catch (BreakException e) {
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

            while (node.getCondition() == null || TypeUtils.toBoolean(evaluate(node.getCondition(), context))) {
                try {
                    evaluate(node.getBody(), context);
                } catch (ContinueException e) {
                }

                if (node.getUpdate() != null) {
                    evaluate(node.getUpdate(), context);
                }
            }
        } catch (BreakException e) {
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
                    false,
                    node);

            if (iterable instanceof Iterable) {
                for (Object item : (Iterable<?>) iterable) {
                    context.getScopeManager().setVariable(node.getItemName(), item, node);
                    try {
                        evaluate(node.getBody(), context);
                    } catch (ContinueException e) {
                    }
                }
            } else if (iterable != null && iterable.getClass().isArray()) {
                int length = Array.getLength(iterable);
                for (int i = 0; i < length; i++) {
                    Object item = Array.get(iterable, i);
                    context.getScopeManager().setVariable(node.getItemName(), item, node);
                    try {
                        evaluate(node.getBody(), context);
                    } catch (ContinueException e) {
                    }
                }
            }
        } catch (BreakException e) {
        } finally {
            context.decrementLoopDepth();
            context.getScopeManager().exitScope();
        }

        return null;
    }

    public static Object evaluateTernary(TernaryNode node, ExecutionContext context) throws EvaluationException {
        Object condition = evaluate(node.getCondition(), context);

        if (TypeUtils.toBoolean(condition)) {
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
            ScopeManager.Variable funcVar = context.getScopeManager().getVariable(funcName, node);

            if (funcVar != null && funcVar.getValue() != null) {
                Object func = funcVar.getValue();

                if (func instanceof Lambda) {
                    return ((Lambda) func).invoke(new Object[] { input });
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
                            if (Modifier.isStatic(method.getModifiers())) {
                                return method.invoke(null);
                            } else {
                                return method.invoke(input);
                            }
                        }
                    }

                    Method method = TypeUtils.findMethod(clazz, methodName, new Object[] { input });
                    if (method != null) {
                        method.setAccessible(true);
                        if (Modifier.isStatic(method.getModifiers())) {
                            return method.invoke(null, input);
                        } else {
                            return method.invoke(input);
                        }
                    }

                    throw new EvaluationException(
                            "Method not found for pipeline: " + clazz.getSimpleName() + "::" + methodName,
                            ErrorCode.METHOD_NOT_FOUND,
                            node);
                } catch (EvaluationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new EvaluationException(
                            "Failed to invoke method in pipeline: " + methodName + " - " + e.getMessage(),
                            ErrorCode.EVAL_METHOD_INVOCATION_FAILED,
                            node);
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
                    methodCall.getLocation());
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
            return lambda.invoke(new Object[] { input });
        }

        throw new EvaluationException(
                "Pipeline requires a function, got: " + functionNode.getClass().getSimpleName(),
                ErrorCode.EVAL_TYPE_MISMATCH,
                node);
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
                        ErrorCode.EVAL_INVALID_OPERATION,
                        node);
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
                Class<?> wrapperType = TypeUtils.getWrapperType(type);
                return wrapperType.isInstance(value);
            }

            return type.isInstance(value);
        } catch (Exception e) {
            throw new EvaluationException(
                    "Failed to resolve type: " + node.getTypeName(),
                    ErrorCode.EVAL_TYPE_MISMATCH,
                    node);
        }
    }

    public static Object evaluateCast(CastNode node, ExecutionContext context) throws EvaluationException {
        Object value = evaluate(node.getExpression(), context);

        Class<?> type = node.getTargetType();

        try {
            if (type == int.class)
                return TypeUtils.toInt(value);
            if (type == long.class)
                return TypeUtils.toLong(value);
            if (type == float.class)
                return TypeUtils.toFloat(value);
            if (type == double.class)
                return TypeUtils.toDouble(value);
            if (type == boolean.class)
                return TypeUtils.toBoolean(value);
            if (type == char.class)
                return TypeUtils.toChar(value);
            if (type == byte.class)
                return TypeUtils.toByte(value);
            if (type == short.class)
                return TypeUtils.toShort(value);
            if (type == String.class)
                return value == null ? "null" : value.toString();

            return type.cast(value);
        } catch (Exception e) {
            throw new EvaluationException(
                    "Failed to cast to: " + type.getName(),
                    ErrorCode.EVAL_TYPE_MISMATCH,
                    node);
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

    public static Object evaluateMethodReference(MethodReferenceNode node, ExecutionContext context)
            throws EvaluationException {
        ASTNode targetNode = node.getTarget();
        String methodName = node.getMethodName();

        Class<?> targetClass;
        Object targetInstance = null;
        boolean isStatic = false;

        if (targetNode instanceof ClassReferenceNode classRef) {
            try {
                String className = classRef.getResolvedClass().getName();
                targetClass = ClassResolver.findClassWithImportsOrFail(className, context.getClassLoader(),
                        context.getImports());
                isStatic = true;
            } catch (ClassNotFoundException e) {
                throw new EvaluationException(
                        "Class not found: " + classRef.getTypeName(),
                        ErrorCode.EVAL_CLASS_NOT_FOUND,
                        node);
            }
        } else if (targetNode instanceof VariableNode varNode) {
            String varName = varNode.getName();

            if (context.getScopeManager().hasVariable(varName)) {
                targetInstance = context.getScopeManager().getVariable(varName, node).getValue();
                targetClass = targetInstance != null ? targetInstance.getClass() : Object.class;
            } else {
                targetClass = context.getClassFinder().findClassWithImports(varName, context.getClassLoader(),
                        context.getImports());
                if (targetClass != null) {
                    isStatic = true;
                } else {
                    throw new EvaluationException(
                            "Cannot resolve method reference target: " + varName,
                            ErrorCode.EVAL_UNDEFINED_VARIABLE,
                            node);
                }
            }
        } else {
            targetInstance = evaluate(targetNode, context);
            targetClass = targetInstance != null ? targetInstance.getClass() : Object.class;
        }

        if (isStatic) {
            Method actualMethod = Arrays.stream(targetClass.getMethods())
                    .filter(method -> method.getName().equals(methodName))
                    .findFirst()
                    .orElse(null);
            if (actualMethod != null && !Modifier.isStatic(actualMethod.getModifiers())) {
                return MethodReference.createUnboundInstanceMethod(targetClass, methodName, node);
            }
        }

        return new MethodReference(targetClass, targetInstance, methodName, isStatic, node);
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
                        false,
                        node);
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
                                false,
                                node);
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
                    ErrorCode.EVAL_EXCEPTION_THROWN,
                    t,
                    node);
        }

        throw new EvaluationException("Cannot throw non-throwable: " + exception, ErrorCode.EVAL_INVALID_OPERATION,
                node);
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
                        ErrorCode.SCOPE_VARIABLE_NOT_FOUND,
                        node);
            }
        }
        return null;
    }

    public static Object evaluateClassDeclaration(ClassDeclarationNode node, ExecutionContext context)
            throws EvaluationException {
        DynamicClassGenerator generator = new DynamicClassGenerator(context);
        Class<?> generatedClass = generator.generateClass(node);

        String className = node.getClassName();
        context.registerCustomClass(className, generatedClass);
        context.getScopeManager().declareVariable(className, Class.class, generatedClass, false, node);

        return generatedClass;
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
            case "int":
                componentType = int.class;
                break;
            case "long":
                componentType = long.class;
                break;
            case "float":
                componentType = float.class;
                break;
            case "double":
                componentType = double.class;
                break;
            case "boolean":
                componentType = boolean.class;
                break;
            case "char":
                componentType = char.class;
                break;
            case "byte":
                componentType = byte.class;
                break;
            case "short":
                componentType = short.class;
                break;
            case "void":
                componentType = void.class;
                break;
            default:
                componentType = context.getClassFinder().findClassWithImports(baseType, context.getClassLoader(),
                        context.getImports());
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

    public static Object evaluateReturn(ReturnNode node, ExecutionContext context) throws EvaluationException {
        Object value = null;
        if (node.getValue() != null) {
            value = evaluate(node.getValue(), context);
        }
        throw new ReturnException(value);
    }

    public static Object evaluateBreak(BreakNode node, ExecutionContext context) {
        throw new BreakException();
    }

    public static Object evaluateContinue(ContinueNode node, ExecutionContext context) {
        throw new ContinueException();
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value.getClass().isArray()) {
            StringBuilder sb = new StringBuilder("[");
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(formatValue(Array.get(value, i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return String.valueOf(value);
    }
}
