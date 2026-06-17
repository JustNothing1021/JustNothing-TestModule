package com.justnothing.engine.parser;

import com.justnothing.engine.ast.ASTNode;
import com.justnothing.engine.ast.GenericType;
import com.justnothing.engine.ast.nodes.ClassReferenceNode;
import com.justnothing.engine.ast.nodes.FieldAccessNode;
import com.justnothing.engine.ast.nodes.LambdaNode;
import com.justnothing.engine.ast.nodes.MethodReferenceNode;
import com.justnothing.engine.ast.nodes.VariableNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析期方法重载选择器。
 * <p>
 * 负责在编译期（解析阶段）为方法调用绑定具体的 {@link Method} 对象，
 * 实现 Java JLS 15.12.2 风格的方法分派算法。
 * </p>
 *
 * <h3>核心能力</h3>
 * <ul>
 *   <li>目标类型推断：从 AST 节点 + typeMap 推断方法接收者类型</li>
 *   <li>适用性筛选：精确匹配 / widening / 装箱拆箱 / varargs</li>
 *   <li>最具体选择：多候选时按 JLS 15.12.2.5 选出最具体重载</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>
 *   MethodResolver resolver = new MethodResolver(context);
 *   Method bound = resolver.resolve(target, "println", args);
 *   if (bound != null) { methodCallNode.setBoundMethod(bound); }
 * </pre>
 *
 * @see com.justnothing.engine.ast.nodes.MethodCallNode
 */
public class MethodResolver {

    private final ParseContext context;

    public MethodResolver(ParseContext context) {
        this.context = context;
    }

    // ==================== 公共 API ====================

    /**
     * 为方法调用解析并绑定最匹配的重载。
     *
     * @param target      调用目标 AST 节点（null 表示无目标/静态调用）
     * @param methodName  方法名
     * @param args        参数节点列表
     * @return 绑定的 {@link Method}，无法确定时返回 null（不抛异常）
     */
    public Method resolve(ASTNode target, String methodName, List<ASTNode> args) {
        Class<?> targetClass = inferTargetClass(target);
        if (targetClass == null) {
            return null;
        }

        Class<?>[] argTypes = extractArgTypes(args);

        try {
            List<Method> candidates = findApplicableMethods(targetClass, methodName, target, argTypes);

            if (candidates.isEmpty()) {
                return null;
            }
            if (candidates.size() == 1) {
                return candidates.get(0);
            }

            return selectMostSpecific(candidates, argTypes);

        } catch (SecurityException e) {
            return null;
        }
    }

    // ==================== 泛型参数替换 ====================

    /**
     * 获取调用目标的声明类型（含泛型参数）。
     * <p>
     * 例如 {@code Map<String, String> s} → GenericType(rawType=Map, typeArgs=[String, String])
     *
     * @param target 方法调用的目标节点
     * @return 声明类型（可能不含泛型信息时 typeArguments 为空）
     */
    public GenericType inferTargetGenericType(ASTNode target) {
        if (target instanceof VariableNode v) {
            return context.getDeclaredType(v.getName());
        }
        // 其他类型从 typeMap 取 JType 再转 GenericType
        JType jtype = context.getType(target);
        return jtype != null ? context.getInferredType(target) : null;
    }

    /**
     * 对已绑定方法执行泛型参数替换，返回替换后的参数类型数组。
     * <p>
     * 例如：{@code Map<String, String>} 的 {@code put(K, V)} 方法，
     * 替换后返回 {@code [String.class, String.class]}。
     *
     * @param method      已绑定的方法（通过反射获取）
     * @param targetType  目标的声明类型（含泛型参数）
     * @return 替换后的参数类型数组；无法替换时回退到擦除类型
     */
    public Class<?>[] substituteGenericParameters(Method method, GenericType targetType) {
        if (targetType == null || !targetType.isGeneric()) {
            // 无泛型信息，使用擦除类型
            return method.getParameterTypes();
        }

        Type[] genericParamTypes = method.getGenericParameterTypes();
        List<GenericType> typeArgs = targetType.getTypeArguments();

        if (typeArgs.isEmpty() || genericParamTypes.length == 0) {
            return method.getParameterTypes();
        }

        // 收集类型变量映射：从方法声明类的 TypeVariable 到实际类型参数
        Map<String, Class<?>> substitutionMap = buildSubstitutionMap(method.getDeclaringClass(), typeArgs);

        Class<?>[] result = new Class<?>[genericParamTypes.length];
        for (int i = 0; i < genericParamTypes.length; i++) {
            result[i] = resolveType(genericParamTypes[i], substitutionMap);
        }
        return result;
    }

    /**
     * 构建类型变量→实际类型的映射表。
     * <p>
     * 例如 Map&lt;K, V&gt; 的类型参数为 [K, V]，
     * 实际类型参数为 [String, Integer] 时，
     * 映射为 {K→String, V→Integer}。
     */
    private Map<String, Class<?>> buildSubstitutionMap(Class<?> declaringClass,
                                                        List<GenericType> actualTypeArgs) {
        Map<String, Class<?>> map = new HashMap<>();
        TypeVariable<?>[] typeParams = declaringClass.getTypeParameters();

        for (int i = 0; i < typeParams.length && i < actualTypeArgs.size(); i++) {
            String varName = typeParams[i].getName();
            GenericType arg = actualTypeArgs.get(i);
            Class<?> actualRaw = resolveWildcardBound(arg);
            if (actualRaw != null) {
                map.put(varName, actualRaw);
            }
        }
        return map;
    }

    /**
     * 解析类型参数的实际类型，处理通配符上界。
     * <p>
     * 对于 {@code ? extends Number}，返回 Number.class 而非 Object.class。
     * 对于普通类型（如 String），直接返回 rawType。
     *
     * @param typeArg 泛型参数
     * @return 实际类型；无法确定时返回 null
     */
    private Class<?> resolveWildcardBound(GenericType typeArg) {
        if (typeArg == null) return null;

        // ★ 检查通配符: originalTypeName 编码了 ? extends / ? super 信息
        String origName = typeArg.getOriginalTypeName();
        if (origName != null) {
            if (origName.startsWith("? extends ")) {
                String boundName = origName.substring("? extends ".length());
                Class<?> bound = context.resolveClass(boundName);
                if (bound != null) return bound;
                // 回退：尝试基本类型
                return switch (boundName) {
                    case "int" -> int.class; case "long" -> long.class;
                    case "double" -> double.class; case "float" -> float.class;
                    case "boolean" -> boolean.class; case "char" -> char.class;
                    case "byte" -> byte.class; case "short" -> short.class;
                    default -> typeArg.getRawType();
                };
            }
            if (origName.startsWith("? super ")) {
                // 下界通配符：使用 rawType（通常是 Object）
                return typeArg.getRawType();
            }
            if (origName.equals("?")) {
                // 无界通配符：使用 Object
                return Object.class;
            }
        }

        // 非通配符：直接使用 rawType
        return typeArg.getRawType();
    }

    /**
     * 将单个 java.lang.reflect.Type 解析为 Class&lt;?&gt;。
     * <p>
     * 支持：
     * <ul>
     *   <li>TypeVariable → 查映射表</li>
     *   <li>ParameterizedType → 取原始类型</li>
     *   <li>Class → 直接返回</li>
     * </ul>
     */
    private Class<?> resolveType(Type type, Map<String, Class<?>> substitutionMap) {
        if (type instanceof TypeVariable<?> tv) {
            Class<?> substituted = substitutionMap.get(tv.getName());
            return substituted != null ? substituted : Object.class;
        }
        if (type instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            return raw instanceof Class<?> c ? c : Object.class;
        }
        if (type instanceof Class<?> c) {
            return c;
        }
        return Object.class;
    }

    /**
     * 对已绑定方法的返回类型执行泛型参数替换。
     * <p>
     * 例如 {@code Map<String, Integer>} 的 {@code get(K)} 方法，
     * 返回类型 {@code V} 替换后为 {@code Integer.class}。
     *
     * @param method     已绑定的方法
     * @param targetType 目标声明类型（含泛型参数）
     * @return 替换后的返回类型；无法替换时返回方法擦除返回类型
     */
    public Class<?> substituteReturnType(Method method, GenericType targetType) {
        if (targetType == null || !targetType.isGeneric()) {
            return method.getReturnType();
        }
        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof TypeVariable<?> tv) {
            List<GenericType> typeArgs = targetType.getTypeArguments();
            Map<String, Class<?>> substitutionMap = buildSubstitutionMap(method.getDeclaringClass(), typeArgs);
            Class<?> substituted = substitutionMap.get(tv.getName());
            return substituted != null ? substituted : method.getReturnType();
        }
        if (genericReturnType instanceof ParameterizedType pt) {
            List<GenericType> typeArgs = targetType.getTypeArguments();
            Map<String, Class<?>> substitutionMap = buildSubstitutionMap(method.getDeclaringClass(), typeArgs);
            Type raw = pt.getRawType();
            if (raw instanceof Class<?> rawClass) {
                return rawClass;
            }
        }
        return method.getReturnType();
    }

    /**
     * 验证方法调用的参数是否与泛型替换后的参数类型兼容。
     * <p>
     * 当目标有声明泛型类型时（如 {@code Map<String, String>}），
     * 此方法会做比原始 {@link #isApplicable} 更精确的检查。
     *
     * @param method      绑定的方法
     * @param targetType  目标声明类型（含泛型）
     * @param args        实际参数列表
     * @return 参数全部兼容返回 true；否则返回 false
     */
    public boolean isGenericApplicable(Method method, GenericType targetType, List<ASTNode> args) {
        Class<?>[] substitutedParams = substituteGenericParameters(method, targetType);
        Class<?>[] argTypes = extractArgTypes(args);

        if (!method.isVarArgs()) {
            if (substitutedParams.length != argTypes.length) {
                return false;
            }
            for (int i = 0; i < substitutedParams.length; i++) {
                if (!isAssignable(argTypes[i], substitutedParams[i])) {
                    return false;
                }
            }
            return true;
        }

        // 可变参数处理
        int fixedCount = substitutedParams.length - 1;
        if (argTypes.length < fixedCount) return false;
        for (int i = 0; i < fixedCount; i++) {
            if (!isAssignable(argTypes[i], substitutedParams[i])) return false;
        }
        Class<?> componentType = substitutedParams[fixedCount].getComponentType();
        if (componentType == null) componentType = substitutedParams[fixedCount];
        for (int i = fixedCount; i < argTypes.length; i++) {
            if (!isAssignable(argTypes[i], componentType)) return false;
        }
        return true;
    }

    /**
     * 从调用目标 AST 节点推断目标类。
     * <p>
     * 支持的目标类型：
     * <ul>
     *   <li>{@link ClassReferenceNode} → 类本身（静态调用）</li>
     *   <li>{@link FieldAccessNode} → 字段声明类型（如 System.out → PrintStream）</li>
     *   <li>{@link VariableNode} → 从 typeMap 查声明类型</li>
     *   <li>其他 → 直接查 typeMap</li>
     * </ul>
     */
    public Class<?> inferTargetClass(ASTNode target) {
        if (target instanceof ClassReferenceNode cr) {
            JType refType = context.getType(cr);
            return refType != null ? refType.getRawType() : context.resolveClass(cr.getTypeName());
        }

        if (target instanceof FieldAccessNode fa) {
            // 优先递归+反射推断精确类型（如 System.out → PrintStream）
            Class<?> ownerClass = inferTargetClass(fa.getTarget());
            if (ownerClass != null) {
                try {
                    Field f = ownerClass.getField(fa.getFieldName());
                    return f.getType();
                } catch (NoSuchFieldException ignored) {
                    // 反射中不存在该字段
                }
            }
            // 回退到 typeMap（可能是不精确的 Object 占位）
            JType fieldType = context.getType(fa);
            if (fieldType != null && fieldType.getRawType() != null) {
                return fieldType.getRawType();
            }
        }

        if (target instanceof VariableNode v) {
            JType varType = context.getType(v);
            if (varType != null) {
                return varType.getRawType();
            }
        }

        JType targetType = context.getType(target);
        return targetType != null ? targetType.getRawType() : null;
    }

    // ==================== 参数类型提取 ====================

    /**
     * 从参数节点的 typeMap 中提取参数运行时类型数组。
     * <p>
     * Lambda/方法引用节点返回对应的标记类型（LambdaNode.class / MethodReferenceNode.class），
     * 供 {@link #isAssignable} 检查函数式接口兼容性。
     * 无法确定类型的参数使用 {@code Object.class} 占位。
     */
    public Class<?>[] extractArgTypes(List<ASTNode> args) {
        Class<?>[] types = new Class<?>[args.size()];
        for (int i = 0; i < args.size(); i++) {
            ASTNode arg = args.get(i);
            if (arg instanceof LambdaNode) {
                types[i] = LambdaNode.class;
            } else if (arg instanceof MethodReferenceNode) {
                types[i] = MethodReferenceNode.class;
            } else {
                JType argType = context.getType(arg);
                types[i] = (argType != null && argType.getRawType() != null)
                        ? argType.getRawType()
                        : Object.class;
            }
        }
        return types;
    }

    // ==================== 适用性判断 (JLS 15.12.2.2) ====================

    /**
     * 在目标类中查找所有适用的同名方法候选列表。
     */
    private List<Method> findApplicableMethods(Class<?> targetClass, String methodName,
                                                ASTNode target, Class<?>[] argTypes) {
        Method[] allMethods = targetClass.getMethods();
        List<Method> candidates = new ArrayList<>(4);

        for (Method m : allMethods) {
            if (!m.getName().equals(methodName)) {
                continue;
            }
            // 静态/实例过滤
            if (!(target instanceof ClassReferenceNode)) {
                if (Modifier.isStatic(m.getModifiers()) && !isInheritedStatic(m, targetClass)) {
                    continue;
                }
            }
            if (isApplicable(m, argTypes)) {
                candidates.add(m);
            }
        }

        return candidates;
    }

    /**
     * 判断方法是否适用于给定参数类型（JLS 15.12.2.2）。
     * <p>
     * 支持：
     * <ul>
     *   <li>精确匹配</li>
     *   <li>基本类型 widening（byte→short→int→long→float→double）</li>
     *   <li>自动装箱/拆箱（int↔Integer 等）</li>
     *   <li>可变参数（varargs）</li>
     * </ul>
     */
    public boolean isApplicable(Method method, Class<?>[] argTypes) {
        Class<?>[] paramTypes = method.getParameterTypes();

        if (!method.isVarArgs()) {
            if (paramTypes.length != argTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!isAssignable(argTypes[i], paramTypes[i])) {
                    return false;
                }
            }
            return true;
        }

        // 可变参数：固定部分 + varargs 部分
        int fixedCount = paramTypes.length - 1;
        if (argTypes.length < fixedCount) {
            return false;
        }

        for (int i = 0; i < fixedCount; i++) {
            if (!isAssignable(argTypes[i], paramTypes[i])) {
                return false;
            }
        }

        Class<?> componentType = paramTypes[fixedCount].getComponentType();
        for (int i = fixedCount; i < argTypes.length; i++) {
            if (!isAssignable(argTypes[i], componentType)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 判断 fromType 是否可以赋值给 toType（含 widening、装箱转换和 lambda/FI 隐式转换）。
     */
    public boolean isAssignable(Class<?> fromType, Class<?> toType) {
        if (toType.isAssignableFrom(fromType)) {
            return true;
        }

        // Lambda/方法引用标记类型 → 函数式接口
        if ((fromType == LambdaNode.class || fromType == MethodReferenceNode.class)
                && isFunctionalInterface(toType)) {
            return true;
        }

        if (fromType.isPrimitive() && toType.isPrimitive()) {
            return isWideningPrimitive(fromType, toType);
        }

        if (fromType.isPrimitive()) {
            return toType.isAssignableFrom(box(fromType));
        }

        if (toType.isPrimitive()) {
            Class<?> unboxed = unbox(fromType);
            return unboxed != null && isWideningPrimitive(unboxed, toType);
        }

        return false;
    }

    // ==================== 最具体方法选择 (JLS 15.12.2.5) ====================

    /**
     * 从多个适用候选中选择最具体的方法（JLS 15.12.2.5 简化版）。
     * <p>
     * 方法 A 比 B 更具体 ↔ A 的每个参数类型都可赋值给 B 的对应参数类型，
     * 且至少有一个更严格。
     * </p>
     *
     * @return 最具体的 Method；有歧义时返回第一个候选
     */
    private Method selectMostSpecific(List<Method> candidates, Class<?>[] argTypes) {
        Method best = candidates.get(0);

        for (int i = 1; i < candidates.size(); i++) {
            Method current = candidates.get(i);
            if (isMoreSpecific(current, best)) {
                best = current;
            }
        }

        return best;
    }

    /**
     * 判断方法 a 是否比方法 b 更具体（JLS 15.12.2.5）。
     * <p>
     * 使用统一的 {@link #isAssignable} 处理原始类型/引用类型的所有组合，
     * 无需对装箱、数值提升做特判。
     */
    private boolean isMoreSpecific(Method a, Method b) {
        Class<?>[] paramsA = a.getParameterTypes();
        Class<?>[] paramsB = b.getParameterTypes();

        int len = Math.min(paramsA.length, paramsB.length);
        if (a.isVarArgs()) len = paramsA.length - 1;
        if (b.isVarArgs()) len = Math.min(len, paramsB.length - 1);

        boolean atLeastAsSpecific = true;
        boolean moreSpecific = false;

        for (int i = 0; i < len; i++) {
            Class<?> pa = paramsA[i];
            Class<?> pb = paramsB[i];

            // 统一判断：pa 能否赋值给 pb（a 的参数范围 ≤ b 的参数范围 → b 更具体）
            boolean aToB = isAssignable(pa, pb);
            // 反向：pb 能否赋值给 pa
            boolean bToA = isAssignable(pb, pa);

            if (!aToB) {
                atLeastAsSpecific = false;
                break;
            }
            if (!bToA) {
                moreSpecific = true;
            }
        }

        return atLeastAsSpecific && moreSpecific;
    }

    // ==================== 内部工具方法 ====================

    /** 基本类型 widening 层次表（JLS 5.1.2）。 */
    private static final Class<?>[][] WIDENING_HIERARCHY = {
            {byte.class, short.class, int.class, long.class, float.class, double.class},
            {short.class, int.class, long.class, float.class, double.class},
            {char.class, int.class, long.class, float.class, double.class},
            {int.class, long.class, float.class, double.class},
            {long.class, float.class, double.class},
            {float.class, double.class}
    };

    private static boolean isWideningPrimitive(Class<?> from, Class<?> to) {
        if (from == to) return true;
        for (Class<?>[] chain : WIDENING_HIERARCHY) {
            int fromIdx = -1, toIdx = -1;
            for (int i = 0; i < chain.length; i++) {
                if (chain[i] == from) fromIdx = i;
                if (chain[i] == to) toIdx = i;
            }
            if (fromIdx >= 0 && toIdx > fromIdx) return true;
        }
        return false;
    }

    private static Class<?> box(Class<?> primitive) {
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == double.class) return Double.class;
        if (primitive == float.class) return Float.class;
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == char.class) return Character.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == short.class) return Short.class;
        if (primitive == void.class) return Void.class;
        return primitive;
    }

    private static Class<?> unbox(Class<?> wrapper) {
        if (wrapper == Integer.class) return int.class;
        if (wrapper == Long.class) return long.class;
        if (wrapper == Double.class) return double.class;
        if (wrapper == Float.class) return float.class;
        if (wrapper == Boolean.class) return boolean.class;
        if (wrapper == Character.class) return char.class;
        if (wrapper == Byte.class) return byte.class;
        if (wrapper == Short.class) return short.class;
        if (wrapper == Void.class) return void.class;
        return null;
    }

    /**
     * 判断一个 AST 节点是否为 lambda 或方法引用。
     */
    public static boolean isLambdaOrMethodRefNode(ASTNode node) {
        return node instanceof LambdaNode || node instanceof MethodReferenceNode;
    }

    /**
     * 判断一个接口是否为函数式接口（有且仅有一个非 Object 的抽象方法）。
     */
    public static boolean isFunctionalInterface(Class<?> clazz) {
        if (clazz == null || !clazz.isInterface()) return false;
        int count = 0;
        for (Method m : clazz.getMethods()) {
            if (m.getDeclaringClass() == Object.class) continue;
            if (Modifier.isAbstract(m.getModifiers()) && !m.isDefault()) {
                count++;
                if (count > 1) return false;
            }
        }
        return count == 1;
    }

    /**
     * 获取函数式接口的唯一抽象方法（SAM）。
     */
    public static Method getSAM(Class<?> fiClass) {
        if (fiClass == null || !fiClass.isInterface()) return null;
        for (Method m : fiClass.getMethods()) {
            if (m.getDeclaringClass() == Object.class) continue;
            if (Modifier.isAbstract(m.getModifiers()) && !m.isDefault()) {
                return m;
            }
        }
        return null;
    }

    /**
     * 为已绑定的方法标注其参数中 lambda/方法引用的目标函数式接口类型。
     * 在方法解析完成后调用。
     *
     * @param method      已绑定的方法
     * @param args        实际参数节点列表
     * @param targetType  目标的声明泛型类型（用于替换泛型参数类型）
     */
    public void annotateFunctionalInterfaceArgs(Method method, List<ASTNode> args, GenericType targetType) {
        Class<?>[] paramTypes = method.getParameterTypes();
        // 先尝试获取泛型替换后的参数类型
        Class<?>[] substituted = null;
        if (targetType != null && targetType.isGeneric()) {
            substituted = substituteGenericParameters(method, targetType);
        }

        for (int i = 0; i < args.size(); i++) {
            Class<?> targetFiType;
            if (substituted != null && i < substituted.length) {
                targetFiType = substituted[i];
            } else if (i < paramTypes.length) {
                targetFiType = paramTypes[i];
            } else {
                // varargs 情况
                targetFiType = paramTypes[paramTypes.length - 1].getComponentType();
            }

            if (!isFunctionalInterface(targetFiType)) continue;

            ASTNode arg = args.get(i);
            if (arg instanceof LambdaNode lambda) {
                lambda.setFunctionalInterfaceType(targetFiType);
            } else if (arg instanceof MethodReferenceNode methodRef) {
                methodRef.setFunctionalInterfaceType(targetFiType);
            }
        }
    }

    /**
     * 标注构造器参数中的 lambda/方法引用为函数式接口类型。
     */
    public void annotateConstructorFunctionalInterfaceArgs(Class<?> targetClass, List<ASTNode> args) {
        if (targetClass == null || args == null || args.isEmpty()) return;

        java.lang.reflect.Constructor<?>[] ctors = targetClass.getConstructors();
        for (java.lang.reflect.Constructor<?> ctor : ctors) {
            Class<?>[] paramTypes = ctor.getParameterTypes();
            if (args.size() != paramTypes.length && !ctor.isVarArgs()) continue;
            if (args.size() < paramTypes.length - (ctor.isVarArgs() ? 1 : 0)) continue;

            // 检查所有 lambda/method-ref arg 对应的参数类型是否为 FI
            boolean valid = true;
            for (int i = 0; i < args.size(); i++) {
                ASTNode arg = args.get(i);
                if (arg instanceof LambdaNode || arg instanceof MethodReferenceNode) {
                    Class<?> pt = paramTypes[Math.min(i, paramTypes.length - 1)];
                    if (ctor.isVarArgs() && i >= paramTypes.length - 1) {
                        pt = pt.getComponentType();
                    }
                    if (!isFunctionalInterface(pt)) { valid = false; break; }
                }
            }
            if (!valid) continue;

            // 标注
            for (int i = 0; i < args.size(); i++) {
                ASTNode arg = args.get(i);
                Class<?> pt = paramTypes[Math.min(i, paramTypes.length - 1)];
                if (ctor.isVarArgs() && i >= paramTypes.length - 1) {
                    pt = pt.getComponentType();
                }
                if (!isFunctionalInterface(pt)) continue;
                if (arg instanceof LambdaNode lambda) {
                    lambda.setFunctionalInterfaceType(pt);
                } else if (arg instanceof MethodReferenceNode methodRef) {
                    methodRef.setFunctionalInterfaceType(pt);
                }
            }
            return;
        }
    }

    /**
     * 判断静态方法是否从父类继承（而非当前类声明）。
     * <p>
     * 允许通过实例调用继承来的静态方法。
     */
    private static boolean isInheritedStatic(Method method, Class<?> targetClass) {
        return !method.getDeclaringClass().equals(targetClass);
    }
}
