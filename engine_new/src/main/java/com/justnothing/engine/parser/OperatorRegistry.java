package com.justnothing.engine.parser;

import com.justnothing.engine.ast.ASTNode;
import com.justnothing.engine.ast.OperatorCallback;
import com.justnothing.engine.eval.Value;
import com.justnothing.engine.eval.Value.BooleanValue;
import com.justnothing.engine.eval.Value.DoubleValue;
import com.justnothing.engine.eval.Value.IntValue;
import com.justnothing.engine.eval.Value.LongValue;
import com.justnothing.engine.eval.Value.StringValue;
import com.justnothing.engine.lexer.Operators;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 统一运算符注册表——内置运算符和用户自定义运算符的唯一真相源。
 * <p>
 * 两种实现模式：
 * <ul>
 *   <li><b>内置运算符</b>（Java 回调）：{@code registerBuiltinBinary("+", Number.class, Number.class, (l,r) -> ...)}
 *   <li><b>用户自定义</b>（AST 节点）：{@code registerBinary("+", Vec2.class, Vec2.class, ..., astNode)}</li>
 * </ul>
 * <p>
 * 解析期通过此表做符号校验 + 预写 callback 到 BinaryOpNode；
 * 运行期直接调用已缓存的 callback，零查找开销。
 */
public final class OperatorRegistry {

    /** 支持的二元运算符集合。 */
    public static final Set<String> BINARY_OPERATORS = Operators.BINARY_OPERATORS;

    /** 支持的一元运算符集合。 */
    public static final Set<String> UNARY_OPERATORS = Operators.UNARY_OPERATORS;

    /**
     * 运算符重载条目。支持两种实现模式：
     * <ul>
     *   <li><b>AST 实现</b>（用户定义的 operator+ 函数）：{@code implementation != null}</li>
     *   <li><b>Java 回调实现</b>（内置运算符）：{@code javaCallback != null}</li>
     * </ul>
     */
    public record Overload(
            String operator,
            List<Class<?>> parameterTypes,
            ASTNode implementation,
            Class<?> returnType,
            BiFunction<Value, Value, Value> javaCallback
    ) {
        /** 是否为内置 Java 回调实现。 */
        public boolean isBuiltin() {
            return javaCallback != null;
        }

        /** 是否为用户 AST 实现。 */
        public boolean isUserDefined() {
            return implementation != null;
        }

        /** 转换为 OperatorCallback（供 BinaryOpNode 缓存使用）。 */
        public OperatorCallback toOperatorCallback(EvaluatorBridge bridge) {
            if (javaCallback != null) {
                // 内置运算符：直接用 Java 回调
                return javaCallback::apply;
            }
            if (implementation != null) {
                // 用户自定义：通过 Evaluator 执行 AST 节点
                return (left, right) -> bridge.invokeUserOverload(this, left, right);
            }
            throw new IllegalStateException("Overload has no implementation: " + operator);
        }
    }

    /** 桥接接口：用户自定义重载需要 Evaluator 来执行 AST 节点。 */
    @FunctionalInterface
    public interface EvaluatorBridge {
        Value invokeUserOverload(Overload overload, Value left, Value right);
    }

    // ==================== 存储 ====================

    /** 二元运算符映射：(operator, lhsType, rhsType) → Overload */
    private final Map<BinaryOpKey, Overload> binaryOps = new ConcurrentHashMap<>();

    /** 一元运算符映射：(operator, operandType) → Overload */
    private final Map<UnaryOpKey, Overload> unaryOps = new ConcurrentHashMap<>();

    /** 所有已注册的重载（按注册顺序） */
    private final List<Overload> allOverloads = Collections.synchronizedList(new ArrayList<>());

    // ==================== 用户自定义注册（AST 节点）====================

    /**
     * 注册一个用户定义的二元运算符重载（AST 实现）。
     *
     * @param operator       运算符符号
     * @param lhsType        左操作数类型
     * @param rhsType        右操作数类型
     * @param returnType     返回值类型
     * @param implementation 实现节点（MethodDeclarationNode 或 FunctionDefNode）
     */
    public void registerBinary(String operator, Class<?> lhsType, Class<?> rhsType,
                               Class<?> returnType, ASTNode implementation) {
        validateOperator(operator, false);
        Overload overload = new Overload(operator,
                List.of(lhsType, rhsType), implementation, returnType, null);
        doRegisterBinary(operator, lhsType, rhsType, overload);
    }

    /**
     * 注册一个用户定义的一元运算符重载（AST 实现）。
     */
    public void registerUnary(String operator, Class<?> operandType,
                              Class<?> returnType, ASTNode implementation) {
        validateOperator(operator, true);
        Overload overload = new Overload(operator,
                List.of(operandType), implementation, returnType, null);
        doRegisterUnary(operator, operandType, overload);
    }

    // ==================== 内置运算符注册（Java 回调）====================

    /**
     * 注册一个内置二元运算符（Java 回调实现）。
     * <p>用于预注册 int+int、String+String 等原生语义。
     *
     * @param operator       运算符符号
     * @param lhsType        左操作数声明类型
     * @param rhsType        右操作数声明类型
     * @param returnType     返回值类型
     * @param callback       运算逻辑回调
     */
    public void registerBuiltinBinary(String operator, Class<?> lhsType, Class<?> rhsType,
                                      Class<?> returnType, BiFunction<Value, Value, Value> callback) {
        validateOperator(operator, false);
        Overload overload = new Overload(operator,
                List.of(lhsType, rhsType), null, returnType, callback);
        doRegisterBinary(operator, lhsType, rhsType, overload);
    }

    /**
     * 注册一个内置一元运算符（Java 回调实现）。
     *
     * @param operator       运算符符号
     * @param operandType    操作数声明类型
     * @param returnType     返回值类型
     * @param callback       运算逻辑回调
     */
    public void registerBuiltinUnary(String operator, Class<?> operandType,
                                     Class<?> returnType, Function<Value, Value> callback) {
        validateOperator(operator, true);
        BiFunction<Value, Value, Value> bifunc = (v, ignore) -> callback.apply(v);
        Overload overload = new Overload(operator,
                List.of(operandType), null, returnType, bifunc);
        doRegisterUnary(operator, operandType, overload);
    }

    private void doRegisterBinary(String op, Class<?> lhs, Class<?> rhs, Overload overload) {
        binaryOps.put(new BinaryOpKey(op, lhs, rhs), overload);
        allOverloads.add(overload);
    }

    private void doRegisterUnary(String op, Class<?> operand, Overload overload) {
        unaryOps.put(new UnaryOpKey(op, operand), overload);
        allOverloads.add(overload);
    }

    // ==================== 查找 ====================

    public Overload findBinary(String operator, Class<?> lhsType, Class<?> rhsType) {
        return binaryOps.get(new BinaryOpKey(operator, lhsType, rhsType));
    }

    public Overload findUnary(String operator, Class<?> operandType) {
        return unaryOps.get(new UnaryOpKey(operator, operandType));
    }

    public Overload findBinaryCompatible(String operator, Class<?> lhsType, Class<?> rhsType) {
        Overload exact = findBinary(operator, lhsType, rhsType);
        if (exact != null) return exact;

        Overload best = null;
        int bestScore = -1;
        for (Map.Entry<BinaryOpKey, Overload> entry : binaryOps.entrySet()) {
            BinaryOpKey key = entry.getKey();
            if (!key.operator().equals(operator)) continue;

            int score = compatibilityScore(key.lhsType(), lhsType)
                    + compatibilityScore(key.rhsType(), rhsType);
            if (score > bestScore && score > 0) {
                bestScore = score;
                best = entry.getValue();
            }
        }
        return best;
    }

    public Overload findUnaryCompatible(String operator, Class<?> operandType) {
        Overload exact = findUnary(operator, operandType);
        if (exact != null) return exact;

        Overload best = null;
        int bestScore = -1;
        for (Map.Entry<UnaryOpKey, Overload> entry : unaryOps.entrySet()) {
            UnaryOpKey key = entry.getKey();
            if (!key.operator().equals(operator)) continue;

            int score = compatibilityScore(key.operandType(), operandType);
            if (score > bestScore) {
                bestScore = score;
                best = entry.getValue();
            }
        }
        return best;
    }

    // ==================== 查询 ====================

    public boolean isEmpty() {
        return binaryOps.isEmpty() && unaryOps.isEmpty();
    }

    public List<Overload> getAllOverloads() {
        return Collections.unmodifiableList(new ArrayList<>(allOverloads));
    }

    /** 是否包含任何内置运算符注册（用于判断是否启用解析期严格校验）。 */
    public boolean hasBuiltins() {
        return allOverloads.stream().anyMatch(Overload::isBuiltin);
    }

    public void clear() {
        binaryOps.clear();
        unaryOps.clear();
        allOverloads.clear();
    }

    /**
     * 预注册所有内置运算符（Number/Number + String 拼接等）。
     * <p>由 ScriptRunner 在构造时调用一次，使 Registry 成为运算符的唯一真相源。
     */
    public void registerAllBuiltins() {
        // ===== 算术运算符 (Number, Number) → Number =====
        registerBuiltinBinary("+", Number.class, Number.class, Object.class,
                (l, r) -> numericAdd(l, r));
        registerBuiltinBinary("-", Number.class, Number.class, Double.class,
                (l, r) -> new Value.DoubleValue(l.asDouble() - r.asDouble()));
        registerBuiltinBinary("*", Number.class, Number.class, Double.class,
                (l, r) -> new Value.DoubleValue(l.asDouble() * r.asDouble()));
        registerBuiltinBinary("/", Number.class, Number.class, Double.class,
                (l, r) -> { double b = r.asDouble(); if (b == 0) throw new RuntimeException("Division by zero"); return new Value.DoubleValue(l.asDouble() / b); });
        registerBuiltinBinary("%", Number.class, Number.class, Double.class,
                (l, r) -> { double b = r.asDouble(); if (b == 0) throw new RuntimeException("Division by zero"); return new Value.DoubleValue(l.asDouble() % b); });
        registerBuiltinBinary("**", Number.class, Number.class, Double.class,
                (l, r) -> new Value.DoubleValue(Math.pow(l.asDouble(), r.asDouble())));

        // 整数专用
        registerBuiltinBinary("//", Integer.class, Integer.class, Long.class,
                (l, r) -> { int b = r.asInt(); if (b == 0) throw new RuntimeException("Division by zero"); return new Value.LongValue((long) l.asInt() / b); });
        registerBuiltinBinary("%%", Integer.class, Integer.class, Long.class,
                (l, r) -> { int b = r.asInt(); if (b == 0) throw new RuntimeException("Division by zero"); return new Value.LongValue(Math.floorMod(l.asInt(), b)); });

        // ===== 比较运算符 =====
        registerBuiltinBinary("==", Object.class, Object.class, Boolean.class,
                (l, r) -> new Value.BooleanValue(l.equals(r)));
        registerBuiltinBinary("!=", Object.class, Object.class, Boolean.class,
                (l, r) -> new Value.BooleanValue(!l.equals(r)));
        registerBuiltinBinary("<", Number.class, Number.class, Boolean.class,
                (l, r) -> new Value.BooleanValue(compareValues(l, r) < 0));
        registerBuiltinBinary("<=", Number.class, Number.class, Boolean.class,
                (l, r) -> new Value.BooleanValue(compareValues(l, r) <= 0));
        registerBuiltinBinary(">", Number.class, Number.class, Boolean.class,
                (l, r) -> new Value.BooleanValue(compareValues(l, r) > 0));
        registerBuiltinBinary(">=", Number.class, Number.class, Boolean.class,
                (l, r) -> new Value.BooleanValue(compareValues(l, r) >= 0));
        registerBuiltinBinary("<=>", Object.class, Object.class, Integer.class,
                (l, r) -> new Value.IntValue(compareValues(l, r)));

        // ===== 逻辑运算符 =====
        registerBuiltinBinary("&&", Object.class, Object.class, Boolean.class,
                (l, r) -> new Value.BooleanValue(l.isTruthy() && r.isTruthy()));
        registerBuiltinBinary("||", Object.class, Object.class, Boolean.class,
                (l, r) -> new Value.BooleanValue(l.isTruthy() || r.isTruthy()));

        // ===== 位运算符 (Integer, Integer) → Integer =====
        registerBuiltinBinary("&", Integer.class, Integer.class, Integer.class,
                (l, r) -> new Value.IntValue(l.asInt() & r.asInt()));
        registerBuiltinBinary("|", Integer.class, Integer.class, Integer.class,
                (l, r) -> new Value.IntValue(l.asInt() | r.asInt()));
        registerBuiltinBinary("^", Integer.class, Integer.class, Integer.class,
                (l, r) -> new Value.IntValue(l.asInt() ^ r.asInt()));
        registerBuiltinBinary("<<", Integer.class, Integer.class, Integer.class,
                (l, r) -> new Value.IntValue(l.asInt() << r.asInt()));
        registerBuiltinBinary(">>", Integer.class, Integer.class, Integer.class,
                (l, r) -> new Value.IntValue(l.asInt() >> r.asInt()));
        registerBuiltinBinary(">>>", Integer.class, Integer.class, Integer.class,
                (l, r) -> new Value.IntValue(l.asInt() >>> r.asInt()));

        // ===== 范围运算符 =====
        registerBuiltinBinary("..", Integer.class, Integer.class, Object.class,
                (l, r) -> makeRange(l, r, true));
        registerBuiltinBinary("..<", Integer.class, Integer.class, Object.class,
                (l, r) -> makeRange(l, r, false));

        // ===== 一元运算符 =====
        registerBuiltinUnary("-", Number.class, Double.class,
                v -> new Value.DoubleValue(-v.asDouble()));
        registerBuiltinUnary("!", Object.class, Boolean.class,
                v -> new Value.BooleanValue(!v.isTruthy()));
        registerBuiltinUnary("~", Integer.class, Integer.class,
                v -> new Value.IntValue(~v.asInt()));
    }

    // ==================== 内置运算辅助方法 ====================

    private static Value numericAdd(Value l, Value r) {
        if (l instanceof Value.StringValue || r instanceof Value.StringValue) {
            return new Value.StringValue(l.asString() + r.asString());
        }
        if (l instanceof Value.DoubleValue || r instanceof Value.DoubleValue) {
            return new Value.DoubleValue(l.asDouble() + r.asDouble());
        }
        if (l instanceof Value.LongValue || r instanceof Value.LongValue) {
            return new Value.LongValue(l.asLong() + r.asLong());
        }
        return new Value.IntValue(l.asInt() + r.asInt());
    }

    private static int compareValues(Value a, Value b) {
        if (a instanceof Value.DoubleValue || b instanceof Value.DoubleValue) {
            return Double.compare(a.asDouble(), b.asDouble());
        }
        if (a instanceof Value.IntValue && b instanceof Value.IntValue) {
            return Integer.compare(a.asInt(), b.asInt());
        }
        return Long.compare(a.asLong(), b.asLong());
    }

    @SuppressWarnings("unchecked")
    private static Value makeRange(Value left, Value right, boolean inclusive) {
        int start = left.asInt();
        int end = right.asInt();
        java.util.List<Integer> list = new java.util.ArrayList<>();
        int limit = inclusive ? end + 1 : end;
        for (int i = start; i < limit; i++) list.add(i);
        return Value.of(list);
    }

    // ==================== 内部工具 ====================

    private void validateOperator(String operator, boolean isUnary) {
        Set<String> allowed = isUnary ? UNARY_OPERATORS : BINARY_OPERATORS;
        if (!allowed.contains(operator)) {
            throw new IllegalArgumentException(
                    "Unsupported operator '" + operator + "' for "
                            + (isUnary ? "unary" : "binary") + " operator overload");
        }
    }

    /** 精确匹配=2，父类匹配=1，不兼容=0 */
    static int compatibilityScore(Class<?> declared, Class<?> actual) {
        if (declared == actual) return 2;
        if (declared.isAssignableFrom(actual)) return 1;
        return 0;
    }

    // ==================== Key 类型 ====================

    record BinaryOpKey(String operator, Class<?> lhsType, Class<?> rhsType) {}

    record UnaryOpKey(String operator, Class<?> operandType) {}
}
