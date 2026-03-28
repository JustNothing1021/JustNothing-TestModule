package com.justnothing.testmodule.command.functions.script_new.evaluator;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.GenericType;
import com.justnothing.testmodule.command.functions.script_new.ast.nodes.*;
import com.justnothing.testmodule.command.functions.script_new.exception.ErrorCode;
import com.justnothing.testmodule.command.functions.script_new.exception.EvaluationException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
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
        context.getScopeManager().enterScope();
        try {
            for (ASTNode stmt : node.getStatements()) {
                result = evaluate(stmt, context);
            }
        } finally {
            context.getScopeManager().exitScope();
        }
        return result;
    }
    
    private static Object evaluateLiteral(LiteralNode node) {
        return node.getValue();
    }
    
    private static Object evaluateVariable(VariableNode node, ExecutionContext context) throws EvaluationException {
        String varName = node.getName();
        Object value = context.getScopeManager().getVariable(varName);
        if (value == null && !context.getScopeManager().hasVariable(varName)) {
            throw new EvaluationException("Undefined variable: " + varName, node.getLocation(), ErrorCode.EVAL_UNDEFINED_VARIABLE);
        }
        return value;
    }
    
    private static Object evaluateBinaryOp(BinaryOpNode node, ExecutionContext context) throws EvaluationException {
        Object left = evaluate(node.getLeft(), context);
        Object right = evaluate(node.getRight(), context);
        BinaryOpNode.Operator op = node.getOperator();
        
        switch (op) {
            case ADD:
                if (left instanceof String || right instanceof String) {
                    return String.valueOf(left) + String.valueOf(right);
                }
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() + ((Number) right).doubleValue();
                }
                break;
            case SUBTRACT:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() - ((Number) right).doubleValue();
                }
                break;
            case MULTIPLY:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() * ((Number) right).doubleValue();
                }
                break;
            case DIVIDE:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() / ((Number) right).doubleValue();
                }
                break;
            case MODULO:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() % ((Number) right).doubleValue();
                }
                break;
            case EQUAL:
                return left == null ? right == null : left.equals(right);
            case NOT_EQUAL:
                return left == null ? right != null : !left.equals(right);
            case LESS_THAN:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() < ((Number) right).doubleValue();
                }
                break;
            case LESS_THAN_OR_EQUAL:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() <= ((Number) right).doubleValue();
                }
                break;
            case GREATER_THAN:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() > ((Number) right).doubleValue();
                }
                break;
            case GREATER_THAN_OR_EQUAL:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() >= ((Number) right).doubleValue();
                }
                break;
            case LOGICAL_AND:
                return toBoolean(left) && toBoolean(right);
            case LOGICAL_OR:
                return toBoolean(left) || toBoolean(right);
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
    
    private static Object evaluateUnaryOp(UnaryOpNode node, ExecutionContext context) throws EvaluationException {
        Object operand = evaluate(node.getOperand(), context);
        UnaryOpNode.Operator op = node.getOperator();
        
        switch (op) {
            case NEGATIVE:
                if (operand instanceof Number) {
                    return -((Number) operand).doubleValue();
                }
                break;
            case LOGICAL_NOT:
                return !toBoolean(operand);
            case BITWISE_NOT:
                if (operand instanceof Number) {
                    return ~((Number) operand).intValue();
                }
                break;
            default:
                break;
        }
        
        throw new EvaluationException(
            "Invalid unary operation: " + op.getSymbol(),
            node.getLocation(),
            ErrorCode.EVAL_INVALID_OPERATION
        );
    }
    
    private static Object evaluateAssignment(AssignmentNode node, ExecutionContext context) throws EvaluationException {
        Object value = evaluate(node.getValue(), context);
        
        if (node.isDeclaration()) {
            Class<?> type = node.getDeclaredClass() != null ? node.getDeclaredClass() : Object.class;
            context.getScopeManager().declareVariable(
                node.getVariableName(), 
                type,
                value, 
                false
            );
        } else {
            context.getScopeManager().setVariable(node.getVariableName(), value);
        }
        
        return value;
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
    
    private static Object evaluateMethodCall(MethodCallNode node, ExecutionContext context) throws EvaluationException {
        Object target = node.getTarget() != null ? evaluate(node.getTarget(), context) : null;
        String methodName = node.getMethodName();
        
        Object[] args = new Object[node.getArguments().size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = evaluate(node.getArguments().get(i), context);
        }
        
        try {
            if (target == null) {
                throw new EvaluationException("Cannot call method on null target", node.getLocation(), ErrorCode.EVAL_NULL_POINTER);
            }
            
            if (target instanceof Class) {
                Constructor<?> constructor = findConstructor((Class<?>) target, args);
                if (constructor != null) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                }
            }
            
            Method method = findMethod(target.getClass(), methodName, args);
            if (method != null) {
                method.setAccessible(true);
                return method.invoke(target, args);
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
    
    private static Object evaluateFieldAccess(FieldAccessNode node, ExecutionContext context) throws EvaluationException {
        Object target = evaluate(node.getTarget(), context);
        
        try {
            Field field = target.getClass().getDeclaredField(node.getFieldName());
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
            if (iterable instanceof Iterable) {
                for (Object item : (Iterable<?>) iterable) {
                    context.getScopeManager().declareVariable(
                        node.getItemName(), 
                        Object.class,
                        item, 
                        false
                    );
                    evaluate(node.getBody(), context);
                }
            } else if (iterable != null && iterable.getClass().isArray()) {
                int length = Array.getLength(iterable);
                for (int i = 0; i < length; i++) {
                    Object item = Array.get(iterable, i);
                    context.getScopeManager().declareVariable(
                        node.getItemName(), 
                        Object.class,
                        item, 
                        false
                    );
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
        
        try {
            Class<?> type = resolveType(node.getTypeName(), context);
            return type.isInstance(value);
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to resolve type: " + node.getTypeName(),
                node.getLocation(),
                ErrorCode.EVAL_TYPE_MISMATCH
            );
        }
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
        return node;
    }
    
    private static Object evaluateMethodReference(MethodReferenceNode node, ExecutionContext context) {
        return node;
    }
    
    private static Object evaluateTry(TryNode node, ExecutionContext context) throws EvaluationException {
        try {
            return evaluate(node.getTryBlock(), context);
        } catch (Exception e) {
            for (CatchClause catchClause : node.getCatchClauses()) {
                for (Class<?> exceptionType : catchClause.getExceptionTypes()) {
                    if (exceptionType.isInstance(e)) {
                        context.getScopeManager().enterScope();
                        context.getScopeManager().declareVariable(
                            catchClause.getVariableName(),
                            exceptionType,
                            e,
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
            if (node.getFinallyBlock() != null) {
                evaluate(node.getFinallyBlock(), context);
            }
        }
    }
    
    private static Object evaluateThrow(ThrowNode node, ExecutionContext context) throws EvaluationException {
        Object exception = evaluate(node.getExpression(), context);
        
        if (exception instanceof Throwable) {
            throw new EvaluationException(
                ((Throwable) exception).getMessage(), 
                node.getLocation(), 
                ErrorCode.EVAL_INVALID_OPERATION,
                (Throwable) exception
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
            context.getScopeManager().exitScope();
            context.getScopeManager().enterScope();
        } else {
            throw new EvaluationException(
                "Delete single variable not supported",
                node.getLocation(),
                ErrorCode.EVAL_INVALID_OPERATION
            );
        }
        return null;
    }
    
    private static Object evaluateClassDeclaration(ClassDeclarationNode node, ExecutionContext context) throws EvaluationException {
        DynamicClassGenerator generator = new DynamicClassGenerator(context);
        Class<?> generatedClass = generator.generateClass(node);
        
        String className = node.getClassName();
        context.getScopeManager().declareVariable(className, Class.class, generatedClass, false);
        
        return generatedClass;
    }
    
    private static Method findMethod(Class<?> clazz, String name, Object[] args) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name) && 
                method.getParameterCount() == args.length) {
                return method;
            }
        }
        
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name) && 
                method.getParameterCount() == args.length) {
                return method;
            }
        }
        
        return null;
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
}
