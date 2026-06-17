package com.justnothing.engine.eval;

import com.justnothing.engine.ast.ASTNode;
import com.justnothing.engine.ast.OperatorCallback;
import com.justnothing.engine.ast.nodes.*;
import com.justnothing.engine.ast.visitor.ASTVisitor;
import com.justnothing.engine.builtins.Lambda;
import com.justnothing.engine.builtins.MethodReference;
import com.justnothing.engine.codegen.DynamicClassGenerator;
import com.justnothing.engine.exception.BreakException;
import com.justnothing.engine.exception.ContinueException;
import com.justnothing.engine.exception.ErrorCode;
import com.justnothing.engine.exception.LabeledBreakException;
import com.justnothing.engine.exception.ReturnException;
import com.justnothing.engine.parser.OperatorRegistry;
import com.justnothing.engine.parser.ParseContext;
import com.justnothing.engine.security.SecurityGate;
import com.justnothing.engine.util.CastUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Evaluator implements ASTVisitor<Value> {

    private final EvalContext evalContext;
    private final ParseContext parseContext;
    private static int anonSeq = 0;


    public EvalContext getEvalContext() {
        return evalContext;
    }

    public Evaluator(EvalContext evalContext, ParseContext parseContext) {
        this.evalContext = evalContext;
        this.parseContext = parseContext;
    }

    public Value evaluate(ASTNode node) {
        return node.accept(this);
    }

    public List<Value> evaluateAll(List<ASTNode> nodes) {
        List<Value> results = new ArrayList<>();
        for (ASTNode node : nodes) {
            try {
                results.add(evaluate(node));
            } catch (ReturnException e) {
                results.add((Value) e.getValue());
                break;
            }
        }
        return results;
    }

    public Value visit(ASTNode node) {
        if (node instanceof LiteralNode n) return visitLiteral(n);
        if (node instanceof VariableNode n) return visitVariable(n);
        if (node instanceof BinaryOpNode n) return visitBinaryOp(n);
        if (node instanceof UnaryOpNode n) return visitUnaryOp(n);
        if (node instanceof AssignmentNode n) return visitAssignment(n);
        if (node instanceof VarDeclNode n) return visitVarDecl(n);
        if (node instanceof MethodCallNode n) return visitMethodCall(n);
        if (node instanceof FieldAccessNode n) return visitFieldAccess(n);
        if (node instanceof ConstructorCallNode n) return visitConstructorCall(n);
        if (node instanceof TernaryNode n) return visitTernary(n);
        if (node instanceof ArrayAccessNode n) return visitArrayAccess(n);
        if (node instanceof ArrayAssignmentNode n) return visitArrayAssignment(n);
        if (node instanceof ArrayLiteralNode n) return visitArrayLiteral(n);
        if (node instanceof NewArrayNode n) return visitNewArray(n);
        if (node instanceof CastNode n) return visitCast(n);
        if (node instanceof InstanceofNode n) return visitInstanceof(n);
        if (node instanceof PipelineNode n) return visitPipeline(n);
        if (node instanceof BlockNode n) return visitBlock(n);
        if (node instanceof IfNode n) return visitIf(n);
        if (node instanceof WhileNode n) return visitWhile(n);
        if (node instanceof ForNode n) return visitFor(n);
        if (node instanceof ForEachNode n) return visitForEach(n);
        if (node instanceof SwitchNode n) return visitSwitch(n);
        if (node instanceof ReturnNode n) return visitReturn(n);
        if (node instanceof BreakNode n) return visitBreak(n);
        if (node instanceof ContinueNode n) return visitContinue(n);
        if (node instanceof LambdaNode n) return visitLambda(n);
        if (node instanceof FunctionCallNode n) return visitFunctionCall(n);
        if (node instanceof AsyncNode n) return visitAsync(n);
        if (node instanceof AwaitNode n) return visitAwait(n);
        if (node instanceof MapLiteralNode n) return visitMapLiteral(n);
        if (node instanceof InterpolatedStringNode n) return visitInterpolatedString(n);
        if (node instanceof FieldAssignmentNode n) return visitFieldAssignment(n);
        if (node instanceof MethodReferenceNode n) return visitMethodReference(n);
        if (node instanceof SafeFieldAccessNode n) return visitSafeFieldAccess(n);
        if (node instanceof SafeMethodCallNode n) return visitSafeMethodCall(n);
        if (node instanceof ThrowNode n) return visitThrow(n);
        if (node instanceof DeleteNode n) return visitDelete(n);
        if (node instanceof LabeledStatementNode n) return visitLabeledStatement(n);
        if (node instanceof ImportNode || node instanceof UsingAliasNode
                || node instanceof UsingStaticNode || node instanceof ClassDeclarationNode) {
            return Value.VoidValue.INSTANCE;
        }
        if (node instanceof FunctionDefNode n) return visitFunctionDef(n);
        if (node instanceof DoWhileNode n) return visitDoWhile(n);
        if (node instanceof TryNode n) return visitTry(n);
        if (node instanceof ClassReferenceNode n) return visitClassReference(n);
        throw new EvalException("Unsupported node: " + node.getClass().getSimpleName(), ErrorCode.EVAL_INVALID_OPERATION);
    }

    private Value visitLiteral(LiteralNode node) {
        Object value = node.getValue();
        Class<?> type = node.getType();
        if (value == null) return Value.NullValue.INSTANCE;
        if (type == void.class) return Value.VoidValue.INSTANCE;
        if (value instanceof String s) return new Value.StringValue(s);
        if (value instanceof Integer i) return new Value.IntValue(i);
        if (value instanceof Long l) return new Value.LongValue(l);
        if (value instanceof Double d) return new Value.DoubleValue(d);
        if (value instanceof Boolean b) return new Value.BooleanValue(b);
        if (value instanceof Character c) return new Value.CharValue(c);
        return Value.of(value);
    }

    private Value visitVariable(VariableNode node) {
        String name = node.getName();
        if (evalContext.hasVariable(name)) {
            return evalContext.getVariable(name);
        }
        if (node.isFieldAccess() && node.getDeclaredType() != null) {
            return Value.NullValue.INSTANCE;
        }
        throw new EvalException("Undefined variable: " + name, ErrorCode.EVAL_UNDEFINED_VARIABLE);
    }

    private Value visitBinaryOp(BinaryOpNode node) {
        Value left = evaluate(node.getLeft());
        if (node.getOperator() == BinaryOpNode.Operator.NULL_COALESCING) {
            if (!(left instanceof Value.NullValue)) return left;
            return evaluate(node.getRight());
        }
        if (node.getOperator() == BinaryOpNode.Operator.ELVIS) {
            return left.isTruthy() ? left : evaluate(node.getRight());
        }
        Value right = evaluate(node.getRight());
        // 自定义运算符重载：优先于内建运算（缓存回调避免重复查找）
        OperatorCallback cached = node.getOperatorCallback();
        if (cached != null) {
            return cached.call(left, right);
        }
        Value customResult = tryCustomBinaryOp(node.getOperator(), left, right);
        if (customResult != null) {
            cacheOperatorCallback(node, left, right);
            return customResult;
        }
        return switch (node.getOperator()) {
            case ADD -> binaryAdd(left, right);
            case SUBTRACT -> binaryNumericOp(left, right, (a, b) -> new Value.DoubleValue(a - b), (a, b) -> new Value.LongValue(a - b), (a, b) -> new Value.IntValue(a - b));
            case MULTIPLY -> binaryNumericOp(left, right, (a, b) -> new Value.DoubleValue(a * b), (a, b) -> new Value.LongValue(a * b), (a, b) -> new Value.IntValue(a * b));
            case DIVIDE -> binaryNumericOp(left, right,
                    (a, b) -> new Value.DoubleValue(a / b),
                    (a, b) -> { if (b == 0) throw new EvalException("Division by zero", ErrorCode.EVAL_DIVISION_BY_ZERO); return new Value.LongValue(a / b); },
                    (a, b) -> { if (b == 0) throw new EvalException("Division by zero", ErrorCode.EVAL_DIVISION_BY_ZERO); return new Value.IntValue(a / b); });
            case MODULO -> binaryNumericOp(left, right,
                    (a, b) -> new Value.DoubleValue(a % b),
                    (a, b) -> { if (b == 0) throw new EvalException("Division by zero", ErrorCode.EVAL_DIVISION_BY_ZERO); return new Value.LongValue(a % b); },
                    (a, b) -> { if (b == 0) throw new EvalException("Division by zero", ErrorCode.EVAL_DIVISION_BY_ZERO); return new Value.IntValue(a % b); });
            case INT_DIVIDE -> {
                long a = left.asLong();
                long b = right.asLong();
                if (b == 0) throw new EvalException("Division by zero", ErrorCode.EVAL_DIVISION_BY_ZERO);
                yield new Value.LongValue(a / b);
            }
            case MATH_MODULO -> {
                long a = left.asLong();
                long b = right.asLong();
                if (b == 0) throw new EvalException("Division by zero", ErrorCode.EVAL_DIVISION_BY_ZERO);
                yield new Value.LongValue(Math.floorMod(a, b));
            }
            case POWER -> binaryNumericOp(left, right, (a, b) -> new Value.DoubleValue(Math.pow(a, b)), (a, b) -> new Value.DoubleValue(Math.pow(a, b)), null);
            case EQUAL -> new Value.BooleanValue(left.equals(right));
            case NOT_EQUAL -> new Value.BooleanValue(!left.equals(right));
            case LESS_THAN -> new Value.BooleanValue(compare(left, right) < 0);
            case LESS_THAN_OR_EQUAL -> new Value.BooleanValue(compare(left, right) <= 0);
            case GREATER_THAN -> new Value.BooleanValue(compare(left, right) > 0);
            case GREATER_THAN_OR_EQUAL -> new Value.BooleanValue(compare(left, right) >= 0);
            case LOGICAL_AND -> new Value.BooleanValue(left.isTruthy() && right.isTruthy());
            case LOGICAL_OR -> new Value.BooleanValue(left.isTruthy() || right.isTruthy());
            case BITWISE_AND -> new Value.IntValue(left.asInt() & right.asInt());
            case BITWISE_OR -> new Value.IntValue(left.asInt() | right.asInt());
            case BITWISE_XOR -> new Value.IntValue(left.asInt() ^ right.asInt());
            case LEFT_SHIFT -> new Value.IntValue(left.asInt() << right.asInt());
            case RIGHT_SHIFT -> new Value.IntValue(left.asInt() >> right.asInt());
            case UNSIGNED_RIGHT_SHIFT -> new Value.IntValue(left.asInt() >>> right.asInt());
            case SPACESHIP -> new Value.IntValue(compare(left, right));
            case RANGE -> makeRange(left, right, true);
            case RANGE_EXCLUSIVE -> makeRange(left, right, false);
            default -> throw new EvalException("Unknown binary operator: " + node.getOperator(), ErrorCode.EVAL_INVALID_OPERATION);
        };
    }

    private Value binaryAdd(Value left, Value right) {
        if (left instanceof Value.StringValue || right instanceof Value.StringValue) {
            return new Value.StringValue(left.asString() + right.asString());
        }
        if (left instanceof Value.DoubleValue || right instanceof Value.DoubleValue) {
            return new Value.DoubleValue(left.asDouble() + right.asDouble());
        }
        if (left instanceof Value.LongValue || right instanceof Value.LongValue) {
            return new Value.LongValue(left.asLong() + right.asLong());
        }
        return new Value.IntValue(left.asInt() + right.asInt());
    }

    private interface DoubleOp { Value apply(double a, double b); }
    private interface LongOp { Value apply(long a, long b); }
    private interface IntOp { Value apply(int a, int b); }

    private Value binaryNumericOp(Value left, Value right, DoubleOp doubleOp, LongOp longOp, IntOp intOp) {
        if (left instanceof Value.DoubleValue || right instanceof Value.DoubleValue) {
            return doubleOp.apply(left.asDouble(), right.asDouble());
        }
        if (left instanceof Value.LongValue || right instanceof Value.LongValue) {
            return longOp.apply(left.asLong(), right.asLong());
        }
        return intOp.apply(left.asInt(), right.asInt());
    }

    private int compare(Value a, Value b) {
        if (a instanceof Value.DoubleValue || b instanceof Value.DoubleValue) {
            return Double.compare(a.asDouble(), b.asDouble());
        }
        if (a instanceof Value.IntValue && b instanceof Value.IntValue) {
            return Integer.compare(a.asInt(), b.asInt());
        }
        return Long.compare(a.asLong(), b.asLong());
    }

    private Value makeRange(Value left, Value right, boolean inclusive) {
        int start = left.asInt();
        int end = right.asInt();
        List<Integer> list = new ArrayList<>();
        int limit = inclusive ? end + 1 : end;
        for (int i = start; i < limit; i++) {
            list.add(i);
        }
        return Value.of(list);
    }

    private Value visitUnaryOp(UnaryOpNode node) {
        Value operand = evaluate(node.getOperand());
        return switch (node.getOperator()) {
            case NEGATIVE -> {
                if (operand instanceof Value.IntValue i) yield new Value.IntValue(-i.getValue());
                else if (operand instanceof Value.LongValue l) yield new Value.LongValue(-l.getValue());
                else yield new Value.DoubleValue(-operand.asDouble());
            }
            case POSITIVE -> operand;
            case NOT_NULL -> operand.requiresNonNull();
            case LOGICAL_NOT -> new Value.BooleanValue(!operand.isTruthy());
            case BITWISE_NOT -> new Value.IntValue(~operand.asInt());
            case PRE_INCREMENT -> {
                if (node.getOperand() instanceof VariableNode v) {
                    Value val = evalContext.getVariable(v.getName());
                    Value inc = increment(val);
                    evalContext.assignVariable(v.getName(), inc);
                    yield inc;
                }
                throw new EvalException("Cannot increment non-variable", ErrorCode.EVAL_INVALID_OPERATION);
            }
            case POST_INCREMENT -> {
                if (node.getOperand() instanceof VariableNode v) {
                    Value val = evalContext.getVariable(v.getName());
                    Value inc = increment(val);
                    evalContext.assignVariable(v.getName(), inc);
                    yield val;
                }
                throw new EvalException("Cannot increment non-variable", ErrorCode.EVAL_INVALID_OPERATION);
            }
            case PRE_DECREMENT -> {
                if (node.getOperand() instanceof VariableNode v) {
                    Value val = evalContext.getVariable(v.getName());
                    Value dec = decrement(val);
                    evalContext.assignVariable(v.getName(), dec);
                    yield dec;
                }
                throw new EvalException("Cannot decrement non-variable", ErrorCode.EVAL_INVALID_OPERATION);
            }
            case POST_DECREMENT -> {
                if (node.getOperand() instanceof VariableNode v) {
                    Value val = evalContext.getVariable(v.getName());
                    Value dec = decrement(val);
                    evalContext.assignVariable(v.getName(), dec);
                    yield val;
                }
                throw new EvalException("Cannot decrement non-variable", ErrorCode.EVAL_INVALID_OPERATION);
            }
        };
    }

    private Value increment(Value v) {
        if (v instanceof Value.IntValue i) return new Value.IntValue(i.getValue() + 1);
        if (v instanceof Value.LongValue l) return new Value.LongValue(l.getValue() + 1);
        if (v instanceof Value.DoubleValue d) return new Value.DoubleValue(d.getValue() + 1.0);
        throw new EvalException("Cannot increment type: " + v.getClass().getSimpleName(), ErrorCode.EVAL_INVALID_OPERATION);
    }

    private Value decrement(Value v) {
        if (v instanceof Value.IntValue i) return new Value.IntValue(i.getValue() - 1);
        if (v instanceof Value.LongValue l) return new Value.LongValue(l.getValue() - 1);
        if (v instanceof Value.DoubleValue d) return new Value.DoubleValue(d.getValue() - 1.0);
        throw new EvalException("Cannot decrement type: " + v.getClass().getSimpleName(), ErrorCode.EVAL_INVALID_OPERATION);
    }

    private Value visitAssignment(AssignmentNode node) {
        Value value = evaluate(node.getValue());
        if (node.isDeclaration()) {
            evalContext.setVariable(node.getVariableName(), value);
        } else {
            if (!evalContext.hasVariable(node.getVariableName())) {
                throw new EvalException("Variable not declared: " + node.getVariableName(), ErrorCode.SCOPE_VARIABLE_NOT_FOUND);
            }
            if (node.isFinal()) {
                throw new EvalException("Cannot assign to final variable: " + node.getVariableName(), ErrorCode.SCOPE_CANNOT_ASSIGN_TO_FINAL);
            }
            evalContext.assignVariable(node.getVariableName(), value);
        }
        return value;
    }

    private Value visitVarDecl(VarDeclNode node) {
        Value value;
        if (node.getInitializer() != null) {
            value = evaluate(node.getInitializer());
        } else {
            Class<?> type = node.getDeclaredType() != null ? node.getDeclaredType().getRawType() : null;
            value = defaultForType(type);
        }
        evalContext.setVariable(node.getVarName(), value);
        return value;
    }

    private Value defaultForType(Class<?> type) {
        if (type == int.class || type == byte.class || type == short.class) return new Value.IntValue(0);
        if (type == long.class) return new Value.LongValue(0L);
        if (type == double.class || type == float.class) return new Value.DoubleValue(0.0);
        if (type == boolean.class) return new Value.BooleanValue(false);
        if (type == char.class) return new Value.CharValue('\0');
        return Value.NullValue.INSTANCE;
    }

    private Value visitMethodCall(MethodCallNode node) {
        List<Value> args = evaluateAll(node.getArguments());
        Object target = resolveTarget(node.getTarget());
        String methodName = node.getMethodName();

        // forEach on Iterable: snapshot to avoid ConcurrentModificationException
        if ("forEach".equals(methodName) && target instanceof Iterable<?> iterable
                && args.size() == 1) {
            Object argObj = args.get(0).asJavaObject();
            if (argObj instanceof Lambda lambda && Lambda.isFunctionalInterface(java.util.function.Consumer.class)) {
                argObj = lambda.asInterface(java.util.function.Consumer.class);
            }
            if (argObj instanceof java.util.function.Consumer) {
                @SuppressWarnings("unchecked")
                java.util.function.Consumer<Object> consumer =
                        (java.util.function.Consumer<Object>) argObj;
                Object[] snapshot;
                if (target instanceof java.util.Collection<?> coll) {
                    snapshot = coll.toArray();
                } else {
                    List<Object> tmp = new ArrayList<>();
                    for (Object item : iterable) { tmp.add(item); }
                    snapshot = tmp.toArray();
                }
                for (Object item : snapshot) {
                    consumer.accept(item);
                }
                return Value.VoidValue.INSTANCE;
            }
        }

        // .invoke() on Function<Value[], Value> or Lambda stored in HashMap, etc.
        if ("invoke".equals(methodName)) {
            if (target instanceof Lambda lambda) {
                return lambda.invoke(args.toArray(new Value[0]));
            }
            if (target instanceof Function) {
                @SuppressWarnings("unchecked")
                Function<Value[], Value> func =
                        (Function<Value[], Value>) target;
                return func.apply(args.toArray(new Value[0]));
            }
        }

        // 无目标方法调用 → 函数调用(builtin 或脚本定义函数)
        if (node.getTarget() == null && evalContext.hasVariable(methodName)) {
            Value funcVal = evalContext.getVariable(methodName);
            if (funcVal instanceof Value.ObjectValue ov) {
                Object raw = ov.getValue();
                if (raw instanceof Lambda lambda) {
                    return lambda.invoke(args.toArray(new Value[0]));
                }
                if (raw instanceof Function) {
                    @SuppressWarnings("unchecked")
                    Function<Value[], Value> func =
                            (Function<Value[], Value>) raw;
                    return func.apply(args.toArray(new Value[0]));
                }
            }
        }
        if (node.getTarget() == null && evalContext.hasBuiltin(methodName)) {
            return evalContext.callBuiltin(methodName, args);
        }

        if (node.getBoundMethod() != null) {
            Method method = node.getBoundMethod();
            // 多态分发：对实例方法，在目标运行时类上查找实际覆写
            if (target != null && !Modifier.isStatic(method.getModifiers())) {
                try {
                    Method override = target.getClass().getMethod(method.getName(), method.getParameterTypes());
                    if (override.getDeclaringClass() != method.getDeclaringClass()) {
                        method = override;
                    }
                } catch (NoSuchMethodException ignored) { }
            }
            try {
                checkMethod(method); // ★ 安全检查
                Object result = method.invoke(target, args.stream().map(Value::asJavaObject).toArray());
                return Value.of(result);
            } catch (InvocationTargetException e) {
                throw new EvalException("Exception in " + methodName + ": " + e.getCause().getMessage(), e.getCause(), ErrorCode.EVAL_EXCEPTION_THROWN);
            } catch (Exception e) {
                throw new EvalException("Method call failed: " + methodName + " (" + e.getMessage() + ")", e, ErrorCode.METHOD_INVOCATION_FAILED);
            }
        }

        Class<?> clazz;
        if (target instanceof Class<?> c) {
            clazz = c;
        } else if (target != null) {
            clazz = target.getClass();
        } else {
            clazz = findClassForMethod(methodName);
        }
        if (clazz == null) {
            throw new EvalException("Cannot resolve method: " + methodName, ErrorCode.METHOD_NOT_FOUND);
        }
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == args.size()) {
                try {
                    checkMethod(m); // ★ 安全检查
                    Object result = m.invoke(target, args.stream().map(Value::asJavaObject).toArray());
                    return Value.of(result);
                } catch (InvocationTargetException e) {
                    throw new EvalException("Exception in " + methodName + ": " + e.getCause().getMessage(), e.getCause(), ErrorCode.EVAL_EXCEPTION_THROWN);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    // argument or access mismatch, try next overload
                }
            }
        }
            throw new EvalException("Method not found: " + methodName + " on " + clazz.getSimpleName(), ErrorCode.METHOD_NO_APPLICABLE_METHOD);
    }

    private Object resolveTarget(ASTNode targetNode) {
        if (targetNode == null) return null;
        Value targetVal = evaluate(targetNode);
        return targetVal.asJavaObject();
    }

    private Class<?> findClassForMethod(String methodName) {
        try {
            checkClassByName(methodName); // ★ 安全检查
            return Class.forName(methodName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Value visitClassReference(ClassReferenceNode node) {
        Class<?> resolvedClass = node.getResolvedClass();
        if (resolvedClass != null) {
            checkClass(resolvedClass); // ★ 安全检查
            return Value.of(resolvedClass);
        }
        try {
            checkClassByName(node.getOriginalTypeName()); // ★ 安全检查
            return Value.of(Class.forName(node.getOriginalTypeName()));
        } catch (ClassNotFoundException e) {
            throw new EvalException("Unknown class: " + node.getOriginalTypeName(), ErrorCode.EVAL_CLASS_NOT_FOUND);
        }
    }

    private Value visitFieldAccess(FieldAccessNode node) {
        Value target = evaluate(node.getTarget());
        Object obj = target.asJavaObject();
        if (obj == null) throw new EvalException("Cannot access field on null", ErrorCode.EVAL_NULL_POINTER);

        // 静态字段访问：target 已经是 Class<?>
        Class<?> resolvedClass = (obj instanceof Class<?> c) ? c : obj.getClass();

        if (node.getBoundField() != null) {
            try {
                checkFieldRead(node.getBoundField()); // ★ 安全检查
                return Value.of(node.getBoundField().get(obj));
            } catch (Exception e) {
                    throw new EvalException("Field access failed: " + node.getFieldName() + " (" + e.getMessage() + ")", e, ErrorCode.EVAL_FIELD_ACCESS_FAILED);
            }
        }

        try {
            Field f = resolvedClass.getField(node.getFieldName());
            checkFieldRead(f); // ★ 安全检查
            return Value.of(f.get(obj));
        } catch (Exception e) {
            throw new EvalException("Field not found: " + node.getFieldName() + " on " + resolvedClass.getSimpleName(), ErrorCode.EVAL_FIELD_ACCESS_FAILED);
        }
    }

    private Value visitConstructorCall(ConstructorCallNode node) {
        List<Value> args = evaluateAll(node.getArguments());

        // 匿名类：动态生成类 + 实例化
        ClassDeclarationNode anonClass = node.getAnonymousClass();
        if (anonClass != null && parseContext != null) {
            DynamicClassGenerator dcg = parseContext.getCodeGenerator();
            if (dcg != null) {
                String anonName = anonClassName(node);
                Class<?> generated = dcg.generateAnonymous(anonName, anonClass);
                if (generated != null) {
                    try {
                        Constructor<?> anonCtor = generated.getDeclaredConstructors()[0];
                        checkConstructor(anonCtor); // ★ 安全检查
                        Object result = anonCtor.newInstance(
                                args.stream().map(Value::asJavaObject).toArray());
                        return Value.of(result);
                    } catch (Exception e) {
                        throw new EvalException("Anonymous class construction failed: " + anonName, e, ErrorCode.EVAL_CONSTRUCTOR_INVOCATION_FAILED);
                    }
                }
            }
        }

        String className = node.getClassName();

        Class<?> clazz = findClass(className);
        if (clazz == null) {
            throw new EvalException("Class not found: " + className, ErrorCode.EVAL_CLASS_NOT_FOUND);
        }

        try {
            for (Constructor<?> ctor : clazz.getConstructors()) {
                if (ctor.getParameterCount() != args.size() && !ctor.isVarArgs()) continue;
                if (args.size() < ctor.getParameterCount() - (ctor.isVarArgs() ? 1 : 0)) continue;
                try {
                    checkConstructor(ctor); // ★ 安全检查
                    Object[] javaArgs = convertArgsForParameters(args, ctor.getParameterTypes(), ctor.isVarArgs());
                    Object result = ctor.newInstance(javaArgs);
                    return Value.of(result);
                } catch (InvocationTargetException e) {
                    throw new EvalException("Exception in " + className + " constructor: " + e.getCause().getMessage(), e.getCause(), ErrorCode.EVAL_EXCEPTION_THROWN);
                } catch (IllegalArgumentException | InstantiationException | IllegalAccessException e) {
                    // argument mismatch or can't instantiate, try next constructor
                }
            }
            throw new EvalException("No matching constructor for " + className, ErrorCode.EVAL_CONSTRUCTOR_INVOCATION_FAILED);
        } catch (Exception e) {
            throw new EvalException("Construction failed: " + className, e, ErrorCode.EVAL_CONSTRUCTOR_INVOCATION_FAILED);
        }
    }


    private static String anonClassName(ConstructorCallNode node) {
        String base = node.getClassName().replace('.', '_');
        return "anon$" + base + "$" + (++anonSeq);
    }

    private Class<?> findClass(String className) {
        if (parseContext != null) {
            Class<?> resolved = parseContext.resolveClass(className);
            if (resolved != null) {
                checkClass(resolved); // ★ 安全检查
                return resolved;
            }
        }
        // 回退
        try {
            checkClassByName(className); // ★ 安全检查
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Value visitTernary(TernaryNode node) {
        Value cond = evaluate(node.getCondition());
        return cond.isTruthy() ? evaluate(node.getThenExpr()) : evaluate(node.getElseExpr());
    }

    private Value visitArrayAccess(ArrayAccessNode node) {
        Value array = evaluate(node.getArray());
        Value index = evaluate(node.getIndex());
        Object[] arr = array.asArray();
        int i = index.asInt();
        if (i < 0 || i >= arr.length) throw new EvalException("Array index out of bounds: " + i, ErrorCode.EVAL_INDEX_OUT_OF_BOUNDS);
        return Value.of(arr[i]);
    }

    private Value visitArrayAssignment(ArrayAssignmentNode node) {
        Value array = evaluate(node.getArray());
        Value index = evaluate(node.getIndex());
        Value value = evaluate(node.getValue());
        Object[] arr = array.asArray();
        int i = index.asInt();
        if (i < 0 || i >= arr.length) throw new EvalException("Array index out of bounds: " + i, ErrorCode.EVAL_INDEX_OUT_OF_BOUNDS);
        arr[i] = value.asJavaObject();
        return value;
    }

    private Value visitArrayLiteral(ArrayLiteralNode node) {
        List<Value> elements = evaluateAll(node.getElements());
        return new Value.ArrayValue(elements.stream().map(Value::asJavaObject).toArray());
    }

    private Value visitNewArray(NewArrayNode node) {
        List<ASTNode> sizes = node.getSizes();
        int dimCount = sizes.size();
        // 找到最后一个非 null 维度来确定实际创建深度
        int nonNullCount = 0;
        int trailingNulls;
        for (int i = dimCount - 1; i >= 0; i--) {
            if (sizes.get(i) != null) {
                nonNullCount = i + 1;
                break;
            }
        }
        trailingNulls = dimCount - nonNullCount;

        Class<?> baseType = node.getElementType() != null ? node.getElementType() : Object.class;
        if (dimCount <= 1 || nonNullCount == 0) {
            // 单维或无具体维度：用 size
            Value size = evaluate(node.getSize());
            int len = size.asInt();
            Object javaArray = Array.newInstance(baseType, len);
            Object[] boxed = new Object[len];
            for (int i = 0; i < len; i++) {
                boxed[i] = Array.get(javaArray, i);
            }
            return new Value.ArrayValue(boxed);
        }

        // 多维度：只取非 null 的维度创建
        int[] lens = new int[nonNullCount];
        for (int i = 0; i < nonNullCount; i++) {
            lens[i] = evaluate(sizes.get(i)).asInt();
        }
        // 组件类型要加上 trailing nulls 的数组维度
        Class<?> componentType = baseType;
        for (int i = 0; i < trailingNulls; i++) {
            componentType = Array.newInstance(componentType, 0).getClass();
        }
        Object javaArray = Array.newInstance(componentType, lens);
        return arrayValueFromJavaArray(javaArray, nonNullCount);
    }

    private Value arrayValueFromJavaArray(Object javaArray, int depth) {
        int len = Array.getLength(javaArray);
        if (depth <= 1) {
            Object[] boxed = new Object[len];
            for (int i = 0; i < len; i++) {
                boxed[i] = Array.get(javaArray, i);
            }
            return new Value.ArrayValue(boxed);
        }
        Object[] boxed = new Object[len];
        for (int i = 0; i < len; i++) {
            Object elem = Array.get(javaArray, i);
            if (elem != null && elem.getClass().isArray()) {
                boxed[i] = arrayValueFromJavaArray(elem, depth - 1);
            } else {
                boxed[i] = elem;
            }
        }
        return new Value.ArrayValue(boxed);
    }

    private Value visitCast(CastNode node) {
        Value value = evaluate(node.getExpression());
        return CastUtils.castValue(value, node.getTargetType());
    }

    private Value visitInstanceof(InstanceofNode node) {
        Value value = evaluate(node.getExpression());
        try {
            checkClassByName(node.getTypeName()); // ★ 安全检查
            Class<?> clazz = Class.forName(node.getTypeName());
            return new Value.BooleanValue(clazz.isInstance(value.asJavaObject()));
        } catch (ClassNotFoundException e) {
            throw new EvalException("Unknown type in instanceof: " + node.getTypeName(), ErrorCode.EVAL_CLASS_NOT_FOUND);
        }
    }

    private Value visitPipeline(PipelineNode node) {
        Value input = evaluate(node.getInput());
        if (node.getFunction() instanceof MethodReferenceNode ref) {
            String methodName = ref.getMethodName();
            ASTNode targetNode = ref.getTarget();
            Object target = targetNode != null ? evaluate(targetNode).asJavaObject() : null;
            Class<?> clazz = target != null ? target.getClass() : findClassForMethod(methodName);
            if (clazz == null) throw new EvalException("Cannot resolve method: " + methodName, ErrorCode.METHOD_NOT_FOUND);
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                    try {
                        checkMethod(m); // ★ 安全检查
                        Object result = m.invoke(target, input.asJavaObject());
                        return Value.of(result);
                    } catch (Exception e) {
                        throw new EvalException("Pipeline method reference failed", e, ErrorCode.METHOD_INVOCATION_FAILED);
                    }
                }
            }
            throw new EvalException("Method not found: " + methodName, ErrorCode.METHOD_NO_APPLICABLE_METHOD);
        }
        if (node.getFunction() instanceof MethodCallNode call) {
            List<Value> args = new ArrayList<>();
            args.add(input);
            for (ASTNode arg : call.getArguments()) {
                args.add(evaluate(arg));
            }
            Object target = resolveTarget(call.getTarget());
            try {
                if (call.getBoundMethod() != null) {
                    checkMethod(call.getBoundMethod()); // ★ 安全检查
                    Object result = call.getBoundMethod().invoke(target, args.stream().map(Value::asJavaObject).toArray());
                    return Value.of(result);
                }
            } catch (Exception e) {
                throw new EvalException("Pipeline method call failed", e, ErrorCode.METHOD_INVOCATION_FAILED);
            }
        }
        // 对已求值的 Lambda / MethodReference 做 pipeline
        Value funcVal = evaluate(node.getFunction());
        Object raw = funcVal.asJavaObject();
        if (raw instanceof Lambda lambda) {
            return lambda.invoke(input);
        }
        if (raw instanceof MethodReference ref) {
            Object result = ref.invoke(input.asJavaObject());
            return Value.of(result);
        }
        if (raw instanceof java.util.function.Function) {
            @SuppressWarnings("unchecked")
            java.util.function.Function<Object, Object> fn =
                    (java.util.function.Function<Object, Object>) raw;
            return Value.of(fn.apply(input.asJavaObject()));
        }
        throw new EvalException("Pipeline: unsupported function type", ErrorCode.EVAL_INVALID_OPERATION);
    }

    private Value visitBlock(BlockNode node) {
        EvalContext childCtx = evalContext.createChild();
        Evaluator childEval = new Evaluator(childCtx, parseContext);
        Value result = Value.VoidValue.INSTANCE;
        for (ASTNode stmt : node.getStatements()) {
            result = childEval.evaluate(stmt);
        }
        // 块结束后将子作用域中匹配父作用域的变量变更传播回去
        // （支持 CustomClassExecutor 方法体内字段写回）
        for (var entry : childCtx.getVariables().entrySet()) {
            if (evalContext.hasVariable(entry.getKey())) {
                evalContext.assignVariable(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private Value visitIf(IfNode node) {
        Value cond = evaluate(node.getCondition());
        if (cond.isTruthy()) {
            return evaluate(node.getThenBlock());
        } else if (node.getElseBlock() != null) {
            return evaluate(node.getElseBlock());
        }
        return Value.VoidValue.INSTANCE;
    }

    private Value visitWhile(WhileNode node) {
        Value result = Value.VoidValue.INSTANCE;
        while (true) {
            Value cond = evaluate(node.getCondition());
            if (!cond.isTruthy()) break;
            try {
                result = evaluate(node.getBody());
            } catch (BreakException e) {
                break;
            } catch (ContinueException ignoredAsNaturalCtrlFlow) {
            }
        }
        return result;
    }

    private Value visitDoWhile(DoWhileNode node) {
        Value result = Value.VoidValue.INSTANCE;
        do {
            try {
                result = evaluate(node.getBody());
            } catch (BreakException e) {
                break;
            } catch (ContinueException ignoredAsNaturalCtrlFlow) {
            }
        } while (evaluate(node.getCondition()).isTruthy());
        return result;
    }

    private Value visitFor(ForNode node) {
        EvalContext loopCtx = evalContext.createChild();
        Evaluator loopEval = new Evaluator(loopCtx, parseContext);
        Value result = Value.VoidValue.INSTANCE;

        if (node.getInitialization() != null) loopEval.evaluate(node.getInitialization());
        while (node.getCondition() == null || loopEval.evaluate(node.getCondition()).isTruthy()) {
            try {
                result = loopEval.evaluate(node.getBody());
            } catch (BreakException e) {
                break;
            } catch (ContinueException ignoredAsNaturalCtrlFlow) {
            }
            if (node.getUpdate() != null) loopEval.evaluate(node.getUpdate());
        }
        return result;
    }

    private Value visitForEach(ForEachNode node) {
        Value collection = evaluate(node.getCollection());
        Object[] items;
        if (collection.asJavaObject() instanceof Iterable<?> iter) {
            List<Object> list = new ArrayList<>();
            for (Object item : iter) list.add(item);
            items = list.toArray();
        } else {
            items = collection.asArray();
        }
        EvalContext loopCtx = evalContext.createChild();
        Evaluator loopEval = new Evaluator(loopCtx, parseContext);
        Value result = Value.VoidValue.INSTANCE;
        for (Object item : items) {
            loopCtx.setVariable(node.getItemName(), Value.of(item));
            try {
                result = loopEval.evaluate(node.getBody());
            } catch (BreakException e) {
                break;
            } catch (ContinueException e) {
                // continue to next iteration
            }
        }
        return result;
    }

    private Value visitSwitch(SwitchNode node) {
        Value expr = evaluate(node.getExpression());
        for (CaseNode caseNode : node.getCases()) {
            Value caseVal = evaluate(caseNode.getValue());
            if (expr.equals(caseVal)) {
                EvalContext childCtx = evalContext.createChild();
                Evaluator childEval = new Evaluator(childCtx, parseContext);
                Value result = Value.VoidValue.INSTANCE;
                for (ASTNode stmt : caseNode.getStatements()) {
                    try {
                        result = childEval.evaluate(stmt);
                    } catch (BreakException e) {
                        return result;
                    }
                }
                return result;
            }
        }
        if (node.getDefaultCase() != null) {
            if (node.getDefaultCase() instanceof BlockNode block) {
                EvalContext childCtx = evalContext.createChild();
                Evaluator childEval = new Evaluator(childCtx, parseContext);
                Value result = Value.VoidValue.INSTANCE;
                for (ASTNode stmt : block.getStatements()) {
                    try {
                        result = childEval.evaluate(stmt);
                    } catch (BreakException e) {
                        return result;
                    }
                }
                return result;
            }
            return evaluate(node.getDefaultCase());
        }
        return Value.VoidValue.INSTANCE;
    }

    private Value visitReturn(ReturnNode node) {
        if (node.getValue() != null) {
            Value val = evaluate(node.getValue());
            throw new ReturnException(val);
        }
        throw new ReturnException(Value.VoidValue.INSTANCE);
    }

    private Value visitBreak(BreakNode node) {
        if (node.isLabeled()) {
            throw new LabeledBreakException(node.getLabel());
        }
        throw new BreakException();
    }

    private Value visitContinue(ContinueNode node) {
        throw new ContinueException();
    }

    private Value visitLambda(LambdaNode node) {
        Function<Value[], Value> lambdaFunc = args -> {
            EvalContext lambdaCtx = evalContext.createChild();
            List<LambdaNode.Parameter> params = node.getParameters();
            for (int i = 0; i < params.size() && i < args.length; i++) {
                lambdaCtx.setVariable(params.get(i).name(), args[i]);
            }
            Evaluator lambdaEval = new Evaluator(lambdaCtx, parseContext);
            try {
                return lambdaEval.evaluate(node.getBody());
            } catch (ReturnException e) {
                return (Value) e.getValue();
            }
        };

        List<String> paramNames = new ArrayList<>();
        for (LambdaNode.Parameter p : node.getParameters()) {
            paramNames.add(p.name());
        }
        Lambda lambda = new Lambda(lambdaFunc, paramNames, evalContext);

        Class<?> fiType = node.getFunctionalInterfaceType();
        if (fiType != null) {
            return Value.of(lambda.asInterface(fiType));
        }

        return Value.of(lambda);
    }

    private Value visitFunctionDef(FunctionDefNode node) {
        Function<Value[], Value> func = args -> {
            EvalContext funcCtx = evalContext.createChild();
            List<LambdaNode.Parameter> params = node.getParameters();
            for (int i = 0; i < params.size() && i < args.length; i++) {
                funcCtx.setVariable(params.get(i).name(), args[i]);
            }
            Evaluator funcEval = new Evaluator(funcCtx, parseContext);
            try {
                funcEval.evaluate(node.getBody());
            } catch (ReturnException e) {
                return (Value) e.getValue();
            }
            return Value.VoidValue.INSTANCE;
        };
        evalContext.setVariable(node.getFunctionName(), Value.of(func));
        return Value.VoidValue.INSTANCE;
    }

    private Value visitFunctionCall(FunctionCallNode node) {
        List<Value> args = evaluateAll(node.getArguments());
        String funcName = node.getFunctionName();
        if (evalContext.hasVariable(funcName)) {
            Value funcVal = evalContext.getVariable(funcName);
            if (funcVal instanceof Value.ObjectValue ov) {
                Object raw = ov.getValue();
                if (raw instanceof Lambda lambda) {
                    return lambda.invoke(args.toArray(new Value[0]));
                }
                if (raw instanceof Function) {
                    @SuppressWarnings("unchecked")
                    Function<Value[], Value> func = (Function<Value[], Value>) raw;
                    return func.apply(args.toArray(new Value[0]));
                }
            }
        }
        if (evalContext.hasBuiltin(funcName)) {
            return evalContext.callBuiltin(funcName, args);
        }
        throw new EvalException("Function not defined: " + funcName, ErrorCode.EVAL_UNDEFINED_VARIABLE);
    }

    private Value visitAsync(AsyncNode node) {
        return evaluate(node.getExpression());
    }

    private Value visitAwait(AwaitNode node) {
        return evaluate(node.getExpression());
    }

    private Value visitMapLiteral(MapLiteralNode node) {
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<ASTNode, ASTNode> entry : node.getEntries().entrySet()) {
            Object key = evaluate(entry.getKey()).asJavaObject();
            Object value = evaluate(entry.getValue()).asJavaObject();
            result.put(key, value);
        }
        return Value.of(result);
    }

    private Value visitInterpolatedString(InterpolatedStringNode node) {
        StringBuilder sb = new StringBuilder();
        for (InterpolatedStringNode.Part part : node.getParts()) {
            if (part.isExpression()) {
                Value val = evaluate(part.getExpression());
                sb.append(val.asString());
            } else {
                sb.append(part.getLiteralText());
            }
        }
        String result = sb.toString();
        return new Value.StringValue(result);
    }

    private Value visitFieldAssignment(FieldAssignmentNode node) {
        Value target = evaluate(node.getTarget());
        Value value = evaluate(node.getValue());
        Object obj = target.asJavaObject();
        if (obj == null) throw new EvalException("Cannot set field on null", ErrorCode.EVAL_NULL_POINTER);
        try {
            Field f = obj.getClass().getField(node.getFieldName());
            checkFieldWrite(f); // ★ 安全检查
            f.set(obj, value.asJavaObject());
        } catch (Exception e) {
            throw new EvalException("Field assignment failed: " + node.getFieldName(), e, ErrorCode.EVAL_FIELD_ACCESS_FAILED);
        }
        return value;
    }

    private Value visitMethodReference(MethodReferenceNode node) {
        String methodName = node.getMethodName();
        ASTNode targetNode = node.getTarget();
        Function<Object[], Object> refFunc = args -> {
            Object target = targetNode != null ? evaluate(targetNode).asJavaObject() : null;
            Class<?> clazz = target != null ? target.getClass() : findClassForMethod(methodName);
            if (clazz == null) throw new EvalException("Cannot resolve method: " + methodName, ErrorCode.METHOD_NOT_FOUND);
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                    try {
                        checkMethod(m); // ★ 安全检查
                        return m.invoke(target, args);
                    } catch (Exception e) {
                        throw new EvalException("Method reference failed", e, ErrorCode.METHOD_INVOCATION_FAILED);
                    }
                }
            }
            throw new EvalException("Method not found: " + methodName, ErrorCode.METHOD_NO_APPLICABLE_METHOD);
        };

        MethodReference ref = new MethodReference(methodName, refFunc);

        Class<?> fiType = node.getFunctionalInterfaceType();
        if (fiType != null) {
            return Value.of(ref.asInterface(fiType));
        }

        return Value.of(ref);
    }

    // ==================== 自定义运算符重载 ====================

    private Value tryCustomBinaryOp(BinaryOpNode.Operator op, Value left, Value right) {
        OperatorRegistry registry = parseContext.getOperatorRegistry();
        if (registry == null || registry.isEmpty()) return null;

        String opSymbol = op.getSymbol();
        if (!OperatorRegistry.BINARY_OPERATORS.contains(opSymbol)) return null;

        Object lhsObj = left.asJavaObject();
        Object rhsObj = right.asJavaObject();
        if (lhsObj == null || rhsObj == null) return null;

        OperatorRegistry.Overload overload = registry.findBinaryCompatible(
                opSymbol, lhsObj.getClass(), rhsObj.getClass());
        if (overload == null) return null;

        return invokeOperatorOverload(overload, List.of(left, right));
    }

    private Value invokeOperatorOverload(OperatorRegistry.Overload overload, List<Value> argValues) {
        ASTNode impl = overload.implementation();
        EvalContext childCtx = evalContext.createChild();
        Evaluator childEval = new Evaluator(childCtx, parseContext);

        if (impl instanceof FunctionDefNode fn) {
            List<LambdaNode.Parameter> params = fn.getParameters();
            for (int i = 0; i < params.size() && i < argValues.size(); i++) {
                childCtx.setVariable(params.get(i).name(), argValues.get(i));
            }
            try {
                return childEval.evaluate(fn.getBody());
            } catch (ReturnException e) {
                return (Value) e.getValue();
            }
        }

        if (impl instanceof MethodDeclarationNode md) {
            List<ParameterNode> params = md.getParameters();
            for (int i = 0; i < params.size() && i < argValues.size(); i++) {
                childCtx.setVariable(params.get(i).getParameterName(), argValues.get(i));
            }
            ASTNode body = md.getBody();
            if (body != null) {
                try {
                    return childEval.evaluate(body);
                } catch (ReturnException e) {
                    return (Value) e.getValue();
                }
            }
            return Value.VoidValue.INSTANCE;
        }

        return null;
    }

    private void cacheOperatorCallback(BinaryOpNode node, Value left, Value right) {
        OperatorRegistry registry = parseContext.getOperatorRegistry();
        if (registry == null || registry.isEmpty()) return;

        String opSymbol = node.getOperator().getSymbol();
        Object lhsObj = left.asJavaObject();
        Object rhsObj = right.asJavaObject();
        if (lhsObj == null || rhsObj == null) return;

        OperatorRegistry.Overload overload = registry.findBinaryCompatible(
                opSymbol, lhsObj.getClass(), rhsObj.getClass());
        if (overload == null) return;

        node.setOperatorCallback((l, r) -> invokeOperatorOverload(overload, List.of(l, r)));
    }

    private Value visitSafeFieldAccess(SafeFieldAccessNode node) {
        Value target = evaluate(node.getTarget());
        if (target instanceof Value.NullValue) return Value.NullValue.INSTANCE;
        Object obj = target.asJavaObject();
        try {
            Field f = obj.getClass().getField(node.getFieldName());
            checkFieldRead(f); // ★ 安全检查
            return Value.of(f.get(obj));
        } catch (Exception e) {
            return Value.NullValue.INSTANCE;
        }
    }

    private Value visitSafeMethodCall(SafeMethodCallNode node) {
        Value target = evaluate(node.getTarget());
        if (target instanceof Value.NullValue) return Value.NullValue.INSTANCE;
        try {
            List<Value> args = evaluateAll(node.getArguments());
            Object obj = target.asJavaObject();
            String methodName = node.getMethodName();
            for (Method m : obj.getClass().getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == args.size()) {
                    checkMethod(m); // ★ 安全检查
                    Object result = m.invoke(obj, args.stream().map(Value::asJavaObject).toArray());
                    return Value.of(result);
                }
            }
            throw new EvalException("Method not found: " + methodName, ErrorCode.METHOD_NO_APPLICABLE_METHOD);
        } catch (Exception e) {
            return Value.NullValue.INSTANCE;
        }
    }

    private Value visitThrow(ThrowNode node) {
        Value val = evaluate(node.getExpression());
        throw new EvalException("Uncaught throw: " + val, ErrorCode.EVAL_EXCEPTION_THROWN);
    }

    private Value visitDelete(DeleteNode node) {
        if (node.isDeleteAll()) {
            evalContext.getVariables().clear();
        } else {
            String name = node.getVariableName();
            if (evalContext.getVariables().containsKey(name)) {
                evalContext.getVariables().remove(name);
            } else if (evalContext.getParent() != null) {
                // Walk up to find and delete from the defining scope
                EvalContext ctx = evalContext;
                while (ctx != null) {
                    if (ctx.getVariables().containsKey(name)) {
                        ctx.getVariables().remove(name);
                        break;
                    }
                    ctx = ctx.getParent();
                }
            }
        }
        return Value.VoidValue.INSTANCE;
    }

    private Value visitLabeledStatement(LabeledStatementNode node) {
        try {
            return evaluate(node.getStatement());
        } catch (LabeledBreakException e) {
            if (e.getLabel().equals(node.getLabel())) {
                return Value.VoidValue.INSTANCE;
            }
            throw e;
        }
    }

    private Value visitTry(TryNode node) {
        try {
            return evaluate(node.getTryBlock());
        } catch (BreakException | ContinueException | ReturnException | LabeledBreakException e) {
            throw e;
        } catch (Exception e) {
            for (CatchClause catchClause : node.getCatchClauses()) {
                EvalContext catchCtx = evalContext.createChild();
                catchCtx.setVariable(catchClause.getVariableName(), Value.of(e));
                Evaluator catchEval = new Evaluator(catchCtx, parseContext);
                return catchEval.evaluate(catchClause.getBody());
            }
            if (node.getFinallyBlock() != null) {
                evaluate(node.getFinallyBlock());
            }
            throw e;
        }
    }


    /** 将 Value 参数列表转换为 Java 对象数组，对 Lambda 类型的 Value 做 FI proxy 兜底。 */
    private Object[] convertArgsForParameters(List<Value> args, Class<?>[] paramTypes, boolean isVarArgs) {
        Object[] javaArgs = new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            Object raw = args.get(i).asJavaObject();
            Class<?> expectedType = paramTypes[Math.min(i, paramTypes.length - 1)];
            if (isVarArgs && i >= paramTypes.length - 1) {
                expectedType = expectedType.getComponentType();
            }
            if (raw instanceof Lambda lambdaObj && expectedType.isInterface()
                    && Lambda.isFunctionalInterface(expectedType)) {
                javaArgs[i] = lambdaObj.asInterface(expectedType);
                continue;
            }
            javaArgs[i] = raw;
        }
        return javaArgs;
    }

    // ==================== 安全检查辅助 ====================

    /** 获取当前的安全门卫（可能为 null）。 */
    private SecurityGate sg() {
        return evalContext.getSecurityGate();
    }

    /** 快捷方法：在方法调用前检查。 */
    private void checkMethod(Method method) throws SecurityException {
        SecurityGate gate = sg();
        if (gate != null) gate.beforeMethodCall(method);
    }

    /** 快捷方法：在字段读取前检查。 */
    private void checkFieldRead(Field field) throws SecurityException {
        SecurityGate gate = sg();
        if (gate != null) gate.beforeFieldRead(field);
    }

    /** 快捷方法：在字段写入前检查。 */
    private void checkFieldWrite(Field field) throws SecurityException {
        SecurityGate gate = sg();
        if (gate != null) gate.beforeFieldWrite(field);
    }

    /** 快捷方法：在构造器调用前检查。 */
    private void checkConstructor(Constructor<?> ctor) throws SecurityException {
        SecurityGate gate = sg();
        if (gate != null) gate.beforeConstructorCall(ctor);
    }

    /** 快捷方法：在类访问前检查（通过 Class 对象）。 */
    private void checkClass(Class<?> clazz) throws SecurityException {
        SecurityGate gate = sg();
        if (gate != null) gate.beforeClassAccess(clazz);
    }

    /** 快捷方法：在类访问前检查（通过类名字符串）。 */
    private void checkClassByName(String className) throws SecurityException {
        SecurityGate gate = sg();
        if (gate != null) gate.beforeClassAccessByName(className);
    }
}
