package com.justnothing.testmodule.command.functions.script;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.justnothing.testmodule.command.functions.script.ASTNodes.*;
import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.command.output.SystemOutputCollector;
import static com.justnothing.testmodule.command.functions.script.ScriptLogger.*;
import static com.justnothing.testmodule.command.functions.script.ScriptUtils.*;


import androidx.annotation.NonNull;

public class ScriptModels {

    public static class Variable {
        public Object value;
        public Class<?> type;

        public Variable(Object value) {
            this.value = value;
            this.type = value != null ? value.getClass() : Void.class;
        }

        public Variable(Object value, Class<?> type) {
            this.value = value;
            this.type = type;
        }
    }

    @FunctionalInterface
    public interface Lambda extends Function<Object[], Object> {
        Object call(Object... args);

        @Override
        default Object apply(Object[] args) {
            return call(args);
        }
    }

    public interface BuiltInFunction {
        Object call(List<Object> args);

    }
    
    public static class ExecutionContext {

        private final ConcurrentHashMap<String, Variable> variables;
        final ConcurrentHashMap<String, BuiltInFunction> builtIns;
        private final ConcurrentHashMap<String, Object> runtimeFlags;
        private final List<String> imports;
        private final ClassLoader classLoader;
        private IOutputHandler outputBuffer;
        private IOutputHandler warnMsgBuffer;
        private final ConcurrentHashMap<String, Class<?>> classCache;
        final ConcurrentHashMap<String, ClassDefinition> customClasses;
        private final ConcurrentHashMap<String, Method> methodCache;
        boolean shouldBreak;
        boolean shouldContinue;
        boolean shouldReturn;
        Object returnValue;
        private String currentMethodReturnType;

        private final List<Map<String, Variable>> scopeStack = new ArrayList<>();

        public ExecutionContext(ClassLoader classLoader) {
            logger.debug("创建ExecutionContext，类加载器: " + classLoader);
            this.classLoader = classLoader;
            this.runtimeFlags = new ConcurrentHashMap<>();
            this.outputBuffer = new SystemOutputCollector(System.out, System.in);
            this.warnMsgBuffer = new SystemOutputCollector(System.err, System.in);
            this.variables = new ConcurrentHashMap<>();
            this.builtIns = new ConcurrentHashMap<>();
            this.imports = new ArrayList<>();
            this.classCache = new ConcurrentHashMap<>();
            this.customClasses = new ConcurrentHashMap<>();
            this.methodCache = new ConcurrentHashMap<>();

            enterScope();

            setupDefaultImports();
            setupBuiltInFunctions();
            logger.debug("ExecutionContext初始化完成");
        }
        public ExecutionContext(ClassLoader classLoader,
                IOutputHandler builtInOutStream,
                IOutputHandler builtInErrStream) {
            logger.debug("创建ExecutionContext，类加载器: " + classLoader + ", 自定义输出流");
            this.classLoader = classLoader;
            this.outputBuffer = builtInOutStream;
            this.warnMsgBuffer = builtInErrStream;
            this.runtimeFlags = new ConcurrentHashMap<>();
            this.variables = new ConcurrentHashMap<>();
            this.builtIns = new ConcurrentHashMap<>();
            this.imports = new ArrayList<>();
            this.classCache = new ConcurrentHashMap<>();
            this.customClasses = new ConcurrentHashMap<>();
            this.methodCache = new ConcurrentHashMap<>();
            setupDefaultImports();
            setupBuiltInFunctions();
            enterScope();
            logger.debug("ExecutionContext初始化完成");
        }

        public String getOutput() {
            return outputBuffer.toString();
        }

        public void clearOutput() {
            outputBuffer.clear();
        }

        public void print(String text) {
            outputBuffer.print(text);
        }

        public void printf(String format, Object... args) {
            outputBuffer.printf(format, args);
        }

        public void println(String text) {
            outputBuffer.println(text);
        }

        public void printStackTrace(Throwable th) {
            outputBuffer.printStackTrace(th);
        }

        public String getWarnMessages() {
            return warnMsgBuffer.toString();
        }

        public void clearWarnMessages() {
            warnMsgBuffer.clear();
        }

        public void printWarn(String text) {
            warnMsgBuffer.print(text);
        }

        public void printfWarn(String format, Object... args) {
            warnMsgBuffer.printf(format, args);
        }

        public void printlnWarn(String text) {
            warnMsgBuffer.println(text);
        }

        public void printStackTraceWarn(Throwable th) {
            warnMsgBuffer.printStackTrace(th);
        }

        public Object getFlag(String key) {
            return runtimeFlags.get(key);
        }

        public void setFlag(String key, Object value) {
            runtimeFlags.put(key, value);
        }


        public void setVariable(String name, Object value) {
            if (scopeStack.isEmpty()) {
                enterScope();
            }

            Map<String, Variable> currentScope = scopeStack.get(scopeStack.size() - 1);
            currentScope.put(name, new Variable(value));
            // 同步更新variables
            variables.put(name, new Variable(value));
        }

        public Variable getVariable(String name) {
            // 从内到外查找变量
            for (int i = scopeStack.size() - 1; i >= 0; i--) {
                Map<String, Variable> scope = scopeStack.get(i);
                if (scope.containsKey(name)) {
                    return scope.get(name);
                }
            }

            logger.error("未定义的变量: " + name);
            throw new RuntimeException("Undefined variable: " + name);
        }

        public boolean hasVariable(String name) {
            for (int i = scopeStack.size() - 1; i >= 0; i--) {
                if (scopeStack.get(i).containsKey(name)) {
                    return true;
                }
            }
            return false;
        }

        public void deleteVariable(String name) {
            // 从内到外删除变量
            for (int i = scopeStack.size() - 1; i >= 0; i--) {
                Map<String, Variable> scope = scopeStack.get(i);
                if (scope.containsKey(name)) {
                    scope.remove(name);
                    variables.remove(name);
                    return;
                }
            }
        }

        public Class<?> getVariableType(String name) {
            if (!hasVariable(name)) {
                logger.error("未定义的变量: " + name);
                throw new RuntimeException("Undefined variable: " + name);
            }
            return Objects.requireNonNull(variables.get(name)).type;
        }

        public void addBuiltIn(String name, BuiltInFunction function) {
            builtIns.put(name, function);
        }

        public boolean hasBuiltIn(String name) {
            return builtIns.containsKey(name);
        }

        public BuiltInFunction getBuiltIn(String name) {
            return builtIns.get(name);
        }

        public Object callBuiltIn(String name, List<Object> args) {
            return Objects.requireNonNull(builtIns.get(name)).call(args);
        }

        public void enterScope() {
            Map<String, Variable> newScope;

            if (scopeStack.isEmpty()) {
                // 第一个作用域（全局）
                newScope = new ConcurrentHashMap<>();
            } else {
                // 创建新作用域，继承父作用域的变量（浅拷贝）
                Map<String, Variable> parentScope = scopeStack.get(scopeStack.size() - 1);
                newScope = new ConcurrentHashMap<>(parentScope);
            }

            scopeStack.add(newScope);
            // 更新当前变量映射到最新作用域
            updateCurrentVariables();
        }


        public void exitScope() {
            if (scopeStack.size() <= 1) {
                // 不能退出全局作用域
                logger.warn("尝试退出全局作用域，忽略");
                return;
            }

            scopeStack.remove(scopeStack.size() - 1);
            // 更新当前变量映射
            updateCurrentVariables();
        }

        private void updateCurrentVariables() {
            if (!scopeStack.isEmpty()) {
                // 清空当前variables，从栈顶作用域重新填充
                variables.clear();
                variables.putAll(scopeStack.get(scopeStack.size() - 1));
            }
        }

        public String getCurrentMethodReturnType() {
            return currentMethodReturnType;
        }

        public void setCurrentMethodReturnType(String returnType) {
            this.currentMethodReturnType = returnType;
        }

        public boolean hasClass(String name) {
            try {
                findClassInternal(name);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        public <T> T castObject(Object object, Class<T> clazz) {
            if (object == null) {
                // 对于基本类型，null 不能转换为基本类型，返回默认值
                return getDefaultValue(clazz);
            }

            // 如果目标类型是 String，转换为字符串
            if (clazz.equals(String.class)) {
                return clazz.cast(object.toString());
            }

            // 如果目标类型是基本类型，处理转换
            if (clazz.isPrimitive()) {
                return castToPrimitive(object, clazz);
            }

            // 如果已经是目标类型，直接转换
            if (clazz.isInstance(object)) {
                return clazz.cast(object);
            }

            // 尝试包装类型到基本类型的转换
            if (isWrapperType(object.getClass())) {
                clazz.isPrimitive();
            }

            // 尝试字符串到基本类型/包装类型的转换
            if (object instanceof String) {
                return castStringToType((String) object, clazz);
            }

            // 尝试数字类型之间的转换
            if (object instanceof Number && isNumericType(clazz)) {
                return castNumberToType((Number) object, clazz);
            }

            // 默认情况下尝试强制转换
            try {
                return clazz.cast(object);
            } catch (ClassCastException e) {
                throw new ClassCastException("Cannot cast " + object.getClass().getName() +
                        " to " + clazz.getName());
            }
        }

        // 将对象转换为基本类型
        private <T> T castToPrimitive(Object object, Class<T> primitiveType) {
            if (object == null) {
                return getDefaultValue(primitiveType);
            }

            // 如果对象已经是基本类型的包装类，拆箱
            if (isWrapperType(object.getClass())) {
                return castWrapperToPrimitive(object, primitiveType);
            }

            // 如果是字符串，尝试解析
            if (object instanceof String) {
                return castStringToPrimitive((String) object, primitiveType);
            }

            // 如果是数字，进行转换
            if (object instanceof Number) {
                return castNumberToPrimitive((Number) object, primitiveType);
            }

            throw new ClassCastException("Cannot convert " + object.getClass().getName() +
                    " to primitive " + primitiveType.getName());
        }

        // 将包装类型转换为基本类型
        @SuppressWarnings("unchecked")
        private <T> T castWrapperToPrimitive(Object wrapper, Class<T> primitiveType) {
            if (wrapper == null) {
                return getDefaultValue(primitiveType);
            }

            if (primitiveType == int.class) {
                if (wrapper instanceof Integer)
                    return (T) (Integer) wrapper;
                if (wrapper instanceof Byte)
                    return (T) Integer.valueOf(((Byte) wrapper).intValue());
                if (wrapper instanceof Short)
                    return (T) Integer.valueOf(((Short) wrapper).intValue());
                if (wrapper instanceof Long)
                    return (T) Integer.valueOf(((Long) wrapper).intValue());
                if (wrapper instanceof Float)
                    return (T) Integer.valueOf(((Float) wrapper).intValue());
                if (wrapper instanceof Double)
                    return (T) Integer.valueOf(((Double) wrapper).intValue());
            } else if (primitiveType == long.class) {
                if (wrapper instanceof Number)
                    return (T) Long.valueOf(((Number) wrapper).longValue());
            } else if (primitiveType == double.class) {
                if (wrapper instanceof Number)
                    return (T) Double.valueOf(((Number) wrapper).doubleValue());
            } else if (primitiveType == float.class) {
                if (wrapper instanceof Number)
                    return (T) Float.valueOf(((Number) wrapper).floatValue());
            } else if (primitiveType == boolean.class) {
                if (wrapper instanceof Boolean)
                    return (T) wrapper;
            } else if (primitiveType == byte.class) {
                if (wrapper instanceof Number)
                    return (T) Byte.valueOf(((Number) wrapper).byteValue());
            } else if (primitiveType == short.class) {
                if (wrapper instanceof Number)
                    return (T) Short.valueOf(((Number) wrapper).shortValue());
            } else if (primitiveType == char.class) {
                if (wrapper instanceof Character)
                    return (T) wrapper;
            }

            throw new ClassCastException("Cannot convert " + wrapper.getClass().getName() +
                    " to primitive " + primitiveType.getName());
        }

        // 将字符串转换为基本类型
        @SuppressWarnings("unchecked")
        private <T> T castStringToPrimitive(String str, Class<T> primitiveType) {
            try {
                if (primitiveType == int.class) {
                    return (T) Integer.valueOf(Integer.parseInt(str));
                } else if (primitiveType == long.class) {
                    return (T) Long.valueOf(Long.parseLong(str));
                } else if (primitiveType == double.class) {
                    return (T) Double.valueOf(Double.parseDouble(str));
                } else if (primitiveType == float.class) {
                    return (T) Float.valueOf(Float.parseFloat(str));
                } else if (primitiveType == boolean.class) {
                    return (T) Boolean.valueOf(Boolean.parseBoolean(str));
                } else if (primitiveType == byte.class) {
                    return (T) Byte.valueOf(Byte.parseByte(str));
                } else if (primitiveType == short.class) {
                    return (T) Short.valueOf(Short.parseShort(str));
                } else if (primitiveType == char.class) {
                    if (str.length() == 1) {
                        return (T) Character.valueOf(str.charAt(0));
                    }
                    throw new NumberFormatException("String must be exactly one character for char conversion");
                }
            } catch (NumberFormatException e) {
                throw new ClassCastException("Cannot convert string \"" + str +
                        "\" to " + primitiveType.getName() + ": " + e.getMessage());
            }

            throw new ClassCastException("Cannot convert string to " + primitiveType.getName());
        }

        // 将字符串转换为指定类型（包括包装类型）
        @SuppressWarnings("unchecked")
        private <T> T castStringToType(String str, Class<T> targetType) {
            if (targetType.equals(String.class)) {
                return (T) str;
            }

            // 如果是基本类型，使用字符串到基本类型的转换
            if (targetType.isPrimitive()) {
                return castStringToPrimitive(str, targetType);
            }

            // 如果是包装类型，转换为包装类型
            try {
                if (targetType.equals(Integer.class)) {
                    return (T) Integer.valueOf(Integer.parseInt(str));
                } else if (targetType.equals(Long.class)) {
                    return (T) Long.valueOf(Long.parseLong(str));
                } else if (targetType.equals(Double.class)) {
                    return (T) Double.valueOf(Double.parseDouble(str));
                } else if (targetType.equals(Float.class)) {
                    return (T) Float.valueOf(Float.parseFloat(str));
                } else if (targetType.equals(Boolean.class)) {
                    return (T) Boolean.valueOf(Boolean.parseBoolean(str));
                } else if (targetType.equals(Byte.class)) {
                    return (T) Byte.valueOf(Byte.parseByte(str));
                } else if (targetType.equals(Short.class)) {
                    return (T) Short.valueOf(Short.parseShort(str));
                } else if (targetType.equals(Character.class)) {
                    if (str.length() == 1) {
                        return (T) Character.valueOf(str.charAt(0));
                    }
                    throw new NumberFormatException("String must be exactly one character for char conversion");
                }
            } catch (NumberFormatException e) {
                throw new ClassCastException("Cannot convert string \"" + str +
                        "\" to " + targetType.getName() + ": " + e.getMessage());
            }

            throw new ClassCastException("Cannot convert string to " + targetType.getName());
        }

        // 将数字转换为基本类型
        @SuppressWarnings("unchecked")
        private <T> T castNumberToPrimitive(Number number, Class<T> primitiveType) {
            if (primitiveType == int.class) {
                return (T) Integer.valueOf(number.intValue());
            } else if (primitiveType == long.class) {
                return (T) Long.valueOf(number.longValue());
            } else if (primitiveType == double.class) {
                return (T) Double.valueOf(number.doubleValue());
            } else if (primitiveType == float.class) {
                return (T) Float.valueOf(number.floatValue());
            } else if (primitiveType == byte.class) {
                return (T) Byte.valueOf(number.byteValue());
            } else if (primitiveType == short.class) {
                return (T) Short.valueOf(number.shortValue());
            }

            throw new ClassCastException("Cannot convert Number to " + primitiveType.getName());
        }

        // 将数字转换为指定类型
        @SuppressWarnings("unchecked")
        private <T> T castNumberToType(Number number, Class<T> targetType) {
            if (targetType.isPrimitive()) {
                return castNumberToPrimitive(number, targetType);
            }

            // 包装类型
            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return (T) Integer.valueOf(number.intValue());
            } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return (T) Long.valueOf(number.longValue());
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return (T) Double.valueOf(number.doubleValue());
            } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return (T) Float.valueOf(number.floatValue());
            } else if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
                return (T) Byte.valueOf(number.byteValue());
            } else if (targetType.equals(Short.class) || targetType.equals(short.class)) {
                return (T) Short.valueOf(number.shortValue());
            }

            throw new ClassCastException("Cannot convert Number to " + targetType.getName());
        }

        // 判断是否是包装类型
        private boolean isWrapperType(Class<?> clazz) {
            return clazz == Integer.class || clazz == Long.class ||
                    clazz == Double.class || clazz == Float.class ||
                    clazz == Boolean.class || clazz == Byte.class ||
                    clazz == Short.class || clazz == Character.class;
        }

        // 判断是否是数值类型
        private boolean isNumericType(Class<?> clazz) {
            if (clazz.isPrimitive()) {
                return clazz == int.class || clazz == long.class ||
                        clazz == double.class || clazz == float.class ||
                        clazz == byte.class || clazz == short.class;
            } else {
                return clazz == Integer.class || clazz == Long.class ||
                        clazz == Double.class || clazz == Float.class ||
                        clazz == Byte.class || clazz == Short.class;
            }
        }

        // 获取基本类型的默认值
        @SuppressWarnings("unchecked")
        private <T> T getDefaultValue(Class<T> clazz) {
            if (clazz == null) {
                return null;
            }

            if (clazz == int.class)
                return (T) Integer.valueOf(0);
            if (clazz == long.class)
                return (T) Long.valueOf(0L);
            if (clazz == double.class)
                return (T) Double.valueOf(0.0);
            if (clazz == float.class)
                return (T) Float.valueOf(0.0f);
            if (clazz == boolean.class)
                return (T) Boolean.valueOf(false);
            if (clazz == byte.class)
                return (T) Byte.valueOf((byte) 0);
            if (clazz == short.class)
                return (T) Short.valueOf((short) 0);
            if (clazz == char.class)
                return (T) Character.valueOf('\0');

            // 对于非基本类型，返回null
            return null;
        }

        public Map<String, Variable> getAllVariables() {
            return new HashMap<>(variables);
        }

        public void clearVariables() {
            variables.clear();
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public void setBuiltInOutputBuffer(IOutputHandler stream) {
            this.outputBuffer = stream;
        }

        public void setBuiltInErrorBuffer(IOutputHandler stream) {
            this.warnMsgBuffer = stream;
        }

        private void setupDefaultImports() {
            logger.debug("设置默认导入");
            try {
                addImport("java.util.*", false);
                addImport("java.lang.reflect.*", false);
                addImport("java.lang.*", false);
                addImport("android.util.*", false);
                addImport("android.os.*", false);
                addImport("java.util.function.*", false);
                addImport("com.justnothing.testmodule.command.functions.script.TestInterpreter$Lambda", false);
                logger.debug("默认导入设置完成");
            } catch (ClassNotFoundException e) {
                logger.error("设置默认导入失败", e);
            }
        }

        public void addImport(String importStatement) throws ClassNotFoundException {
            logger.debug("添加导入: " + importStatement);
            addImport(importStatement, true);
        }

        public void addImport(String importStatement, boolean validate) throws ClassNotFoundException {
            if (validate) {
                validateImport(importStatement);
            }
            imports.add(importStatement);
            logger.debug("导入已添加: " + importStatement);
        }

        private void validateImport(String importStatement) throws ClassNotFoundException {
            logger.debug("验证导入: " + importStatement);
            if (importStatement.endsWith(".*")) {
                String packageName = importStatement.substring(0, importStatement.length() - 2);
                validatePackage(packageName);
            } else {
                validateClass(importStatement);
            }
        }

        private void validatePackage(String packageName) {
            logger.debug("验证包: " + packageName);
        }

        private void validateClass(String className) throws ClassNotFoundException {
            logger.debug("验证类: " + className);
            findClass(className);
        }

        public Class<?> findClass(String className) throws ClassNotFoundException {
            try {
                return resolveClassAndCacheInternal(className);
            } catch (ClassNotFoundException e) {
                logger.error("找不到类: " + className, e);
                throw e;
            }
        }

        private Class<?> resolveClassAndCacheInternal(String className) throws ClassNotFoundException {
            if (classCache.containsKey(className)) {
                return classCache.get(className);
            }

            logger.debug("解析类名: " + className);
            className = getArrayTypeNameWithoutLength(className);
            String classNameCopy = className;
            int count = 0;
            while (className.endsWith("[]")) {
                className = className.substring(0, className.length() - 2);
                count++;
            }
            logger.debug("数组维度: " + count + ", 基础类名: " + className);

            Class<?> clazz;
            clazz = findClassInternal(className);

            for (int i = 0; i < count; i++)
                clazz = Array.newInstance(clazz, 0).getClass();
            classCache.put(classNameCopy, clazz);
            logger.debug("类已缓存: " + classNameCopy + " -> " + clazz.getName());
            return clazz;

        }

        public Class<?> findClassInternal(String className) throws ClassNotFoundException {
            // TODO: 支持泛型
            if (className.contains("<")) {
                logger.warn("目前不支持泛型类，将会擦除类型信息");
                className = className.substring(0, className.indexOf("<"));
                logger.warn("经过擦除过的类型名称为" + className);
            }

            switch (className) {
                case "int":
                    logger.debug("基本类型: int");
                    return int.class;
                case "long":
                    logger.debug("基本类型: long");
                    return long.class;
                case "float":
                    logger.debug("基本类型: float");
                    return float.class;
                case "double":
                    logger.debug("基本类型: double");
                    return double.class;
                case "boolean":
                    logger.debug("基本类型: boolean");
                    return boolean.class;
                case "char":
                    logger.debug("基本类型: char");
                    return char.class;
                case "byte":
                    logger.debug("基本类型: byte");
                    return byte.class;
                case "short":
                    logger.debug("基本类型: short");
                    return short.class;
                case "void":
                    logger.debug("基本类型: void");
                    return void.class;
            }

            if (customClasses.containsKey(className)) {
                logger.debug("找到自定义类: " + className);
                throw new CustomClassException(className);
            }

            Class<?> clazz;
            if (className.contains(".")) {
                // 尝试原始类名
                clazz = findClassThroughApi(className, classLoader);

                if (clazz != null) {
                    logger.debug("通过完整类名找到类: " + clazz.getName());
                    return clazz;
                }

                // 如果失败，尝试把"."替换为"$"
                // 从后往前逐步替换，找到第一个能成功加载的分割点
                String[] parts = className.split("\\.");
                if (parts.length >= 2) { // 至少有两个部分才有可能是嵌套类
                    // 一个一个替换
                    for (int i = parts.length - 1; i >= 1; i--) {
                        StringBuilder nestedClassName = new StringBuilder(parts[0]);
                        for (int j = 1; j < parts.length; j++) {
                            if (j < i) {
                                nestedClassName.append('.').append(parts[j]);
                            } else {
                                nestedClassName.append('$').append(parts[j]);
                            }
                        }
                        clazz = findClassThroughApi(nestedClassName.toString(), classLoader);

                        if (clazz != null) {
                            logger.debug("通过嵌套类名找到类: " + clazz.getName());
                            return clazz;
                        }
                    }
                }
            }

            for (String importStmt : imports) {
                String fullClassName;
                if (importStmt.endsWith(".*")) {
                    String packageName = importStmt.substring(0, importStmt.length() - 2);
                    fullClassName = packageName + "." + className;
                } else {
                    fullClassName = importStmt;
                    String lastName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
                    if (!lastName.equals(className)) {
                        continue;
                    }
                }
                clazz = findClassThroughApi(fullClassName, classLoader);

                if (clazz != null) {
                    logger.debug("通过完整类名找到类: " + clazz.getName());
                    return clazz;
                }

                String[] parts = fullClassName.split("\\.");
                if (parts.length >= 2) {
                    for (int i = parts.length - 1; i >= 1; i--) {
                        StringBuilder nestedClassName = new StringBuilder(parts[0]);
                        for (int j = 1; j < parts.length; j++) {
                            if (j < i) {
                                nestedClassName.append('.').append(parts[j]);
                            } else {
                                nestedClassName.append('$').append(parts[j]);
                            }
                        }
                        clazz = findClassThroughApi(nestedClassName.toString(), classLoader);

                        if (clazz != null) {
                            logger.debug("通过嵌套类名找到类: " + clazz.getName());
                            return clazz;
                        }
                    }
                }
            }
            throw new ClassNotFoundException("Class not found: " + className);
        }

        private void setupBuiltInFunctions() {
            addBuiltIn("enableLog", args -> {
                if (!isStandaloneMode())
                    return null;
                if (args.isEmpty()) {
                    logger.error("printf() 至少需要一个参数");
                    throw new RuntimeException("printf() requires at least one argument");
                }
                StandaloneLogger.enabled = Boolean.TRUE.equals(args.get(0));
                return null;
            });

            addBuiltIn("println", args -> {
                StringBuilder sb = new StringBuilder();
                for (Object arg : args) {
                    sb.append(arg);
                }
                String output = sb.toString();
                println(output);
                return null;
            });

            addBuiltIn("print", args -> {
                StringBuilder sb = new StringBuilder();
                for (Object arg : args) {
                    sb.append(arg);
                }
                String output = sb.toString();
                print(output);
                return null;
            });

            addBuiltIn("printf", args -> {
                if (args.isEmpty()) {
                    logger.error("printf() 至少需要一个参数");
                    throw new RuntimeException("printf() requires at least one argument");
                }
                String format = (String) args.get(0);
                Object[] params = args.subList(1, args.size()).toArray();
                printf(format, params);
                return null;
            });

            addBuiltIn("range", args -> {
                if (args.size() == 1) {
                    int end = ((Number) args.get(0)).intValue();
                    return createRange(0, end, 1);
                } else if (args.size() == 2) {
                    int start = ((Number) args.get(0)).intValue();
                    int end = ((Number) args.get(1)).intValue();
                    return createRange(start, end, 1);
                } else if (args.size() == 3) {
                    int start = ((Number) args.get(0)).intValue();
                    int end = ((Number) args.get(1)).intValue();
                    int step = ((Number) args.get(2)).intValue();
                    return createRange(start, end, step);
                }
                logger.error("range需要1-3个参数，实际上提供了" + args.size() + "个");
                throw new RuntimeException("range() takes 1-3 arguments");
            });

            addBuiltIn("getInterpreterClassLoader", args -> classLoader);

            addBuiltIn("analyze", args -> {
                if (args.size() != 1) {
                    logger.error("analyze requires exactly 1 argument, but got " + args.size());
                    throw new RuntimeException("analyze() requires exactly 1 argument");
                }

                Object target = args.get(0);
                StringBuilder result = new StringBuilder();

                if (target == null) {
                    result.append("Target object is null\n");
                    println(result.toString());
                    return null;
                }

                Class<?> clazz = target.getClass();
                result.append("=== Object Analysis ===\n");
                result.append("String: ").append(target).append("\n");
                result.append("Class Name: ").append(clazz.getName()).append("\n");
                result.append("Simple Name: ").append(clazz.getSimpleName()).append("\n");
                result.append("Package: ").append(clazz.getPackage() != null ? clazz.getPackage().getName() : "None")
                        .append("\n");
                result.append("Is Array: ").append(clazz.isArray()).append("\n");
                result.append("Is Interface: ").append(clazz.isInterface()).append("\n");
                result.append("Is Annotation: ").append(clazz.isAnnotation()).append("\n");
                result.append("Is Enum: ").append(clazz.isEnum()).append("\n");
                result.append("Is Primitive: ").append(clazz.isPrimitive()).append("\n\n");

                result.append("=== Fields ===\n");
                Field[] fields = clazz.getDeclaredFields();
                if (fields.length == 0) {
                    result.append("No fields\n");
                } else {
                    for (Field field : fields) {
                        result.append("  ").append(field.toString()).append("\n");
                    }
                }
                if (fields.length > 0)
                    result.append("Total Fields: ")
                            .append(fields.length).append("\n\n");

                result.append("=== Methods ===\n");
                Method[] methods = clazz.getDeclaredMethods();
                if (methods.length == 0) {
                    result.append("No methods\n");
                } else {
                    for (Method method : methods) {
                        result.append("  ").append(method.toString()).append("\n");
                    }
                }
                if (methods.length > 0)
                    result.append("Total Methods: ")
                            .append(methods.length).append("\n\n");

                result.append("=== Superclass ===\n");
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != null) {
                    result.append(superClass.getName());
                } else {
                    result.append("No superclass");
                }
                result.append("\n\n");

                result.append("=== Implemented Interfaces ===\n");
                Class<?>[] interfaces = clazz.getInterfaces();
                if (interfaces.length == 0) {
                    result.append("No interfaces\n");
                } else {
                    for (Class<?> _interface : interfaces) {
                        result.append("  ").append(_interface.getName()).append("\n");
                    }
                }
                if (interfaces.length > 0)
                    result.append("Total Interfaces: ")
                            .append(interfaces.length).append("\n");
                println(result.toString());
                return null;
            });

            addBuiltIn("getContext", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getContext() 不接受任何参数，忽略参数");
                }

                try {
                    // 方法1: 通过 ActivityThread 获取 Application Context
                    Class<?> activityThreadClass = findClass("android.app.ActivityThread");
                    Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
                    Object activityThread = currentActivityThreadMethod.invoke(null);

                    Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
                    return getApplicationMethod.invoke(activityThread);

                } catch (Exception e1) {
                    logger.warn("方法1失败: " + e1.getMessage());

                    try {
                        // 方法2: 尝试获取系统 Context
                        Class<?> contextImplClass = findClass("android.app.ContextImpl");
                        Method getSystemContextMethod = contextImplClass.getMethod("getSystemContext");
                        return getSystemContextMethod.invoke(null);

                    } catch (Exception e2) {
                        logger.warn("方法2失败: " + e2.getMessage());

                        try {
                            // 方法3: 尝试通过 ServiceManager 获取
                            Class<?> serviceManagerClass = findClass("android.os.ServiceManager");
                            Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);

                            // 尝试获取 activity 服务
                            Object activityService = getServiceMethod.invoke(null, "activity");
                            if (activityService != null) {
                                findClass("android.app.IActivityManager");
                            }

                            // 尝试获取 window 服务
                            Object windowService = getServiceMethod.invoke(null, "window");
                            if (windowService != null) {
                                // 这可能是一个 Context
                                return windowService;
                            }

                        } catch (Exception e3) {
                            logger.warn("方法3失败: " + e3.getMessage());
                        }

                        // 所有方法都失败了
                        logger.error("无法获取 Context: " + e1.getMessage() + ", " + e2.getMessage());
                        throw new RuntimeException("Failed to get Context. Are you running in an Android environment?");
                    }
                }
            });

            addBuiltIn("getApplicationInfo", args -> {
                try {
                    Object context = getBuiltIn("getContext").call(Collections.emptyList());
                    Method getApplicationInfoMethod = context.getClass().getMethod("getApplicationInfo");
                    return getApplicationInfoMethod.invoke(context);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to get ApplicationInfo: " + e.getMessage());
                }
            });

            addBuiltIn("getPackageName", args -> {
                try {
                    Object context = getBuiltIn("getContext").call(Collections.emptyList());
                    Method getPackageNameMethod = context.getClass().getMethod("getPackageName");
                    return getPackageNameMethod.invoke(context);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to get package name: " + e.getMessage());
                }
            });

            addBuiltIn("createSafeExecutor", args -> new Object() {
                // 创建一个有 Looper 的线程
                private Object createLooperThread() throws Exception {
                    Class<?> handlerThreadClass = findClass("android.os.HandlerThread");
                    Constructor<?> constructor = handlerThreadClass.getConstructor(String.class);
                    Object handlerThread = constructor.newInstance("SafeExecutor-Thread");
                    handlerThreadClass.getMethod("start").invoke(handlerThread);
                    return handlerThread;
                }

                // 获取线程的 Looper
                private Object getThreadLooper(Object handlerThread) throws Exception {
                    return handlerThread.getClass().getMethod("getLooper").invoke(handlerThread);
                }

                // 创建一个 Handler
                private Object createHandler(Object looper) throws Exception {
                    Class<?> handlerClass = findClass("android.os.Handler");
                    Constructor<?> constructor = handlerClass
                            .getConstructor(findClassThroughApi("android.os.Looper", classLoader));
                    return constructor.newInstance(looper);
                }

                // 在主线程执行任务
                public Object runOnMainThread(Callable<Object> task) throws Exception {
                    Class<?> looperClass = findClass("android.os.Looper");
                    Class<?> handlerClass = findClass("android.os.Handler");
                    Class<?> runnableClass = findClass("java.lang.Runnable");

                    // 获取主线程 Looper
                    Method getMainLooperMethod = looperClass.getMethod("getMainLooper");
                    Object mainLooper = getMainLooperMethod.invoke(null);

                    // 创建主线程 Handler
                    Constructor<?> handlerConstructor = handlerClass.getConstructor(looperClass);
                    Object mainHandler = handlerConstructor.newInstance(mainLooper);

                    // 使用 CountDownLatch 等待
                    CountDownLatch latch = new CountDownLatch(1);
                    final Object[] result = new Object[1];
                    final Exception[] exception = new Exception[1];

                    // 创建 Runnable
                    Object runnable = Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class[] { runnableClass },
                            (proxy, method, params) -> {
                                if (method.getName().equals("run")) {
                                    try {
                                        result[0] = task.call();
                                    } catch (Exception e) {
                                        exception[0] = e;
                                    } finally {
                                        latch.countDown();
                                    }
                                }
                                return null;
                            });

                    // 在主线程执行
                    Method postMethod = handlerClass.getMethod("post", runnableClass);
                    postMethod.invoke(mainHandler, runnable);

                    // 等待完成
                    latch.await();

                    if (exception[0] != null) {
                        throw exception[0];
                    }

                    return result[0];
                }

                // 在有 Looper 的线程中执行任务（新建线程）
                public Object runOnLooperThread(Callable<Object> task) throws Exception {
                    Object handlerThread = createLooperThread();
                    try {
                        Object looper = getThreadLooper(handlerThread);
                        Object handler = createHandler(looper);

                        CountDownLatch latch = new CountDownLatch(1);
                        final Object[] result = new Object[1];
                        final Exception[] exception = new Exception[1];

                        Class<?> runnableClass = findClass("java.lang.Runnable");
                        Object runnable = Proxy.newProxyInstance(
                                getClass().getClassLoader(),
                                new Class[] { runnableClass },
                                (proxy, method, params) -> {
                                    if (method.getName().equals("run")) {
                                        try {
                                            result[0] = task.call();
                                        } catch (Exception e) {
                                            exception[0] = e;
                                        } finally {
                                            latch.countDown();
                                        }
                                    }
                                    return null;
                                });

                        Method postMethod = handler.getClass().getMethod("post", runnableClass);
                        postMethod.invoke(handler, runnable);

                        latch.await();

                        if (exception[0] != null) {
                            throw exception[0];
                        }

                        return result[0];
                    } finally {
                        // 退出线程
                        try {
                            handlerThread.getClass().getMethod("quit").invoke(handlerThread);
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                }

                // 在当前线程准备 Looper（如果还没有的话）
                public Object runWithLooper(Callable<Object> task) throws Exception {
                    Class<?> looperClass = findClass("android.os.Looper");
                    Method myLooperMethod = looperClass.getMethod("myLooper");
                    Object currentLooper = myLooperMethod.invoke(null);

                    boolean needPrepare = currentLooper == null;
                    boolean needLoop = false;

                    if (needPrepare) {
                        Method prepareMethod = looperClass.getMethod("prepare");
                        prepareMethod.invoke(null);
                        needLoop = true;
                    }

                    try {
                        // 执行任务
                        Object result = task.call();

                        // 如果启动了 Looper，需要处理消息队列
                        if (needLoop) {
                            // 创建一个 Handler 来处理消息（防止阻塞）
                            Object handler = createHandler(myLooperMethod.invoke(null));

                            // 发送一个退出消息
                            Class<?> handlerClass = findClass("android.os.Handler");
                            Method postDelayedMethod = handlerClass.getMethod("postDelayed",
                                    findClassThroughApi("java.lang.Runnable", classLoader), long.class);

                            Class<?> runnableClass = findClass("java.lang.Runnable");
                            Object quitRunnable = Proxy.newProxyInstance(
                                    getClass().getClassLoader(),
                                    new Class[] { runnableClass },
                                    (proxy, method, params) -> {
                                        if (method.getName().equals("run")) {
                                            try {
                                                Method quitMethod = looperClass.getMethod("quit");
                                                quitMethod.invoke(currentLooper);
                                            } catch (Exception e) {
                                                // 忽略
                                            }
                                        }
                                        return null;
                                    });

                            postDelayedMethod.invoke(handler, quitRunnable, 100L);

                            // 开始处理消息循环
                            Method loopMethod = looperClass.getMethod("loop");
                            loopMethod.invoke(null);
                        }

                        return result;
                    } catch (Exception e) {
                        if (needLoop) {
                            // 退出 Looper
                            try {
                                Method quitMethod = looperClass.getMethod("quit");
                                quitMethod.invoke(currentLooper);
                            } catch (Exception e2) {
                                // 忽略
                            }
                        }
                        throw e;
                    }
                }

                // 直接创建需要 Handler 的类
                public Object createInstanceWithHandler(String className, Object... args) throws Exception {
                    return runWithLooper(() -> {
                        Class<?> clazz = findClass(className);

                        // 查找匹配的构造函数
                        for (Constructor<?> constructor : clazz.getConstructors()) {
                            if (constructor.getParameterTypes().length == args.length) {
                                boolean match = true;
                                for (int i = 0; i < args.length; i++) {
                                    if (args[i] != null &&
                                            !constructor.getParameterTypes()[i]
                                                    .isAssignableFrom(args[i].getClass())) {
                                        match = false;
                                        break;
                                    }
                                }
                                if (match) {
                                    return constructor.newInstance(args);
                                }
                            }
                        }

                        throw new RuntimeException("No matching constructor found");
                    });
                }
            });

            // 添加函数接口支持
            addBuiltIn("asRunnable", args -> {
                if (args.size() != 1) {
                    throw new RuntimeException("asRunnable requires one parameter: lambda expression or function");
                }

                Object func = args.get(0);
                return Proxy.newProxyInstance(
                        this.getClassLoader(),
                        new Class[] { Runnable.class },
                        (proxy, method, params) -> {
                            try {
                                // 调用函数
                                return this.callMethod(func, "run", Collections.emptyList());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            });

            addBuiltIn("asFunction", args -> {
                if (args.size() != 1) {
                    throw new RuntimeException("asFunction requires one parameter: lambda expression or function");
                }

                Object func = args.get(0);
                return Proxy.newProxyInstance(
                        this.getClassLoader(),
                        new Class[] { Function.class },
                        (proxy, method, params) -> {
                            try {
                                // 调用函数
                                return this.callMethod(func, "apply",
                                        params != null ? Arrays.asList(params) : Collections.emptyList());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            });

            addBuiltIn("runLater", args -> {
                if (args.size() != 1) {
                    throw new RuntimeException("runLater requires one parameter: Runnable");
                }

                Object runnable = args.get(0);
                try {
                    // 创建一个新线程来运行
                    Thread thread = new Thread(() -> {
                        try {
                            this.callMethod(runnable, "run", Collections.emptyList());
                        } catch (Exception e) {
                            logger.error("运行Runnable时出错: " + e);
                        }
                    }, "runLater");
                    thread.start();
                    return thread;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to run Runnable: " + e.getMessage());
                }
            });

        }

        private List<Integer> createRange(int start, int end, int step) {
            List<Integer> list = new ArrayList<>();
            if (step > 0) {
                for (int i = start; i < end; i += step) {
                    list.add(i);
                }
            } else if (step < 0) {
                for (int i = start; i > end; i += step) {
                    list.add(i);
                }
            }
            return list;
        }

        public Object callMethod(Object object, String methodName, List<Object> args) throws Exception {
            if (methodName == null)
                methodName = "call";

            if (object == null) {
                throw new NullPointerException(
                        "Attempt to invoke method " + methodName + " on a null object reference");
            }

            Class<?> clazz = object.getClass();
            List<Class<?>> argTypes = args.stream()
                    .map(arg -> arg != null ? arg.getClass() : Void.class)
                    .collect(Collectors.toList());

            Method method = findMethod(clazz, methodName, argTypes, object);
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

        public Object callStaticMethod(String className, String methodName, List<Object> args) throws Exception {
            Class<?> clazz = findClass(className);
            if (clazz == null) {
                throw new ClassNotFoundException("Class not found: " + className);
            }

            List<Class<?>> argTypes = args.stream()
                    .map(arg -> arg != null ? arg.getClass() : Void.class)
                    .collect(Collectors.toList());

            // 查找方法（支持可变参数）
            Method method = findMethod(clazz, methodName, argTypes, null);

            // 准备调用参数（处理可变参数）
            Object[] invokeArgs = prepareInvokeArguments(method, args);

            try {
                return method.invoke(null, invokeArgs);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                } else if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else if (cause instanceof Error) {
                    throw (Error) cause;
                } else {
                    throw e;
                }
            }
        }

        // 修改 prepareInvokeArguments 方法中的可变参数处理部分
private Object[] prepareInvokeArguments(Method method, List<Object> args) {
    Class<?>[] paramTypes = method.getParameterTypes();

    if (!method.isVarArgs() || paramTypes.length == 0) {
        return args.toArray();
    }

    int fixedParamCount = paramTypes.length - 1;
    Class<?> varArgsType = paramTypes[fixedParamCount];
    Class<?> varArgsComponentType = varArgsType.getComponentType();

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
                varArgsComponentType.isAssignableFrom(lastArg.getClass().getComponentType())) {
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


private Class<?> inferVarArgElementType(Type genericType, List<Object> args, int fixedParamCount) {
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


private boolean shouldExpandArrayForVarArgs(Method method, Object arrayArg, Class<?> inferredElementType) {
    if (arrayArg == null || !arrayArg.getClass().isArray()) {
        return false;
    }
    
    Class<?> varArgsType = method.getParameterTypes()[method.getParameterTypes().length - 1];
    Class<?> varArgsComponentType = varArgsType.getComponentType();
    Class<?> arrayComponentType = arrayArg.getClass().getComponentType();
    
    // 对于像 Arrays.asList(T... a) 这样的泛型方法
    // 如果传入数组，且数组的元素类型可以赋值给可变参数的元素类型，则展开
    if (inferredElementType != null && 
        inferredElementType.isAssignableFrom(arrayComponentType)) {
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

        public Method findMethod(Class<?> clazz, String name, List<Class<?>> argTypes) throws Exception {
            return findMethod(clazz, name, argTypes, null);
        }

        private String getMethodCacheKey(Class<?> clazz, String name, List<Class<?>> argTypes) {
            StringBuilder key = new StringBuilder();
            key.append(clazz.getName()).append(".").append(name).append("(");
            for (int i = 0; i < argTypes.size(); i++) {
                if (i > 0) {
                    key.append(",");
                }
                key.append(argTypes.get(i).getName());
            }
            key.append(")");
            return key.toString();
        }

        /**
         * 查找方法（支持可变参数、缓存、接口查找和模块安全访问）
         */
        public Method findMethod(Class<?> clazz, String name, List<Class<?>> argTypes, Object targetObj)
                throws Exception {
            String cacheKey = getMethodCacheKey(clazz, name, argTypes);

            // 检查缓存
            if (methodCache.containsKey(cacheKey)) {
                Method cachedMethod = methodCache.get(cacheKey);
                if (cachedMethod != null) {
                    // 检查缓存的方法是否仍然可用
                    if (targetObj == null || canAccessMethodSafely(cachedMethod, targetObj, false)) {
                        logger.debug("从缓存返回方法: " + cacheKey);
                        return cachedMethod;
                    } else {
                        logger.debug("缓存的方法" + cacheKey + "无法访问，重新查找");
                    }
                }
            }

            List<Method> candidates = new ArrayList<>();
            Method exactMatch = null; // 精确匹配的非可变参数方法
            Method varArgsCandidate = null; // 可变参数方法
            Method interfaceCandidate = null; // 通过接口找到的方法

            // 第一步：收集所有公共方法
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(name)) {
                    candidates.add(method);
                }
            }

            // 第二步：如果没有公共方法，尝试查找声明的方法（包括非公共方法）
            if (candidates.isEmpty()) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(name)) {
                        // 尝试设置可访问性，但处理模块限制
                        trySetAccessibleSafely(method);
                        candidates.add(method);
                    }
                }
            }

            // 第三步：在候选方法中查找匹配的方法
            for (Method method : candidates) {
                boolean isVarArgs = method.isVarArgs();
                Class<?>[] paramTypes = method.getParameterTypes();

                if (isApplicableArgs(paramTypes, argTypes, isVarArgs)) {
                    // 检查访问权限
                    if (targetObj != null && !canAccessMethodSafely(method, targetObj, true)) {
                        logger.debug("方法 " + method + " 当前无法访问，尝试通过接口查找");
                        continue; // 跳过无法访问的方法
                    }

                    // 优先选择非可变参数方法（如果参数完全匹配）
                    if (!isVarArgs && paramTypes.length == argTypes.size()) {
                        exactMatch = method;
                        break; // 找到精确匹配，停止查找
                    }

                    // 记住可变参数方法作为备选
                    if (isVarArgs && varArgsCandidate == null) {
                        varArgsCandidate = method;
                    }
                }
            }

            // 第四步：如果找到了精确匹配的方法，返回它
            if (exactMatch != null) {
                methodCache.put(cacheKey, exactMatch);
                logger.debug(
                        "方法已缓存(精确匹配): " + cacheKey + " -> " + exactMatch.getDeclaringClass().getName() + "." + name);
                return exactMatch;
            }

            // 第五步：如果找到了可变参数方法，返回它
            if (varArgsCandidate != null) {
                methodCache.put(cacheKey, varArgsCandidate);
                logger.debug("方法已缓存(可变参数): " + cacheKey + " -> " + varArgsCandidate.getDeclaringClass().getName() + "."
                        + name);
                return varArgsCandidate;
            }

            // 第六步：如果当前类中没有找到可访问的方法，尝试通过接口查找
            if (candidates.isEmpty() || allCandidatesInaccessible(candidates, targetObj)) {
                logger.warn("类 " + clazz.getName() + " 的方法 " + name + " 无法直接访问，尝试通过接口查找");
                interfaceCandidate = findMethodThroughInterfaces(clazz, name, argTypes);
                if (interfaceCandidate != null) {
                    methodCache.put(cacheKey, interfaceCandidate);
                    logger.debug("方法已缓存(接口): " + cacheKey + " -> " + interfaceCandidate.getDeclaringClass().getName()
                            + "." + name);
                    return interfaceCandidate;
                }
            }

            // 第七步：没有找到匹配的方法，构建详细的错误信息
            logger.debug("找不到类" + clazz.getName() + "的方法" + name + "，参数为" + Arrays.toString(argTypes.toArray()));
            StringBuilder sb = new StringBuilder();
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(name)) {
                    sb.append(Arrays.toString(method.getParameterTypes())).append("\n");
                }
            }

            String sigString = Arrays.toString(argTypes.toArray());
            sigString = sigString.substring(1, sigString.length() - 1);
            throw new NoSuchMethodException("Method not found: " +
                    clazz.getName() + "." + name + "(" + sigString + ")" +
                    (sb.length() > 0 ? "\nAvailable signatures:\n" + sb + "\n" : ""));
        }

        /**
         * 检查所有候选方法是否都无法访问
         */
        private boolean allCandidatesInaccessible(List<Method> candidates, Object targetObj) {
            for (Method method : candidates) {
                if (canAccessMethodSafely(method, targetObj, true)) {
                    return false;
                }
            }
            return true;
        }


        /**
         * 通过接口查找方法（确保返回公共接口中的方法）
         */
        private Method findMethodThroughInterfaces(Class<?> clazz, String methodName, List<Class<?>> argTypes) {
            // 收集所有接口（包括间接实现的）
            Set<Class<?>> allInterfaces = new HashSet<>();
            collectAllInterfaces(clazz, allInterfaces);

            // 在所有接口中查找匹配的public方法
            for (Class<?> _interface : allInterfaces) {
                // 确保接口是public的
                if (!Modifier.isPublic(_interface.getModifiers())) {
                    continue;
                }
                // 在接口中查找方法
                Method method = findMethodInInterface(_interface, methodName, argTypes);
                if (method != null) {
                    logger.debug("通过接口 " + _interface.getName() + " 找到方法: " + methodName);
                    return method;
                }
            }
            return null;
        }

        /**
         * 收集类实现的所有接口（包括父类实现的接口）
         */
        private void collectAllInterfaces(Class<?> clazz, Set<Class<?>> interfaces) {
            if (clazz == null || clazz == Object.class) {
                return;
            }
            // 当前类直接实现的接口
            for (Class<?> _interface : clazz.getInterfaces()) {
                interfaces.add(_interface);
                collectAllInterfaces(_interface, interfaces); // 递归收集父接口
            }
            // 父类实现的接口
            collectAllInterfaces(clazz.getSuperclass(), interfaces);
        }

        /**
         * 在接口中查找匹配的方法
         */
        private Method findMethodInInterface(Class<?> _interface, String methodName, List<Class<?>> argTypes) {
            try {
                // 查找接口中声明的方法（包括继承的方法）
                for (Method method : _interface.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        boolean isVarArgs = method.isVarArgs();
                        Class<?>[] paramTypes = method.getParameterTypes();

                        if (isApplicableArgs(paramTypes, argTypes, isVarArgs)) {
                            // 确保方法来自接口本身（不是Object类的方法）
                            if (method.getDeclaringClass().isInterface()) {
                                return method;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略异常，继续查找
            }
            return null;
        }

        /**
         * 安全的访问权限检查（处理模块限制）
         */
        private boolean canAccessMethodSafely(Method method, Object targetObj, boolean trySetAccessible) {
            try {

                // 对于静态方法，targetObj为null
                if (targetObj == null) {
                    // 静态方法：检查是否为public
                    if (Modifier.isPublic(method.getModifiers())) {
                        return true;
                    }
                    if (trySetAccessible) {
                        return trySetAccessibleSafely(method);
                    }
                    return false;
                }

                // 实例方法：使用Java 9+的canAccess方法（如果可用）
                try {
                    Method canAccessMethod = AccessibleObject.class.getMethod("canAccess", Object.class);
                    return (Boolean) canAccessMethod.invoke(method, targetObj);
                } catch (NoSuchMethodException e) {
                    // Java 8或没有canAccess方法，回退到旧的检查方式
                    if (Modifier.isPublic(method.getModifiers())) {
                        return true;
                    }
                    if (trySetAccessible) {
                        return trySetAccessibleSafely(method);
                    }
                    return false;
                }
            } catch (Exception e) {
                // 任何异常都视为无法访问
                return false;
            }
        }


        /**
         * 安全地尝试设置方法可访问（处理模块系统限制）
         */
        private boolean trySetAccessibleSafely(Method method) {
            if (method == null || method.isAccessible()) {
                return true;
            }
            try {
                method.setAccessible(true);
                return true;
            } catch (SecurityException e) {
                // 安全管理器限制
                logger.debug("安全管理器限制，无法设置方法可访问: " + method);
                return false;
            }
        }
    }

      public static class ClassDefinition {
        private final String className;
        private final Map<String, List<MethodDefinition>> methods;
        private final Map<String, FieldDefinition> fields;
        private final Map<String, ConstructorDefinition> constructors;
        private String superClassName;
        private final List<String> interfaceNames;

        public ClassDefinition(String className) {
            this.className = className;
            this.methods = new HashMap<>();
            this.fields = new HashMap<>();
            this.constructors = new HashMap<>();
            this.superClassName = null;
            this.interfaceNames = new ArrayList<>();
        }

        public String getClassName() {
            return className;
        }

        public void addMethod(String methodName, MethodDefinition method) {
            methods.computeIfAbsent(methodName, k -> new ArrayList<>()).add(method);
        }

        public void addField(String fieldName, FieldDefinition field) {
            fields.put(fieldName, field);
        }

        public void addConstructor(ConstructorDefinition constructor) {
            // 使用参数列表的签名作为键来区分重载的构造函数
            String signature = constructor.getSignature();
            constructors.put(signature, constructor);
        }

        public MethodDefinition getMethod(String methodName) {
            List<MethodDefinition> methodList = methods.get(methodName);
            return methodList != null && !methodList.isEmpty() ? methodList.get(0) : null;
        }

        public List<MethodDefinition> getMethods(String methodName) {
            return methods.get(methodName);
        }

        public FieldDefinition getField(String fieldName) {
            return fields.get(fieldName);
        }

        public List<FieldDefinition> getFields() {
            return new ArrayList<>(fields.values());
        }

        public List<ConstructorDefinition> getConstructors() {
            return new ArrayList<>(constructors.values());
        }

        public ConstructorDefinition getConstructor(List<Class<?>> paramTypes, ExecutionContext context)
                throws ClassNotFoundException {
            for (ConstructorDefinition constructor : constructors.values()) {
                if (constructor.matchesParameters(paramTypes, context)) {
                    return constructor;
                }
            }
            return null;
        }

        public List<String> getMethodNames() {
            return new ArrayList<>(methods.keySet());
        }

        public void setSuperClassName(String superClassName) {
            this.superClassName = superClassName;
        }

        public String getSuperClassName() {
            return superClassName;
        }

        public void addInterfaceName(String interfaceName) {
            this.interfaceNames.add(interfaceName);
        }

        public List<String> getInterfaceNames() {
            return new ArrayList<>(interfaceNames);
        }
    }

    public static class MethodDefinition {
        private final String methodName;
        private final String returnType;
        private final List<Parameter> parameters;
        private final ASTNode body;

        public MethodDefinition(String methodName, List<Parameter> parameters, ASTNode body) {
            this.methodName = methodName;
            this.returnType = "void";
            this.parameters = parameters;
            this.body = body;
        }

        public MethodDefinition(String methodName, String returnType, List<Parameter> parameters, ASTNode body) {
            this.methodName = methodName;
            this.returnType = returnType;
            this.parameters = parameters;
            this.body = body;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getReturnType() {
            return returnType;
        }

        public List<Parameter> getParameters() {
            return parameters;
        }

        public ASTNode getBody() {
            return body;
        }
    }

    public static class ConstructorDefinition {
        private final String className;
        private final List<Parameter> parameters;
        private final ASTNode body;

        public ConstructorDefinition(String className, List<Parameter> parameters, ASTNode body) {
            this.className = className;
            this.parameters = parameters;
            this.body = body;
        }

        public String getClassName() {
            return className;
        }

        public List<Parameter> getParameters() {
            return parameters;
        }

        public ASTNode getBody() {
            return body;
        }

        public String getSignature() {
            StringBuilder sb = new StringBuilder(className);
            sb.append("(");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0)
                    sb.append(",");
                sb.append(parameters.get(i).getType());
            }
            sb.append(")");
            return sb.toString();
        }

        public boolean matchesParameters(List<Class<?>> paramTypes, ExecutionContext context)
                throws ClassNotFoundException {
            if (parameters.size() != paramTypes.size()) {
                return false;
            }
            for (int i = 0; i < parameters.size(); i++) {
                Class<?> expectedType = context.findClass(parameters.get(i).getType());
                Class<?> actualType = paramTypes.get(i);
                if (!expectedType.isAssignableFrom(actualType) &&
                        !isPrimitiveWrapperMatch(expectedType, actualType)) {
                    return false;
                }
            }
            return true;
        }

    }

    public static class Parameter {
        private final String type;
        private final String name;

        public Parameter(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }

    public static class FieldDefinition {
        private final String fieldName;
        private final String typeName;
        private final ASTNode initializer;

        public FieldDefinition(String fieldName, String typeName) {
            this(fieldName, typeName, null);
        }

        public FieldDefinition(String fieldName, String typeName, ASTNode initializer) {
            this.fieldName = fieldName;
            this.typeName = typeName;
            this.initializer = initializer;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getTypeName() {
            return typeName;
        }

        public ASTNode getInitializer() {
            return initializer;
        }

        public boolean hasInitializer() {
            return initializer != null;
        }
    }

    /**
     * 自定义类的异常，用于标识解释器中定义的自定义类
     */
    public static class CustomClassException extends RuntimeException {
        private final String className;

        public CustomClassException(String className) {
            super("Custom class: " + className);
            this.className = className;
        }

        public String getClassName() {
            return className;
        }
    }

    /**
     * 自定义类的实例
     */
    public static class CustomClassInstance {
        private final ClassDefinition classDef;
        private final Map<String, Object> fields;

        public CustomClassInstance(ClassDefinition classDef) {
            this(classDef, null);
        }

        public CustomClassInstance(ClassDefinition classDef, ExecutionContext context) {
            this.classDef = classDef;
            this.fields = new HashMap<>();

            // 初始化字段值
            if (context != null) {
                initializeFields(context);
            }
        }

        public ClassDefinition getClassDefinition() {
            return classDef;
        }

        public Object getField(String fieldName) {
            return fields.get(fieldName);
        }

        public void setField(String fieldName, Object value) {
            fields.put(fieldName, value);
        }

        /**
         * 初始化字段值，包括默认值和初始化表达式
         */
        private void initializeFields(ExecutionContext context) {
            // 递归初始化继承层次结构中的字段
            initializeFieldsRecursive(classDef, context);
        }

        /**
         * 递归初始化字段，处理继承层次结构
         */
        private void initializeFieldsRecursive(ClassDefinition classDef, ExecutionContext context) {
            // 首先初始化父类的字段
            String superClassName = classDef.getSuperClassName();
            if (superClassName != null) {
                ClassDefinition superClassDef = context.customClasses.get(superClassName);
                if (superClassDef != null) {
                    initializeFieldsRecursive(superClassDef, context);
                }
            }

            // 初始化当前类的字段
            for (FieldDefinition fieldDef : classDef.getFields()) {
                if (fieldDef.hasInitializer()) {
                    // 如果有初始化表达式，计算并设置字段值
                    try {
                        Object value = fieldDef.getInitializer().evaluate(context);
                        fields.put(fieldDef.getFieldName(), value);
                    } catch (Exception e) {
                        logger.warn("字段初始化失败: " + fieldDef.getFieldName() + ", 错误: " + e.getMessage());
                        // 设置默认值
                        fields.put(fieldDef.getFieldName(), getDefaultValue(fieldDef.getTypeName()));
                    }
                } else {
                    // 设置字段的默认值
                    fields.put(fieldDef.getFieldName(), getDefaultValue(fieldDef.getTypeName()));
                }
            }
        }

        /**
         * 获取字段的默认值
         */
        private Object getDefaultValue(String typeName) {
            return switch (typeName) {
                case "int", "long", "short", "byte" -> 0;
                case "double", "float" -> 0.0;
                case "boolean" -> false;
                case "char" -> '\0';
                default -> null; // 对象类型默认值为null
            };
        }

        @NonNull
        @Override
        public String toString() {
            return "CustomClassInstance{" + classDef.getClassName() + "}";
        }
    }

}
