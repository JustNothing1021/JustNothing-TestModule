package com.justnothing.testmodule.utils.reflect;

import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ReflectionUtils {

    private static final Logger logger = Logger.getLoggerForName("ReflectionUtils");

    public static boolean isPrimitiveWrapperMatch(Class<?> target, Class<?> source) {
        if (target == int.class && source == Integer.class)
            return true;
        if (target == Integer.class && source == int.class)
            return true;
        if (target == long.class && source == Long.class)
            return true;
        if (target == Long.class && source == long.class)
            return true;
        if (target == float.class && source == Float.class)
            return true;
        if (target == Float.class && source == float.class)
            return true;
        if (target == double.class && source == Double.class)
            return true;
        if (target == Double.class && source == double.class)
            return true;
        if (target == boolean.class && source == Boolean.class)
            return true;
        if (target == Boolean.class && source == boolean.class)
            return true;
        if (target == char.class && source == Character.class)
            return true;
        if (target == Character.class && source == char.class)
            return true;
        if (target == byte.class && source == Byte.class)
            return true;
        if (target == Byte.class && source == byte.class)
            return true;
        if (target == short.class && source == Short.class)
            return true;
        return target == Short.class && source == short.class;
    }


    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, List<Class<?>> usingArgTypes,
                                           boolean isVarArgs) {
        return ClassResolver.isApplicableArgs(methodArgsTypes, usingArgTypes, isVarArgs);
    }

    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, Class<?>[] usingArgTypes, boolean isVarArgs) {
        return isApplicableArgs(methodArgsTypes, Arrays.asList(usingArgTypes), isVarArgs);
    }

    public static boolean isApplicableArgs(List<Class<?>> methodArgsTypes, List<Class<?>> usingArgTypes,
                                           boolean isVarArgs) {
        return isApplicableArgs(methodArgsTypes.toArray(new Class<?>[0]), usingArgTypes, isVarArgs);
    }

    public static boolean isApplicableArgs(List<Class<?>> methodArgsTypes, Class<?>[] usingArgTypes,
                                           boolean isVarArgs) {
        return isApplicableArgs(methodArgsTypes.toArray(new Class<?>[0]), usingArgTypes, isVarArgs);
    }


    public static Method[] findAllMethods(Class<?> clazz, String methodName, String signature, ClassLoader classLoader)
            throws NoSuchMethodException, ClassNotFoundException {
        logger.debug("在类 " + clazz.getName() + " 中查找方法: " + methodName
                + (signature != null ? " (签名: " + signature + ")" : ""));

        Method[] methods = clazz.getDeclaredMethods();
        List<Method> candidates = new ArrayList<>();

        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                candidates.add(method);
                logger.debug("找到候选方法: " + method + " (参数: " + Arrays.toString(method.getParameterTypes()) + ")");
            }
        }

        if (candidates.isEmpty()) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                logger.debug("在父类 " + superClass.getName() + " 中继续查找");
                return findAllMethods(superClass, methodName, signature, classLoader);
            }

            StringBuilder availableMethods = new StringBuilder("可用方法:\n");
            for (Method m : clazz.getDeclaredMethods()) {
                availableMethods.append("  ").append(m).append("\n");
            }
            logger.error("未找到方法 " + methodName + "\n" + availableMethods);
            throw new NoSuchMethodException(
                    "未找到方法: " + methodName + " 在类 " + clazz.getName() + "\n" + availableMethods);
        }

        if (signature == null || signature.isEmpty()) {
            logger.debug("未指定签名，返回所有方法");
            return candidates.toArray(new Method[0]);
        }

        Class<?>[] expected = SignatureUtils.parseSignature(signature, classLoader);
        for (Method method : candidates) {
            if (ClassResolver.isApplicableArgs(method.getParameterTypes(), expected, method.isVarArgs())) {
                return new Method[] { method };
            }
        }

        StringBuilder errorMsg = new StringBuilder("未找到匹配签名的方法: " + methodName + " (签名: " + signature + ")\n");
        errorMsg.append("候选方法:\n");
        for (Method m : candidates) {
            errorMsg.append("  ").append(m).append("\n");
        }
        logger.error(errorMsg.toString());
        throw new NoSuchMethodException(errorMsg.toString());
    }

    public static Constructor<?>[] findAllConstructors(Class<?> clazz, String signature, ClassLoader classLoader)
            throws NoSuchMethodException, ClassNotFoundException {
        logger.debug("在类 " + clazz.getName() + " 中查找构造函数"
                + (signature != null ? " (签名: " + signature + ")" : ""));

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        List<Constructor<?>> candidates = new ArrayList<>();

        for (Constructor<?> constructor : constructors) {
            candidates.add(constructor);
            logger.debug("找到候选构造函数: " + Arrays.toString(constructors));
        }

        if (candidates.isEmpty()) {
            StringBuilder availableConstructors = new StringBuilder("可用构造函数:\n");
            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                availableConstructors.append("  ").append(c).append("\n");
            }
            logger.error("未找到构造函数\n" + availableConstructors);
            throw new NoSuchMethodException(
                    "在类" + clazz.getName() + "中未找到构造函数\n" + availableConstructors);
        }

        if (signature == null || signature.isEmpty()) {
            logger.debug("未指定签名，返回所有构造函数");
            return candidates.toArray(new Constructor<?>[0]);
        }

        Class<?>[] expected = SignatureUtils.parseSignature(signature, classLoader);
        for (Constructor<?> constructor : candidates) {
            if (ClassResolver.isApplicableArgs(constructor.getParameterTypes(), expected, constructor.isVarArgs())) {
                return new Constructor<?>[] { constructor };
            }
        }

        StringBuilder errorMsg = new StringBuilder("未找到匹配签名的构造函数: (签名: " + signature + ")\n");
        errorMsg.append("候选构造函数:\n");
        for (Constructor<?> c : candidates) {
            errorMsg.append("  ").append(c).append("\n");
        }
        logger.error(errorMsg.toString());
        throw new NoSuchMethodException(errorMsg.toString());
    }

    public static Constructor<?> findConstructor(Class<?> clazz, Class<?>[] paramTypes) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == paramTypes.length) {
                Class<?>[] constructorParams = constructor.getParameterTypes();
                if (ClassResolver.isApplicableArgs(constructorParams, paramTypes, constructor.isVarArgs()))
                    return constructor;
            }
        }
        return null;
    }

    private static Constructor<?> findConstructorOrFail(Class<?> clazz, Class<?>[] paramTypes) throws NoSuchMethodException {
        Constructor<?> constructor = findConstructor(clazz, paramTypes);
        if (constructor == null) throw new NoSuchMethodException(
                "没有找到" + clazz + "对应指定签名的构造函数: " + Arrays.toString(paramTypes));
        return constructor;
    }


    public static Object parseValue(String value, Class<?> type) {
        if (value == null) {
            return null;
        }

        try {
            if (type == String.class) {
                return value;
            } else if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            } else if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            } else if (type == float.class || type == Float.class) {
                return Float.parseFloat(value);
            } else if (type == double.class || type == Double.class) {
                return Double.parseDouble(value);
            } else if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (type == byte.class || type == Byte.class) {
                return Byte.parseByte(value);
            } else if (type == short.class || type == Short.class) {
                return Short.parseShort(value);
            } else if (type == char.class || type == Character.class) {
                if (value.length() == 1) {
                    return value.charAt(0);
                }
                throw new IllegalArgumentException("字符值必须是一个字符");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法将 '" + value + "' 转换为 " + type.getSimpleName());
        }

        return value;
    }

    public static String[] parseParams(String paramsStr) {
        if (paramsStr == null || paramsStr.trim().isEmpty()) {
            return new String[0];
        }

        List<String> params = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : paramsStr.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    params.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            params.add(current.toString());
        }

        return params.toArray(new String[0]);
    }

    public static Object[] convertParams(String[] params, Class<?>[] paramTypes) {
        if (params == null || params.length == 0) {
            return new Object[0];
        }

        Object[] result = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (i < paramTypes.length) {
                result[i] = parseValue(params[i], paramTypes[i]);
            } else {
                result[i] = parseValue(params[i], String.class);
            }
        }

        return result;
    }

    public static String getDescriptor(Member member) {
        return getDescriptor(member, false);
    }

    public static String getDescriptor(Member member, boolean simple) {
        StringBuilder sb = new StringBuilder();
        int modifiers = member.getModifiers();

        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");
        else sb.append("[package-private] ");

        if (Modifier.isStatic(modifiers)) sb.append("static ");
        if (Modifier.isFinal(modifiers)) sb.append("final ");
        if (Modifier.isSynchronized(modifiers)) sb.append("synchronized ");
        if (Modifier.isNative(modifiers)) sb.append("native ");

        if (Modifier.isInterface(modifiers)) sb.append("interface ");
        if (Modifier.isVolatile(modifiers)) sb.append("volatile ");
        if (Modifier.isStrict(modifiers)) sb.append("strictfp ");
        if (Modifier.isTransient(modifiers)) sb.append("transient ");

        if (member instanceof Method method) {
            sb.append(simple ? method.getReturnType().getSimpleName() :
                               method.getReturnType().getName()).append(" ");
            sb.append(method.getName()).append("(");

            Class<?>[] params = method.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                sb.append(simple ? params[i].getSimpleName() : params[i].getName());
                if (i < params.length - 1) sb.append(", ");
            }
            sb.append(")");
        } else if (member instanceof Field field) {
            sb.append(simple ? member.getDeclaringClass().getName() + "." + field.getName() : field.getName())
                    .append(" ");
            sb.append(field.getName());
        } else if (member instanceof Constructor<?> constructor) {
            sb.append(simple ? constructor.getDeclaringClass().getSimpleName() :
                               constructor.getDeclaringClass().getName());
            sb.append(" (");
            Class<?>[] params = constructor.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                sb.append(simple ? params[i].getSimpleName() : params[i].getName());
                if (i < params.length - 1) sb.append(", ");
            }
            sb.append(")");
        }

        return sb.toString().trim();
    }


    public static boolean isTypeCompatible(Class<?> expected, Class<?> actual) {
        return ClassResolver.isTypeCompatible(expected, actual);
    }


    public static String getModifiersString(int modifiers) {
        return Modifier.toString(modifiers);
    }


    public static Object callMethod(Object object, String methodName, List<Object> args) throws Exception {
        if (methodName == null)
            methodName = "call";

        if (object == null) {
            throw new NullPointerException(
                    "Attempt to invoke method " + methodName + " on a null object reference");
        }

        Class<?> clazz = object.getClass();

        List<Class<?>> paramTypes = args.stream()
                .map(arg -> arg != null ? arg.getClass() : Void.class)
                        .collect(Collectors.toList());



        logger.debug("查找实例方法: " + clazz.getName() + "." + methodName + ", 参数类型: " + paramTypes);

        Method method = ClassResolver.findMethod(clazz.getName(), methodName, paramTypes.toArray());

        if (method == null) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().equals(methodName) && !Modifier.isStatic(m.getModifiers())) {
                    if (ClassResolver.isApplicableArgs(m.getParameterTypes(), paramTypes, m.isVarArgs())) {
                        method = m;
                        break;
                    }
                }
            }
        }

        if (method == null) {
            throw new NoSuchMethodException("Method not found: " + clazz.getName() + "." + methodName + " with args: " + paramTypes);
        }

        logger.debug("找到方法: " + method + ", 可变参数: " + method.isVarArgs());

        Object[] invokeArgs = prepareInvokeArguments(method, args);

        try {
            method.setAccessible(true);
        } catch (SecurityException ignored) {
        }
        try {
            return method.invoke(object, invokeArgs);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new Exception(cause);
            }
        }
    }

    /**
     * 直接调用 Method 对象的方法
     * @param method Method 对象
     * @param target 目标对象（对于静态方法为 null）
     * @param args 参数列表
     * @return 方法调用结果
     * @throws Exception 调用失败时抛出异常
     */
    public static Object callMethod(Method method, Object target, List<Object> args) throws Exception {
        if (method == null) {
            throw new NullPointerException("Method cannot be null");
        }

        Object[] invokeArgs = prepareInvokeArguments(method, args);
        try {
            method.setAccessible(true);
        } catch (SecurityException ignored) {
        }
        try {
            return method.invoke(target, invokeArgs);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new Exception(cause);
            }
        }
    }

    public static Object callStaticMethod(String className, String methodName, List<Object> args) throws Exception {
        Class<?> clazz = ClassResolver.findClass(className);
        if (clazz == null) {
            throw new ClassNotFoundException("Class not found: " + className);
        }

        List<Class<?>> paramTypes = args.stream()
                .map(arg -> arg != null ? arg.getClass() : Void.class)
                        .collect(Collectors.toList());

        logger.debug("查找静态方法: " + className + "." + methodName + ", 参数类型: " + paramTypes);

        Method method = ClassResolver.findMethod(clazz.getName(), methodName, paramTypes.toArray());

        if (method == null) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().equals(methodName) && Modifier.isStatic(m.getModifiers())) {
                    if (ClassResolver.isApplicableArgs(m.getParameterTypes(), paramTypes, m.isVarArgs())) {
                        method = m;
                        break;
                    }
                }
            }
        }

        if (method == null) {
            throw new NoSuchMethodException("Method not found: " + className + "." + methodName + " with args: " + paramTypes);
        }

        logger.debug("找到方法: " + method + ", 可变参数: " + method.isVarArgs());

        Object[] invokeArgs = prepareInvokeArguments(method, args);

        try {
            return method.invoke(null, invokeArgs);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw e;
            }
        }
    }

    private static Object[] prepareInvokeArguments(Method method, List<Object> args) {
        Class<?>[] paramTypes = method.getParameterTypes();

        if (!method.isVarArgs() || paramTypes.length == 0) {
            return args.toArray();
        }

        int fixedParamCount = paramTypes.length - 1;
        Class<?> varArgsType = paramTypes[fixedParamCount];
        Class<?> varArgsComponentType = varArgsType.getComponentType();
        assert varArgsComponentType != null;

        // 获取泛型类型信息（如果有）
        Type genericVarArgsType = method.getGenericParameterTypes()[fixedParamCount];
        Class<?> inferredElementType = inferVarArgElementType(genericVarArgsType, args, fixedParamCount);

        // 检查传入参数数量
        if (args.size() < fixedParamCount) {
            throw new IllegalArgumentException("Insufficient arguments");
        }

        Object[] invokeArgs = new Object[paramTypes.length];

        // 设置固定参数
        for (int i = 0; i < fixedParamCount; i++) {
            invokeArgs[i] = args.get(i);
        }

        // 处理可变参数部分
        int varArgCount = args.size() - fixedParamCount;

        if (varArgCount == 1) {
            Object lastArg = args.get(fixedParamCount);

            // 检查是否需要展开数组
            if (shouldExpandArrayForVarArgs(method, lastArg, inferredElementType)) {
                // 展开数组为多个参数
                int length = Array.getLength(lastArg);
                Object varArgArray = Array.newInstance(varArgsComponentType, length);
                for (int i = 0; i < length; i++) {
                    Array.set(varArgArray, i, Array.get(lastArg, i));
                }
                invokeArgs[fixedParamCount] = varArgArray;
            } else if (lastArg != null && lastArg.getClass().isArray() &&
                    isTypeCompatible(varArgsComponentType, lastArg.getClass().getComponentType())) {
                // 参数已经是正确的数组，直接使用
                invokeArgs[fixedParamCount] = lastArg;
            } else {
                // 创建单元素数组
                Object varArgArray = Array.newInstance(varArgsComponentType, 1);
                Array.set(varArgArray, 0, lastArg);
                invokeArgs[fixedParamCount] = varArgArray;
            }
        } else if (varArgCount == 0) {
            // 没有传入可变参数，创建空数组
            invokeArgs[fixedParamCount] = Array.newInstance(varArgsComponentType, 0);
        } else {
            // 多个可变参数，创建数组
            Object varArgArray = Array.newInstance(varArgsComponentType, varArgCount);
            for (int i = 0; i < varArgCount; i++) {
                Object arg = args.get(fixedParamCount + i);
                Array.set(varArgArray, i, arg);
            }
            invokeArgs[fixedParamCount] = varArgArray;
        }

        return invokeArgs;
    }

    private static Class<?> inferVarArgElementType(Type genericType, List<Object> args, int fixedParamCount) {
        if (genericType instanceof ParameterizedType) {
            // 处理泛型情况，如 List<T> asList(T... a)
            Type[] actualTypeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (actualTypeArgs.length > 0 && actualTypeArgs[0] instanceof Class) {
                return (Class<?>) actualTypeArgs[0];
            }
        } else if (genericType instanceof GenericArrayType) {
            // 处理泛型数组情况
            Type componentType = ((GenericArrayType) genericType).getGenericComponentType();
            if (componentType instanceof Class) {
                return (Class<?>) componentType;
            }
        }

        // 如果没有泛型信息，尝试从参数推断
        if (args.size() > fixedParamCount) {
            Object lastArg = args.get(fixedParamCount);
            if (lastArg != null && lastArg.getClass().isArray()) {
                return lastArg.getClass().getComponentType();
            }
        }

        return null;
    }

    private static boolean shouldExpandArrayForVarArgs(Method method, Object arrayArg, Class<?> inferredElementType) {
        if (arrayArg == null || !arrayArg.getClass().isArray()) {
            return false;
        }

        Class<?> varArgsType = method.getParameterTypes()[method.getParameterTypes().length - 1];
        Class<?> varArgsComponentType = varArgsType.getComponentType();
        Class<?> arrayComponentType = arrayArg.getClass().getComponentType();
        assert varArgsComponentType != null;

        // 对于像 Arrays.asList(T... a) 这样的泛型方法
        // 如果传入数组，且数组的元素类型可以赋值给可变参数的元素类型，则展开
        if (inferredElementType != null &&
                isTypeCompatible(inferredElementType, arrayComponentType)) {
            return true;
        }

        // 避免双重数组
        // 如果可变参数已经是数组类型（如 String[]...），则不展开
        if (varArgsComponentType.isArray()) {
            return false;
        }

        // 如果数组元素类型可以赋值给可变参数元素类型，考虑展开
        // 但这里需要小心，因为有时候确实需要传递数组作为单个参数
        // 这是最复杂的情况，需要根据具体方法决定
        // 但实际上我也不是很会补充，所以暂时返回false
        return false;
    }

    public Object callStaticMethod(Class<?> clazz, String methodName, List<Object> args) throws Exception {
        return callStaticMethod(clazz.getName(), methodName, args);
    }
}