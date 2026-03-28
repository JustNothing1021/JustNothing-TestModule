package com.justnothing.testmodule.command.functions.script_new.evaluator;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.GenericType;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.nodes.*;
import com.justnothing.testmodule.command.functions.script_new.exception.ErrorCode;
import com.justnothing.testmodule.command.functions.script_new.exception.EvaluationException;
import com.justnothing.testmodule.command.functions.script_new.exception.ReturnException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    
    public static Object evaluate(ASTNode node, ExecutionContext context) throws EvaluationException {
        if (node == null) {
            return null;
        }
        
        if (node instanceof BlockNode) {
            return evaluateBlock((BlockNode) node, context);
        }
        
        if (node instanceof LiteralNode) {
            return evaluateLiteral((LiteralNode) node);
        }
        
        if (node instanceof VariableNode) {
            return evaluateVariable((VariableNode) node, context);
        }
        
        if (node instanceof BinaryOpNode) {
            return evaluateBinaryOp((BinaryOpNode) node, context);
        }
        
        if (node instanceof UnaryOpNode) {
            return evaluateUnaryOp((UnaryOpNode) node, context);
        }
        
        if (node instanceof AssignmentNode) {
            return evaluateAssignment((AssignmentNode) node, context);
        }
        
        if (node instanceof FieldAssignmentNode) {
            return evaluateFieldAssignment((FieldAssignmentNode) node, context);
        }
        
        if (node instanceof ArrayAssignmentNode) {
            return evaluateArrayAssignment((ArrayAssignmentNode) node, context);
        }
        
        if (node instanceof MethodCallNode) {
            return evaluateMethodCall((MethodCallNode) node, context);
        }
        
        if (node instanceof FunctionCallNode) {
            return evaluateFunctionCall((FunctionCallNode) node, context);
        }
        
        if (node instanceof FieldAccessNode) {
            return evaluateFieldAccess((FieldAccessNode) node, context);
        }
        
        if (node instanceof ArrayAccessNode) {
            return evaluateArrayAccess((ArrayAccessNode) node, context);
        }
        
        if (node instanceof ClassReferenceNode) {
            return evaluateClassReference((ClassReferenceNode) node, context);
        }
        
        if (node instanceof NewArrayNode) {
            return evaluateNewArray((NewArrayNode) node, context);
        }
        
        if (node instanceof ArrayLiteralNode) {
            return evaluateArrayLiteral((ArrayLiteralNode) node, context);
        }
        
        if (node instanceof MapLiteralNode) {
            return evaluateMapLiteral((MapLiteralNode) node, context);
        }
        
        if (node instanceof IfNode) {
            return evaluateIf((IfNode) node, context);
        }
        
        if (node instanceof WhileNode) {
            return evaluateWhile((WhileNode) node, context);
        }
        
        if (node instanceof DoWhileNode) {
            return evaluateDoWhile((DoWhileNode) node, context);
        }
        
        if (node instanceof ForNode) {
            return evaluateFor((ForNode) node, context);
        }
        
        if (node instanceof ForEachNode) {
            return evaluateForEach((ForEachNode) node, context);
        }
        
        if (node instanceof TernaryNode) {
            return evaluateTernary((TernaryNode) node, context);
        }
        
        if (node instanceof InstanceofNode) {
            return evaluateInstanceof((InstanceofNode) node, context);
        }
        
        if (node instanceof CastNode) {
            return evaluateCast((CastNode) node, context);
        }
        
        if (node instanceof LambdaNode) {
            return evaluateLambda((LambdaNode) node, context);
        }
        
        if (node instanceof ReturnNode) {
            return evaluateReturn((ReturnNode) node, context);
        }
        
        if (node instanceof MethodReferenceNode) {
            return evaluateMethodReference((MethodReferenceNode) node, context);
        }
        
        if (node instanceof TryNode) {
            return evaluateTry((TryNode) node, context);
        }
        
        if (node instanceof ThrowNode) {
            return evaluateThrow((ThrowNode) node, context);
        }
        
        if (node instanceof SwitchNode) {
            return evaluateSwitch((SwitchNode) node, context);
        }
        
        if (node instanceof ImportNode) {
            return null;
        }
        
        if (node instanceof DeleteNode) {
            return evaluateDelete((DeleteNode) node, context);
        }
        
        if (node instanceof ClassDeclarationNode) {
            return evaluateClassDeclaration((ClassDeclarationNode) node, context);
        }
        
        if (node instanceof ConstructorCallNode) {
            return evaluateConstructorCall((ConstructorCallNode) node, context);
        }
        
        throw new EvaluationException(
            "Unsupported AST node type: " + node.getClass().getSimpleName(),
            node.getLocation(),
            ErrorCode.EVAL_INVALID_OPERATION
        );
    }
    
    private static Object evaluateBlock(BlockNode node, ExecutionContext context) throws EvaluationException {
        Object result = null;
        boolean isGlobalScope = context.getScopeManager().getCurrentLevel() == 1;
        
        if (!isGlobalScope) {
            context.getScopeManager().enterScope();
        }
        
        try {
            for (ASTNode stmt : node.getStatements()) {
                result = evaluate(stmt, context);
            }
        } finally {
            if (!isGlobalScope) {
                context.getScopeManager().exitScope();
            }
        }
        return result;
    }
    
    private static Object evaluateLiteral(LiteralNode node) {
        return node.getValue();
    }
    
    private static Object evaluateVariable(VariableNode node, ExecutionContext context) throws EvaluationException {
        String varName = node.getName();
        ScopeManager.Variable variable = context.getScopeManager().getVariable(varName);
        if (variable == null) {
            throw new EvaluationException("Undefined variable: " + varName, node.getLocation(), ErrorCode.EVAL_UNDEFINED_VARIABLE);
        }
        return variable.getValue();
    }
    
    private static Object evaluateBinaryOp(BinaryOpNode node, ExecutionContext context) throws EvaluationException {
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
                if (left instanceof Object[] && right instanceof Object[]) {
                    // 合并数组
                    Object[] leftArr = (Object[]) left;
                    Object[] rightArr = (Object[]) right;
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
            case EQUAL:
                return left == null ? right == null : left.equals(right);
            case NOT_EQUAL:
                return left == null ? right != null : !left.equals(right);
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
            case LOGICAL_AND:
                return toBoolean(left) && toBoolean(right);
            case LOGICAL_OR:
                return toBoolean(left) || toBoolean(right);
            case BITWISE_AND:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() & ((Number) right).longValue();
                }
                break;
            case BITWISE_OR:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() | ((Number) right).longValue();
                }
                break;
            case BITWISE_XOR:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).longValue() ^ ((Number) right).longValue();
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
    
    private static boolean isIntegerType(Number n) {
        return n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte;
    }
    
    private static boolean isFloatingType(Number n) {
        return n instanceof Double || n instanceof Float;
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
    
    private static Object evaluateUnaryOp(UnaryOpNode node, ExecutionContext context) throws EvaluationException {
        UnaryOpNode.Operator op = node.getOperator();
        
        switch (op) {
            case NEGATIVE: {
                Object operand = evaluate(node.getOperand(), context);
                if (operand instanceof Number) {
                    Number n = (Number) operand;
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
            case BITWISE_NOT: {
                Object operand = evaluate(node.getOperand(), context);
                if (operand instanceof Number) {
                    Number n = (Number) operand;
                    if (n instanceof Long) {
                        return ~n.longValue();
                    }
                    return ~n.intValue();
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
        
        if (!(currentValue instanceof Number)) {
            throw new EvaluationException(
                "Increment/decrement operator requires a numeric variable",
                node.getLocation(),
                ErrorCode.EVAL_INVALID_OPERATION
            );
        }
        
        Number numValue = (Number) currentValue;
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
    
    private static Object evaluateAssignment(AssignmentNode node, ExecutionContext context) throws EvaluationException {
        ASTNode valueNode = node.getValue();
        
        if (node.isDeclaration() && valueNode instanceof LambdaNode && node.getDeclaredClass() != null) {
            Class<?> declaredClass = node.getDeclaredClass();
            if (declaredClass.isInterface()) {
                ((LambdaNode) valueNode).setFunctionalInterfaceType(declaredClass);
            }
        }
        
        Object value;
        if (valueNode != null) {
            if (node.isDeclaration() && valueNode instanceof LambdaNode) {
                Class<?> declaredType = node.getDeclaredClass() != null ? node.getDeclaredClass() : Object.class;
                context.getScopeManager().declareVariable(
                    node.getVariableName(), 
                    declaredType,
                    null,
                    false
                );
                value = evaluate(valueNode, context);
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
            value = convertValue(value, declaredType, node.getLocation());
            
            context.getScopeManager().declareVariable(
                node.getVariableName(), 
                declaredType,
                value, 
                false
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
        
        if (value instanceof Number) {
            Number num = (Number) value;
            
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
    
    private static Object evaluateFieldAssignment(FieldAssignmentNode node, ExecutionContext context) throws EvaluationException {
        Object target = evaluate(node.getTarget(), context);
        Object value = evaluate(node.getValue(), context);
        
        try {
            Field field = target.getClass().getDeclaredField(node.getFieldName());
            field.setAccessible(true);
            field.set(target, value);
            return value;
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to set field: " + node.getFieldName() + " - " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_FIELD_ACCESS_FAILED
            );
        }
    }
    
    private static Object evaluateArrayAssignment(ArrayAssignmentNode node, ExecutionContext context) throws EvaluationException {
        Object array = evaluate(node.getArray(), context);
        Object index = evaluate(node.getIndex(), context);
        Object value = evaluate(node.getValue(), context);
        
        int idx = toInt(index);
        Array.set(array, idx, value);
        return value;
    }
    
    private static Object evaluateFunctionCall(FunctionCallNode node, ExecutionContext context) throws EvaluationException {
        String functionName = node.getFunctionName();
        
        Object[] args = new Object[node.getArguments().size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = evaluate(node.getArguments().get(i), context);
        }
        
        if (context.hasBuiltin(functionName)) {
            List<Object> argList = new ArrayList<>();
            for (Object arg : args) {
                argList.add(arg);
            }
            return context.callBuiltin(functionName, argList);
        }
        
        if (context.getScopeManager().hasVariable(functionName)) {
            Object func = context.getScopeManager().getVariable(functionName).getValue();
            if (func instanceof Lambda) {
                return ((Lambda) func).invoke(args);
            }
        }
        
        throw new EvaluationException(
            "Unknown function: " + functionName,
            node.getLocation(),
            ErrorCode.EVAL_UNDEFINED_VARIABLE
        );
    }
    
    private static Object evaluateMethodCall(MethodCallNode node, ExecutionContext context) throws EvaluationException {
        Object target = evaluate(node.getTarget(), context);
        String methodName = node.getMethodName();
        
        Object[] args = new Object[node.getArguments().size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = evaluate(node.getArguments().get(i), context);
        }
        
        try {
            if (target instanceof Lambda) {
                return ((Lambda) target).invoke(args);
            }
            
            if (target instanceof Class) {
                Class<?> targetClass = (Class<?>) target;
                
                Method method = findMethod(targetClass, methodName, args);
                if (method != null) {
                    method.setAccessible(true);
                    Object[] invokeArgs = prepareInvokeArguments(method, args);
                    return method.invoke(null, invokeArgs);
                }
                
                throw new EvaluationException(
                    "Method not found: " + methodName,
                    node.getLocation(),
                    ErrorCode.METHOD_NOT_FOUND
                );
            }
            
            Method method = findMethod(target.getClass(), methodName, args);
            if (method != null) {
                method.setAccessible(true);
                Object[] invokeArgs = prepareInvokeArguments(method, args);
                return method.invoke(target, invokeArgs);
            }
            
            throw new EvaluationException(
                "Method not found: " + methodName,
                node.getLocation(),
                ErrorCode.METHOD_NOT_FOUND
            );
        } catch (EvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to call method: " + methodName + " - " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_METHOD_INVOCATION_FAILED
            );
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
        
        for (int i = 0; i < fixedParamCount; i++) {
            invokeArgs[i] = args[i];
        }
        
        int varArgCount = args.length - fixedParamCount;
        
        if (varArgCount == 1) {
            Object lastArg = args[fixedParamCount];
            
            if (lastArg != null && lastArg.getClass().isArray() &&
                    varArgsComponentType.isAssignableFrom(lastArg.getClass().getComponentType())) {
                invokeArgs[fixedParamCount] = lastArg;
            } else {
                Object varArgArray = Array.newInstance(varArgsComponentType, 1);
                Array.set(varArgArray, 0, lastArg);
                invokeArgs[fixedParamCount] = varArgArray;
            }
        } else if (varArgCount == 0) {
            invokeArgs[fixedParamCount] = Array.newInstance(varArgsComponentType, 0);
        } else {
            Object varArgArray = Array.newInstance(varArgsComponentType, varArgCount);
            for (int i = 0; i < varArgCount; i++) {
                Array.set(varArgArray, i, args[fixedParamCount + i]);
            }
            invokeArgs[fixedParamCount] = varArgArray;
        }
        
        return invokeArgs;
    }
    
    private static Object evaluateFieldAccess(FieldAccessNode node, ExecutionContext context) throws EvaluationException {
        Object target = evaluate(node.getTarget(), context);
        
        try {
            Class<?> targetClass;
            if (target instanceof Class) {
                targetClass = (Class<?>) target;
            } else {
                targetClass = target.getClass();
            }
            
            Field field = targetClass.getDeclaredField(node.getFieldName());
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to access field: " + node.getFieldName() + " - " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_FIELD_ACCESS_FAILED
            );
        }
    }
    
    private static Object evaluateArrayAccess(ArrayAccessNode node, ExecutionContext context) throws EvaluationException {
        Object array = evaluate(node.getArray(), context);
        Object index = evaluate(node.getIndex(), context);
        
        return Array.get(array, toInt(index));
    }
    
    private static Object evaluateClassReference(ClassReferenceNode node, ExecutionContext context) throws EvaluationException {
        try {
            return node.getType().getRawType();
        } catch (Exception e) {
            throw new EvaluationException(
                "Class not found: " + node.getType().getTypeName(),
                node.getLocation(),
                ErrorCode.EVAL_TYPE_MISMATCH
            );
        }
    }
    
    private static Object evaluateNewArray(NewArrayNode node, ExecutionContext context) throws EvaluationException {
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
    
    private static Object evaluateConstructorCall(ConstructorCallNode node, ExecutionContext context) throws EvaluationException {
        try {
            GenericType type = node.getType();
            List<ASTNode> argNodes = node.getArguments();
            
            Object[] args = new Object[argNodes.size()];
            Class<?>[] argTypes = new Class<?>[argNodes.size()];
            
            for (int i = 0; i < argNodes.size(); i++) {
                args[i] = evaluate(argNodes.get(i), context);
                argTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            
            Class<?> clazz = type.getRawType();
            
            String originalTypeName = type.getOriginalTypeName();
            if (originalTypeName != null && context.hasCustomClass(originalTypeName)) {
                clazz = context.getCustomClass(originalTypeName);
            }
            
            Constructor<?> constructor = findConstructor(clazz, argTypes);
            return constructor.newInstance(args);
        } catch (EvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to create instance: " + e.getMessage(),
                node.getLocation(),
                ErrorCode.EVAL_INVALID_OPERATION
            );
        }
    }
    
    private static Constructor<?> findConstructor(Class<?> clazz, Class<?>[] argTypes) throws NoSuchMethodException {
        try {
            return clazz.getDeclaredConstructor(argTypes);
        } catch (NoSuchMethodException ignored) {
        }
        
        Constructor<?> bestMatch = null;
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (isApplicableArgs(constructor.getParameterTypes(), argTypes, constructor.isVarArgs())) {
                bestMatch = constructor;
                break;
            }
        }
        
        if (bestMatch != null) {
            return bestMatch;
        }
        
        throw new NoSuchMethodException("No suitable constructor found");
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
            return true;
        } else {
            if (paramTypes.length != argTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!isAssignable(paramTypes[i], argTypes[i])) {
                    return false;
                }
            }
            return true;
        }
    }
    
    private static boolean isAssignable(Class<?> targetType, Class<?> sourceType) {
        if (sourceType == null) {
            return !targetType.isPrimitive();
        }
        if (targetType.isPrimitive()) {
            if (sourceType.isPrimitive()) {
                return isPrimitiveAssignable(targetType, sourceType);
            }
            Class<?> wrapper = getWrapperClass(targetType);
            return wrapper != null && wrapper.isAssignableFrom(sourceType);
        }
        return targetType.isAssignableFrom(sourceType);
    }
    
    private static boolean isPrimitiveAssignable(Class<?> targetType, Class<?> sourceType) {
        if (targetType == sourceType) {
            return true;
        }
        if (targetType == byte.class) {
            return sourceType == short.class || sourceType == int.class || sourceType == long.class;
        }
        if (targetType == short.class) {
            return sourceType == int.class || sourceType == long.class;
        }
        if (targetType == int.class) {
            return sourceType == long.class;
        }
        if (targetType == long.class) {
            return false;
        }
        if (targetType == float.class) {
            return sourceType == double.class;
        }
        if (targetType == double.class) {
            return false;
        }
        if (targetType == char.class) {
            return sourceType == int.class || sourceType == long.class;
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
    
    private static Object evaluateArrayLiteral(ArrayLiteralNode node, ExecutionContext context) throws EvaluationException {
        List<ASTNode> elements = node.getElements();
        Object[] array = new Object[elements.size()];
        
        for (int i = 0; i < elements.size(); i++) {
            array[i] = evaluate(elements.get(i), context);
        }
        
        return array;
    }
    
    private static Object evaluateMapLiteral(MapLiteralNode node, ExecutionContext context) throws EvaluationException {
        Map<Object, Object> map = new java.util.LinkedHashMap<>();
        
        for (Map.Entry<ASTNode, ASTNode> entry : node.getEntries().entrySet()) {
            Object key = evaluate(entry.getKey(), context);
            Object value = evaluate(entry.getValue(), context);
            map.put(key, value);
        }
        
        return map;
    }
    
    private static Object evaluateIf(IfNode node, ExecutionContext context) throws EvaluationException {
        Object condition = evaluate(node.getCondition(), context);
        
        if (toBoolean(condition)) {
            return evaluate(node.getThenBlock(), context);
        } else if (node.getElseBlock() != null) {
            return evaluate(node.getElseBlock(), context);
        }
        
        return null;
    }
    
    private static Object evaluateWhile(WhileNode node, ExecutionContext context) throws EvaluationException {
        context.incrementLoopDepth();
        try {
            while (toBoolean(evaluate(node.getCondition(), context))) {
                evaluate(node.getBody(), context);
            }
        } finally {
            context.decrementLoopDepth();
        }
        return null;
    }
    
    private static Object evaluateDoWhile(DoWhileNode node, ExecutionContext context) throws EvaluationException {
        context.incrementLoopDepth();
        try {
            do {
                evaluate(node.getBody(), context);
            } while (toBoolean(evaluate(node.getCondition(), context)));
        } finally {
            context.decrementLoopDepth();
        }
        return null;
    }
    
    private static Object evaluateFor(ForNode node, ExecutionContext context) throws EvaluationException {
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
    
    private static Object evaluateForEach(ForEachNode node, ExecutionContext context) throws EvaluationException {
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
    
    private static Object evaluateTernary(TernaryNode node, ExecutionContext context) throws EvaluationException {
        Object condition = evaluate(node.getCondition(), context);
        
        if (toBoolean(condition)) {
            return evaluate(node.getThenExpr(), context);
        } else {
            return evaluate(node.getElseExpr(), context);
        }
    }
    
    private static Object evaluateInstanceof(InstanceofNode node, ExecutionContext context) throws EvaluationException {
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
    
    private static Object evaluateCast(CastNode node, ExecutionContext context) throws EvaluationException {
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
    
    private static Object evaluateLambda(LambdaNode node, ExecutionContext context) {
        Lambda func = new Lambda(node, context);
        
        Class<?> targetInterface = node.getFunctionalInterfaceType();
        if (targetInterface != null) {
            return func.asInterface(targetInterface);
        }
        
        return func;
    }
    
    private static Object evaluateMethodReference(MethodReferenceNode node, ExecutionContext context) {
        return node;
    }
    
    private static Object evaluateTry(TryNode node, ExecutionContext context) throws EvaluationException {
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
    
    private static Object evaluateThrow(ThrowNode node, ExecutionContext context) throws EvaluationException {
        Object exception = evaluate(node.getExpression(), context);
        
        if (exception instanceof Throwable) {
            Throwable t = (Throwable) exception;
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
    
    private static Object evaluateSwitch(SwitchNode node, ExecutionContext context) throws EvaluationException {
        Object value = evaluate(node.getExpression(), context);
        
        for (CaseNode caseNode : node.getCases()) {
            if (caseNode.getValue() == null) {
                continue;
            }
            Object caseValue = evaluate(caseNode.getValue(), context);
            if (value == null ? caseValue == null : value.equals(caseValue)) {
                for (ASTNode stmt : caseNode.getStatements()) {
                    evaluate(stmt, context);
                }
                return null;
            }
        }
        
        for (CaseNode caseNode : node.getCases()) {
            if (caseNode.getValue() == null) {
                for (ASTNode stmt : caseNode.getStatements()) {
                    evaluate(stmt, context);
                }
                return null;
            }
        }
        
        return null;
    }
    
    private static Object evaluateDelete(DeleteNode node, ExecutionContext context) throws EvaluationException {
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
    
    private static Object evaluateClassDeclaration(ClassDeclarationNode node, ExecutionContext context) throws EvaluationException {
        DynamicClassGenerator generator = new DynamicClassGenerator(context);
        Class<?> generatedClass = generator.generateClass(node);
        
        String className = node.getClassName();
        context.registerCustomClass(className, generatedClass);
        context.getScopeManager().declareVariable(className, Class.class, generatedClass, false);
        
        return generatedClass;
    }
    
    private static Method findMethod(Class<?> clazz, String name, Object[] args) {
        List<Method> candidates = new ArrayList<>();
        
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
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
                if (!isVarArgs && paramTypes.length == args.length) {
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
        
        if (varArgsCandidate != null) {
            return varArgsCandidate;
        }
        
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                Class<?>[] paramTypes = method.getParameterTypes();
                boolean isVarArgs = method.isVarArgs();
                
                if (isApplicableArgs(paramTypes, args, isVarArgs)) {
                    return method;
                }
            }
        }
        
        return null;
    }
    
    private static int computeMatchScore(Class<?>[] paramTypes, Object[] args) {
        int score = 0;
        for (int i = 0; i < paramTypes.length; i++) {
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
        int argCount = args.length;
        
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
            
            return true;
        } else {
            if (paramCount != argCount) {
                return false;
            }
            
            for (int i = 0; i < paramCount; i++) {
                if (!isAssignable(paramTypes[i], args[i])) {
                    return false;
                }
            }
            
            return true;
        }
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
    
    private static Constructor<?> findConstructor(Class<?> clazz, Object[] args) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.getParameterCount() == args.length) {
                return constructor;
            }
        }
        
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == args.length) {
                return constructor;
            }
        }
        
        return null;
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
                componentType = ClassFinder.findClassWithImports(baseType, context.getClassLoader(), context.getImports());
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
    
    private static Object evaluateReturn(ReturnNode node, ExecutionContext context) throws EvaluationException {
        Object value = null;
        if (node.getValue() != null) {
            value = evaluate(node.getValue(), context);
        }
        throw new ReturnException(value);
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
