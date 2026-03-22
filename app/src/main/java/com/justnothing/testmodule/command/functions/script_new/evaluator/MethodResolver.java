package com.justnothing.testmodule.command.functions.script_new.evaluator;

import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方法解析器
 * <p>
 * 负责处理方法调用的参数适配和签名选择。
 * 支持方法重载、隐式类型转换和可变参数。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class MethodResolver {
    
    /**
     * 方法解析结果
     */
    public static class ResolutionResult {
        private final Method method;
        private final Object[] convertedArgs;
        
        public ResolutionResult(Method method, Object[] convertedArgs) {
            this.method = method;
            this.convertedArgs = convertedArgs;
        }
        
        public Method getMethod() {
            return method;
        }
        
        public Object[] getConvertedArgs() {
            return convertedArgs;
        }
    }
    
    /**
     * 缓存键
     * <p>
     * 用于唯一标识一个方法查找请求。
     * 包含ClassLoader、Class、MethodName、ArgTypes。
     * </p>
     */
    private static class CacheKey {
        private final ClassLoader classLoader;
        private final Class<?> clazz;
        private final String methodName;
        private final Class<?>[] argTypes;
        
        public CacheKey(ClassLoader classLoader, Class<?> clazz, 
                       String methodName, Class<?>[] argTypes) {
            this.classLoader = classLoader;
            this.clazz = clazz;
            this.methodName = methodName;
            this.argTypes = argTypes;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return classLoader.equals(cacheKey.classLoader) &&
                   clazz.equals(cacheKey.clazz) &&
                   methodName.equals(cacheKey.methodName) &&
                   Arrays.equals(argTypes, cacheKey.argTypes);
        }
        
        @Override
        public int hashCode() {
            int result = classLoader.hashCode();
            result = 31 * result + clazz.hashCode();
            result = 31 * result + methodName.hashCode();
            result = 31 * result + Arrays.hashCode(argTypes);
            return result;
        }
    }
    
    /**
     * 方法缓存
     * <p>
     * 缓存已解析的方法，提高性能。
     * 键：ClassLoader + Class + MethodName + ArgTypes
     * 值：ResolutionResult（包含方法和转换后的参数）
     * </p>
     */
    private static final Map<CacheKey, ResolutionResult> methodCache = 
        new ConcurrentHashMap<>();
    
    /**
     * 解析方法调用
     * <p>
     * 根据参数列表选择最匹配的方法签名，并进行必要的类型转换。
     * </p>
     * 
     * @param classLoader 类加载器
     * @param clazz 目标类
     * @param methodName 方法名
     * @param args 参数列表
     * @return 解析结果
     * @throws Exception 如果没有匹配的方法
     */
    public static ResolutionResult resolve(ClassLoader classLoader, Class<?> clazz, 
                                      String methodName, List<Object> args) 
            throws Exception {
        Class<?>[] argTypes = new Class<?>[args.size()];
        for (int i = 0; i < args.size(); i++) {
            argTypes[i] = args.get(i) != null ? args.get(i).getClass() : Void.class;
        }
        
        CacheKey cacheKey = new CacheKey(classLoader, clazz, methodName, argTypes);
        ResolutionResult cachedResult = methodCache.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        List<Method> candidates = findCandidateMethods(clazz, methodName);
        
        if (candidates.isEmpty()) {
            throw new NoSuchMethodException(
                "Method not found: " + clazz.getName() + "." + methodName);
        }
        
        for (Method method : candidates) {
            ResolutionResult result = tryExactMatch(method, args);
            if (result != null) {
                methodCache.put(cacheKey, result);
                return result;
            }
        }
        
        List<ResolutionResult> compatibleResults = new ArrayList<>();
        for (Method method : candidates) {
            ResolutionResult result = tryCompatibleMatch(method, args);
            if (result != null) {
                compatibleResults.add(result);
            }
        }
        
        if (compatibleResults.isEmpty()) {
            throw new NoSuchMethodException(
                "No applicable method found: " + clazz.getName() + "." + methodName +
                " with args: " + getArgTypes(args));
        }
        
        if (compatibleResults.size() > 1) {
            StringBuilder errorMsg = new StringBuilder(
                "Ambiguous method call: " + clazz.getName() + "." + methodName +
                " with args: " + getArgTypes(args) + "\n");
            errorMsg.append("Candidate methods:\n");
            for (ResolutionResult result : compatibleResults) {
                errorMsg.append("  ").append(result.getMethod()).append("\n");
            }
            throw new NoSuchMethodException(errorMsg.toString());
        }
        
        ResolutionResult result = compatibleResults.get(0);
        methodCache.put(cacheKey, result);
        return result;
    }
    
    /**
     * 查找候选方法
     * <p>
     * 在目标类及其父类中查找所有同名方法。
     * </p>
     */
    private static List<Method> findCandidateMethods(Class<?> clazz, String methodName) {
        List<Method> candidates = new ArrayList<>();
        Class<?> current = clazz;
        
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    candidates.add(method);
                }
            }
            current = current.getSuperclass();
        }
        
        return candidates;
    }
    
    /**
     * 尝试完全匹配
     * <p>
     * 参数类型必须完全一致（包括null的处理）。
     * </p>
     */
    private static ResolutionResult tryExactMatch(Method method, List<Object> args) {
        Class<?>[] paramTypes = method.getParameterTypes();
        boolean isVarArgs = method.isVarArgs();
        
        if (!isVarArgs && paramTypes.length != args.size()) {
            return null;
        }
        
        if (isVarArgs && args.size() < paramTypes.length - 1) {
            return null;
        }
        
        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = args.get(i);
            Class<?> argType = arg != null ? arg.getClass() : Void.class;
            
            if (!isVarArgs || i < paramTypes.length - 1) {
                if (paramTypes[i] != argType) {
                    return null;
                }
            } else {
                Class<?> varArgsComponentType = paramTypes[i].getComponentType();
                for (int j = i; j < args.size(); j++) {
                    Object varArg = args.get(j);
                    Class<?> varArgType = varArg != null ? varArg.getClass() : Void.class;
                    if (varArgsComponentType != varArgType) {
                        return null;
                    }
                }
            }
        }
        
        return new ResolutionResult(method, args.toArray());
    }
    
    /**
     * 尝试兼容匹配
     * <p>
     * 使用ReflectionUtils.isTypeCompatible检查参数类型是否兼容。
     * </p>
     */
    private static ResolutionResult tryCompatibleMatch(Method method, List<Object> args) {
        Class<?>[] paramTypes = method.getParameterTypes();
        boolean isVarArgs = method.isVarArgs();
        
        if (!isVarArgs && paramTypes.length != args.size()) {
            return null;
        }
        
        if (isVarArgs && args.size() < paramTypes.length - 1) {
            return null;
        }
        
        Object[] convertedArgs = new Object[paramTypes.length];
        
        if (isVarArgs) {
            int fixedParamCount = paramTypes.length - 1;
            Class<?> varArgsType = paramTypes[fixedParamCount];
            Class<?> varArgsComponentType = varArgsType.getComponentType();
            
            for (int i = 0; i < fixedParamCount; i++) {
                Object arg = args.get(i);
                Class<?> argType = arg != null ? arg.getClass() : Void.class;
                
                if (!isTypeCompatible(paramTypes[i], argType)) {
                    return null;
                }
                
                convertedArgs[i] = convertArg(arg, paramTypes[i]);
            }
            
            Object varArgsArray = Array.newInstance(
                varArgsComponentType, args.size() - fixedParamCount);
            
            for (int i = fixedParamCount; i < args.size(); i++) {
                Object arg = args.get(i);
                Class<?> argType = arg != null ? arg.getClass() : Void.class;
                
                if (!isTypeCompatible(varArgsComponentType, argType)) {
                    return null;
                }
                
                Array.set(varArgsArray, i - fixedParamCount, 
                    convertArg(arg, varArgsComponentType));
            }
            
            convertedArgs[fixedParamCount] = varArgsArray;
        } else {
            for (int i = 0; i < paramTypes.length; i++) {
                Object arg = args.get(i);
                Class<?> argType = arg != null ? arg.getClass() : Void.class;
                
                if (!isTypeCompatible(paramTypes[i], argType)) {
                    return null;
                }
                
                convertedArgs[i] = convertArg(arg, paramTypes[i]);
            }
        }
        
        return new ResolutionResult(method, convertedArgs);
    }
    
    /**
     * 检查类型是否兼容
     * <p>
     * 使用ReflectionUtils.isTypeCompatible，并处理null可以适配所有Object。
     * </p>
     */
    private static boolean isTypeCompatible(Class<?> expected, Class<?> actual) {
        if (actual == Void.class) {
            return !expected.isPrimitive();
        }
        
        return ReflectionUtils.isTypeCompatible(expected, actual);
    }
    
    /**
     * 转换参数
     * <p>
     * 将参数转换为目标类型。
     * </p>
     */
    private static Object convertArg(Object arg, Class<?> targetType) {
        if (arg == null) {
            return null;
        }
        
        if (targetType.isInstance(arg)) {
            return arg;
        }
        
        if (targetType == String.class) {
            return arg.toString();
        }
        
        if (targetType.isPrimitive()) {
            return convertToPrimitive(arg, targetType);
        }
        
        return targetType.cast(arg);
    }
    
    /**
     * 转换为基本类型
     */
    private static Object convertToPrimitive(Object arg, Class<?> primitiveType) {
        if (arg == null) {
            return getDefaultValue(primitiveType);
        }
        
        if (arg instanceof Number) {
            Number number = (Number) arg;
            if (primitiveType == int.class) return number.intValue();
            if (primitiveType == long.class) return number.longValue();
            if (primitiveType == double.class) return number.doubleValue();
            if (primitiveType == float.class) return number.floatValue();
            if (primitiveType == byte.class) return number.byteValue();
            if (primitiveType == short.class) return number.shortValue();
        }
        
        if (arg instanceof Boolean && primitiveType == boolean.class) {
            return arg;
        }
        
        if (arg instanceof Character && primitiveType == char.class) {
            return arg;
        }
        
        if (arg instanceof String) {
            String str = (String) arg;
            if (primitiveType == int.class) return Integer.parseInt(str);
            if (primitiveType == long.class) return Long.parseLong(str);
            if (primitiveType == double.class) return Double.parseDouble(str);
            if (primitiveType == float.class) return Float.parseFloat(str);
            if (primitiveType == boolean.class) return Boolean.parseBoolean(str);
            if (primitiveType == byte.class) return Byte.parseByte(str);
            if (primitiveType == short.class) return Short.parseShort(str);
            if (primitiveType == char.class && str.length() == 1) {
                return str.charAt(0);
            }
        }
        
        return getDefaultValue(primitiveType);
    }
    
    /**
     * 获取基本类型的默认值
     */
    private static Object getDefaultValue(Class<?> primitiveType) {
        if (primitiveType == int.class) return 0;
        if (primitiveType == long.class) return 0L;
        if (primitiveType == double.class) return 0.0;
        if (primitiveType == float.class) return 0.0f;
        if (primitiveType == boolean.class) return false;
        if (primitiveType == byte.class) return (byte) 0;
        if (primitiveType == short.class) return (short) 0;
        if (primitiveType == char.class) return '\0';
        return null;
    }
    
    /**
     * 获取参数类型列表
     */
    private static String getArgTypes(List<Object> args) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            Object arg = args.get(i);
            sb.append(arg != null ? arg.getClass().getSimpleName() : "null");
        }
        sb.append("]");
        return sb.toString();
    }
}
