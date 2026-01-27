package com.justnothing.testmodule.command.functions.script;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.command.output.SystemOutputCollector;
import com.justnothing.testmodule.hooks.XposedBasicHook;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class TestInterpreter {

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

    private static final class InterpreterLogger extends Logger {
        @Override
        public String getTag() {
            return "TestInterpreter";
        }
    }

    private static final class StandaloneLogger extends Logger {
        @Override
        public String getTag() {
            return "TestInterpreter";
        }

        @Override
        public void debug(String str) {
            System.out.println("[DEBUG] " + str);
        }

        @Override
        public void debug(Throwable th) {
            System.out.println("[DEBUG] " + th.getMessage());
            th.printStackTrace(System.out);
        }

        @Override
        public void debug(String str, Throwable th) {
            System.out.println("[DEBUG] " + str);
            th.printStackTrace(System.out);
        }

        @Override
        public void info(String str) {
            System.out.println("[INFO] " + str);
        }

        @Override
        public void info(Throwable th) {
            System.out.println("[INFO] " + th.getMessage());
            th.printStackTrace(System.out);
        }

        @Override
        public void info(String str, Throwable th) {
            System.out.println("[INFO] " + str);
            th.printStackTrace(System.out);
        }

        @Override
        public void warn(String str) {
            System.out.println("[WARN] " + str);
        }

        @Override
        public void warn(Throwable th) {
            System.out.println("[WARN] " + th.getMessage());
            th.printStackTrace(System.out);
        }

        @Override
        public void warn(String str, Throwable th) {
            System.out.println("[WARN] " + str);
            th.printStackTrace(System.out);
        }

        @Override
        public void error(String str) {
            System.err.println("[ERROR] " + str);
        }

        @Override
        public void error(Throwable th) {
            System.err.println("[ERROR] " + th.getMessage());
            th.printStackTrace(System.err);
        }

        @Override
        public void error(String str, Throwable th) {
            System.err.println("[ERROR] " + str);
            th.printStackTrace(System.err);
        }
    }

    private static final String getStackTraceString(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        return sw.toString();
    }

    private static Class<?> findClassThroughApi(String className, ClassLoader classLoader) {
        try {
            return XposedBasicHook.ClassFinder.withClassLoader(classLoader).find(className);
        } catch (Exception e) {
            try {
                return Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException ex) {
                return null;
            }
        }
    }

    private static boolean isStandaloneMode() {
        try {
            Class<?> clazz = Class.forName("android.util.Log");
            Method method = clazz.getMethod("i", String.class, String.class);
            method.invoke(null, "TestInterpreter", "这个日志是用来测试现在是不是在安卓环境里的");
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static final Logger logger = isStandaloneMode() ? new StandaloneLogger() : new InterpreterLogger();

    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, List<Class<?>> usingArgTypes) {
        if (methodArgsTypes.length != usingArgTypes.size())
            return false;
        for (int i = 0; i < methodArgsTypes.length; i++)
            if (!methodArgsTypes[i].isAssignableFrom(usingArgTypes.get(i))
                    && !isPrimitiveWrapperMatch(methodArgsTypes[i], usingArgTypes.get(i))
                    && Void.class != usingArgTypes.get(i))
                return false;
        return true;
    }

    // 就很神秘你知道吗

    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, Class<?>[] usingArgTypes) {
        return isApplicableArgs(methodArgsTypes, Arrays.asList(usingArgTypes));
    }

    public static boolean isApplicableArgs(List<Class<?>> methodArgsTypes, List<Class<?>> usingArgTypes) {
        return isApplicableArgs(methodArgsTypes.toArray(new Class<?>[0]), usingArgTypes);
    }

    public static boolean isApplicableArgs(List<Class<?>> methodArgsTypes, Class<?>[] usingArgTypes) {
        return isApplicableArgs(methodArgsTypes.toArray(new Class<?>[0]), usingArgTypes);
    }

    private static boolean isPrimitiveWrapperMatch(Class<?> target, Class<?> source) {
        // 是这样的
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

    private static boolean isReturnTypeMatch(String expectedType, Object returnValue, ExecutionContext context) {
        if ("void".equals(expectedType)) {
            return returnValue == null;
        }

        if (returnValue == null) {
            return true;
        }

        Class<?> expectedClass;
        try {
            expectedClass = context.findClassInternal(expectedType);
        } catch (ClassNotFoundException e) {
            return false;
        }

        Class<?> actualClass = returnValue.getClass();

        if (expectedClass.isAssignableFrom(actualClass)) {
            return true;
        }

        return isPrimitiveWrapperMatch(expectedClass, actualClass);
    }

    private static String getArrayTypeNameWithoutLength(String typeName) {
        boolean insideBracket = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < typeName.length(); i++) {
            if (typeName.charAt(i) == '[') {
                insideBracket = true;
                sb.append('[');
            } else if (typeName.charAt(i) == ']') {
                insideBracket = false;
                sb.append(']');
            } else if (!insideBracket)
                sb.append(typeName.charAt(i));
        }
        return sb.toString();
    }

    private static final Set<String> KEYWORDS = Set.of(
            "public", "private", "protected", "static", "final", "abstract",
            "class", "interface", "enum", "void", "int", "long", "short",
            "byte", "char", "float", "double", "boolean", "true", "false",
            "if", "else", "for", "while", "do", "switch", "case", "default",
            "break", "continue", "return", "new", "this", "super",
            "null", "try", "catch", "finally", "throw", "throws",
            "auto" // auto类型java没有，但是也算
    );

    private static final Set<String> ACCESS_MODIFIERS = Set.of(
            "public", "private", "protected");

    private static final Set<String> TYPES = Set.of(
            "void", "int", "long", "short", "byte", "char", "float", "double", "boolean");

    private static boolean isAccessModifier(String word) {
        return word != null && ACCESS_MODIFIERS.contains(word);
    }

    private static boolean isKeyword(String word) {
        return word != null && KEYWORDS.contains(word);
    }

    private static boolean isTypeOrVoid(String word) {
        if (word == null || word.isEmpty())
            return false;
        if (TYPES.contains(word))
            return true;
        return Character.isUpperCase(word.charAt(0));
    }

    public static class ExecutionContext {

        private final ConcurrentHashMap<String, Variable> variables;
        private final ConcurrentHashMap<String, BuiltInFunction> builtIns;
        private final List<String> imports;
        private final ClassLoader classLoader;
        private IOutputHandler outputBuffer;
        private IOutputHandler warnMsgBuffer;
        private final ConcurrentHashMap<String, Class<?>> classCache;
        private final ConcurrentHashMap<String, ClassDefinition> customClasses;
        private final Stack<Map<String, Variable>> scopeStack = new Stack<>();
        private boolean shouldBreak;
        private boolean shouldContinue;
        private boolean shouldReturn;
        private Object returnValue;
        private String currentMethodReturnType;

        public ExecutionContext(ClassLoader classLoader) {
            logger.debug("创建ExecutionContext，类加载器: " + classLoader);
            this.classLoader = classLoader;
            this.outputBuffer = new SystemOutputCollector(System.out, System.in);
            this.warnMsgBuffer = new SystemOutputCollector(System.err, System.in);
            this.variables = new ConcurrentHashMap<>();
            this.builtIns = new ConcurrentHashMap<>();
            this.imports = new ArrayList<>();
            this.classCache = new ConcurrentHashMap<>();
            this.customClasses = new ConcurrentHashMap<>();
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
            this.variables = new ConcurrentHashMap<>();
            this.builtIns = new ConcurrentHashMap<>();
            this.imports = new ArrayList<>();
            this.classCache = new ConcurrentHashMap<>();
            this.customClasses = new ConcurrentHashMap<>();
            setupDefaultImports();
            setupBuiltInFunctions();
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

        public void setVariable(String name, Object value) {
            variables.put(name, new Variable(value));
        }

        public void setVariable(Map<String, Object> variables) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                setVariable(entry.getKey(), entry.getValue());
            }
        }

        public void replaceVariables(Map<String, Object> newVars) {
            this.variables.clear();
            for (Map.Entry<String, Object> entry : newVars.entrySet()) {
                if (this.variables.containsKey(entry.getKey())) {
                    this.variables.put(entry.getKey(), new Variable(entry.getValue()));
                }
            }
        }

        public void replaceVariablesWith(Map<String, Variable> variables) {
            this.variables.clear();
            this.variables.putAll(variables);
        }

        public Variable getVariable(String name) {
            if (!variables.containsKey(name)) {
                logger.error("未定义的变量: " + name);
                throw new RuntimeException("Undefined variable: " + name);
            }
            return variables.get(name);
        }

        public boolean hasVariable(String name) {
            return variables.containsKey(name);
        }

        public void deleteVariable(String name) {
            variables.remove(name);
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

        public void recordScope() {
            scopeStack.push(new HashMap<>(variables));
        }

        public void restoreScope() {
            if (!scopeStack.isEmpty()) {
                variables.clear();
                variables.putAll(scopeStack.pop());
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
            return clazz.cast(object);
        }

        public Map<String, Variable> getAllVariables() {
            Map<String, Variable> result = new HashMap<>();
            for (Map.Entry<String, Variable> entry : variables.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
            return result;
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
                    return Integer.class;
                case "long":
                    logger.debug("基本类型: long");
                    return Long.class;
                case "float":
                    logger.debug("基本类型: float");
                    return Float.class;
                case "double":
                    logger.debug("基本类型: double");
                    return Double.class;
                case "boolean":
                    logger.debug("基本类型: boolean");
                    return Boolean.class;
                case "char":
                    logger.debug("基本类型: char");
                    return Character.class;
                case "byte":
                    logger.debug("基本类型: byte");
                    return Byte.class;
                case "short":
                    logger.debug("基本类型: short");
                    return Short.class;
                case "void":
                    logger.debug("基本类型: void");
                    return Void.class;
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
                    logger.debug("通过导入找到类: " + clazz.getName());
                    return clazz;
                }
            }
            throw new ClassNotFoundException("Class not found: " + className);
        }

        private void setupBuiltInFunctions() {
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
                result.append("String: ").append(target.toString()).append("\n");
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
                    for (Class<?> iface : interfaces) {
                        result.append("  ").append(iface.getName()).append("\n");
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
                                    constructor.setAccessible(true);
                                    return constructor.newInstance(args);
                                }
                            }
                        }

                        throw new RuntimeException("No matching constructor found");
                    });
                }
            });

            // 添加函数式接口支持
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
                    });
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

            Class<?> clazz;
            Object targetObj;

            if (object == null) {
                throw new NullPointerException("Attempt to invoke method" + methodName + " on a null object reference");
            } else {
                clazz = object.getClass();
                targetObj = object;
            }

            List<Class<?>> argTypes = args.stream()
                    .map(arg -> arg != null ? arg.getClass() : Void.class)
                    .collect(Collectors.toList());

            Method method = findMethod(clazz, methodName, argTypes);
            return method.invoke(targetObj, args.toArray());
        }

        public Object callStaticMethod(String className, String methodName, List<Object> args) throws Exception {
            Class<?> clazz = findClass(className);
            if (clazz == null) {
                throw new ClassNotFoundException("Class not found: " + className);
            }
            Method method = findMethod(clazz, methodName,
                    args.stream().map(Object::getClass).collect(Collectors.toList()));
            return method.invoke(null, args.toArray());
        }

        public Method findMethod(Class<?> clazz, String name, List<Class<?>> argTypes) throws Exception {
            Method applicable = null;
            StringBuilder sb = new StringBuilder();
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(name)) {
                    sb.append(Arrays.toString(method.getParameterTypes())).append("\n");
                    if (Arrays.equals(method.getParameterTypes(), argTypes.toArray())) {
                        return method;
                    } else if (isApplicableArgs(method.getParameterTypes(), argTypes)) {
                        applicable = method;
                    }
                }
            }
            if (applicable != null) {
                // logger.warn("使用了参数不完全匹配的类方法" + clazz.getName() + "." + name);
                // logger.warn("要求的参数: " + Arrays.toString(argTypes.toArray()));
                // logger.warn("实际参数: " + Arrays.toString(applicable.getParameterTypes()));
                return applicable;
            }
            logger.error("找不到类" + clazz.getName() + "的方法" + name + "，参数为" + Arrays.toString(argTypes.toArray()));
            String sigString = Arrays.toString(argTypes.toArray());
            sigString = sigString.substring(1);
            sigString = sigString.substring(0, sigString.length() - 1);
            throw new NoSuchMethodException("Method not found: " +
                    clazz.getName() + "." + name + "(" + sigString + ")"
                    + (sb.length() > 0 ? "\nAvailable signatures:\n" + sb + "\n" : ""));

        }
    }

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

    public static class ClassDefinition {
        private final String className;
        private final Map<String, MethodDefinition> methods;
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
            methods.put(methodName, method);
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

    public static abstract class ASTNode {
        /**
         * 评估这个节点。
         *
         * @param context 执行上下文
         * @return 评估结果
         * @throws Exception 如果评估出错
         */
        public abstract Object evaluate(ExecutionContext context) throws Exception;

        public abstract Class<?> getType(ExecutionContext context) throws Exception;
    }

    private static boolean toBoolean(Object obj) {
        if (obj == null)
            return false;
        if (obj instanceof Boolean)
            return (Boolean) obj;
        if (obj instanceof Number)
            return ((Number) obj).doubleValue() != 0;
        if (obj instanceof String)
            return !((String) obj).isEmpty();
        if (obj instanceof Character)
            return ((Character) obj) != 0;
        return true;
    }

    private static String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static class LiteralNode extends ASTNode {
        private final Object value;
        private final Class<?> type;

        public LiteralNode(Object value) {
            this.value = value;
            this.type = value != null ? value.getClass() : Void.class;
        }

        public LiteralNode(Object value, Class<?> type) {
            this.value = value;
            this.type = type;
        }

        @Override
        public Object evaluate(ExecutionContext context) {
            return value;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return type;
        }
    }

    public static class ImportNode extends ASTNode {
        private final String packageName;

        public ImportNode(String packageName) {
            this.packageName = packageName;
        }

        @Override
        public Object evaluate(ExecutionContext context) {
            try {
                context.addImport(packageName);
            } catch (ClassNotFoundException e) {
                logger.warn("导入包失败: " + packageName + ": " + e.getMessage());
            }
            return null;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class ArrayNode extends ASTNode {

        private final List<ASTNode> elements;

        public ArrayNode(List<ASTNode> elements) {
            this.elements = elements;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws RuntimeException {
            try {
                ArrayList<Object> values = new ArrayList<>();
                Class<?> type = getElementType(context);
                for (ASTNode element : elements) {
                    Object value = element.evaluate(context);
                    values.add(value);
                    if (!type.isAssignableFrom(value.getClass())) {
                        logger.error("数组元素类型不一致: " + type + " != " + value.getClass());
                        throw new RuntimeException(
                                "Array element type mismatch: " + type + " != " + value.getClass());
                    }
                }
                if (values.isEmpty())
                    return new Object[0];
                Object array = Array.newInstance(type, values.size());
                for (int i = 0; i < values.size(); i++)
                    Array.set(array, i, values.get(i));
                return array;
            } catch (Exception e) {
                logger.error("解析数组时出现错误: " + e);
                throw new RuntimeException("Error parsing array: " + e);
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            if (elements.isEmpty())
                return Object[].class;
            return Array.newInstance(elements.get(0).getType(context), 0).getClass();
        }

        public Class<?> getElementType(ExecutionContext context) throws Exception {
            if (elements.isEmpty())
                return Object.class;
            return elements.get(0).getType(context);
        }
    }

    public static class MapNode extends ASTNode {

        private final Map<String, ASTNode> entries;
        private final Class<?> type;

        public MapNode(Map<String, ASTNode> entries, Class<?> type) {
            this.entries = entries;
            this.type = type;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws RuntimeException {
            try {
                Map<String, Object> map = new HashMap<>();
                for (Map.Entry<String, ASTNode> entry : entries.entrySet()) {
                    map.put(entry.getKey(), entry.getValue().evaluate(context));
                }
                return map;
            } catch (Exception e) {
                logger.error("解析Map时出现错误: " + e);
                throw new RuntimeException("Error parsing map: " + e);
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return type;
        }
    }

    public static class BinaryOperatorNode extends ASTNode {
        private final String operator;
        private final ASTNode left;
        private final ASTNode right;

        public BinaryOperatorNode(String operator, ASTNode left, ASTNode right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object leftVal = left.evaluate(context);
            Object rightVal = null;
            if (right != null)
                rightVal = right.evaluate(context);

            return switch (operator) {
                case "+" -> add(leftVal, rightVal);
                case "-" -> subtract(leftVal, rightVal);
                case "*" -> multiply(leftVal, rightVal);
                case "/" -> divide(leftVal, rightVal);
                case "%" -> modulo(leftVal, rightVal);
                case "==" -> equals(leftVal, rightVal);
                case "!=" -> !equals(leftVal, rightVal);
                case "<" -> compare(leftVal, rightVal) < 0;
                case ">" -> compare(leftVal, rightVal) > 0;
                case "<=" -> compare(leftVal, rightVal) <= 0;
                case ">=" -> compare(leftVal, rightVal) >= 0;
                case "&&" -> logicAnd(leftVal, rightVal);
                case "||" -> logicOr(leftVal, rightVal);
                case "^" -> xor(leftVal, rightVal);
                case ">>" -> rightShift(leftVal, rightVal);
                case "<<" -> leftShift(leftVal, rightVal);
                case "&" -> bitAnd(leftVal, rightVal);
                case "|" -> bitOr(leftVal, rightVal);
                case "!" -> logicNot(leftVal);
                case "~" -> bitRev(leftVal);
                case ">>>" -> unsignedRightShift(leftVal, rightVal);
                case "instanceof" -> leftVal != null && rightVal != null
                        && leftVal.getClass().isAssignableFrom(rightVal.getClass());
                // 不处理= += -=之类的运算符，因为它们归Assignment管
                default -> {
                    logger.error("未知的运算符: " + operator);
                    throw new RuntimeException("Unknown operator: " + operator);
                }
            };
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return switch (operator) {
                case "+", "-", "*", "/", "%", ">>", "<<", "~" -> Number.class;
                case "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!" -> Boolean.class;
                default -> Object.class;
            };
        }

        private Object add(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply operations on null values");
            }
            if (a instanceof Number num1 && b instanceof Number num2) {
                if (a instanceof Double || b instanceof Double) {
                    return num1.doubleValue() + num2.doubleValue();
                }
                if (a instanceof Float || b instanceof Float) {
                    return num1.floatValue() + num2.floatValue();
                }
                if (a instanceof Long || b instanceof Long) {
                    return num1.longValue() + num2.longValue();
                }
                return num1.intValue() + num2.intValue();
            } else if (a instanceof String || b instanceof String) {
                return a + b.toString();
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行加法运算");
            throw new RuntimeException("Cannot add: " + a.getClass() + " and " + b.getClass());
        }

        private Object subtract(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply operations on null values");
            }
            if (a instanceof Number num1 && b instanceof Number num2) {
                if (a instanceof Double || b instanceof Double) {
                    return num1.doubleValue() - num2.doubleValue();
                }
                if (a instanceof Float || b instanceof Float) {
                    return num1.floatValue() - num2.floatValue();
                }
                if (a instanceof Long || b instanceof Long) {
                    return num1.longValue() - num2.longValue();
                }
                return num1.intValue() - num2.intValue();
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行减法运算");
            throw new RuntimeException("Cannot subtract: " + a.getClass() + " and " + b.getClass());
        }

        private Object multiply(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行运算: a = " + a + ", b = " + b);
                throw new RuntimeException("Cannot apply operations on null values");
            }
            if (a instanceof Number num1 && b instanceof Number num2) {
                if (a instanceof Double || b instanceof Double) {
                    return num1.doubleValue() * num2.doubleValue();
                }
                if (a instanceof Float || b instanceof Float) {
                    return num1.floatValue() * num2.floatValue();
                }
                if (a instanceof Long || b instanceof Long) {
                    return num1.longValue() * num2.longValue();
                }
                return num1.intValue() * num2.intValue();
            }
            if (a instanceof String && b instanceof Number) {
                return repeat((String) a, (Integer) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行乘法运算");
            throw new RuntimeException("Cannot multiply: " + a.getClass() + " and " + b.getClass());
        }

        private Object divide(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply operations on null values");
            }
            if (a instanceof Number num1 && b instanceof Number num2) {
                if (a instanceof Double || b instanceof Double) {
                    return num1.doubleValue() / num2.doubleValue();
                }
                if (a instanceof Float || b instanceof Float) {
                    return num1.floatValue() / num2.floatValue();
                }
                if (a instanceof Long || b instanceof Long) {
                    return num1.longValue() / num2.longValue();
                }
                return num1.intValue() / num2.intValue();
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行除法运算");
            throw new RuntimeException("Cannot divide: " + a.getClass() + " and " + b.getClass());
        }

        private Object modulo(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply operations on null values");
            }
            if (a instanceof Number num1 && b instanceof Number num2) {
                if (a instanceof Double || b instanceof Double) {
                    return num1.doubleValue() % num2.doubleValue();
                }
                if (a instanceof Float || b instanceof Float) {
                    return num1.floatValue() % num2.floatValue();
                }
                if (a instanceof Long || b instanceof Long) {
                    return num1.longValue() % num2.longValue();
                }
                return num1.intValue() % num2.intValue();
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行模运算");
            throw new RuntimeException("Cannot modulo: " + a.getClass() + " and " + b.getClass());
        }

        private boolean equals(Object a, Object b) {
            if (a == null && b == null)
                return true;
            if (a == null || b == null)
                return false;
            return a.equals(b);
        }

        private int compare(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行比较: a = " + a);

                throw new RuntimeException("Cannot apply comparison on null values");
            }
            if (a instanceof Comparable && b instanceof Comparable) {
                @SuppressWarnings("unchecked")
                Comparable<Object> compA = (Comparable<Object>) a;
                return compA.compareTo(b);
            }
            logger.error("无法比较" + a.getClass() + "与" + b.getClass());
            throw new RuntimeException("Cannot compare: " + a.getClass() + " and " + b.getClass());
        }

        private boolean logicAnd(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            return toBoolean(a) && toBoolean(b);
        }

        private boolean logicOr(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            return toBoolean(a) || toBoolean(b);
        }

        private boolean logicXor(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            return toBoolean(a) ^ toBoolean(b);
        }

        private boolean logicNot(Object a) {
            if (a == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + null);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            return !toBoolean(a);
        }

        private Object xor(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            if (a instanceof Boolean && b instanceof Boolean) {
                return logicXor(a, b);
            } else {
                return bitXor(a, b);
            }
        }

        private Object bitAnd(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) & ((Integer) b);
            } else if (a instanceof Long && b instanceof Long) {
                return ((Long) a) & ((Long) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行按位与操作");
            throw new RuntimeException("Cannot bitwise and: " + a.getClass() + " and " + b.getClass());
        }

        private Object bitOr(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) | ((Integer) b);
            } else if (a instanceof Long && b instanceof Long) {
                return ((Long) a) | ((Long) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行按位或操作");
            throw new RuntimeException("Cannot bitwise or: " + a.getClass() + " and " + b.getClass());
        }

        private Object bitXor(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + a + ", b = " + b);

                throw new RuntimeException("Cannot apply logic operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) ^ ((Integer) b);
            } else if (a instanceof Long && b instanceof Long) {
                return ((Long) a) ^ ((Long) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行按位异或操作");
            throw new RuntimeException("Cannot bitwise xor: " + a.getClass() + " and " + b.getClass());
        }

        private Object bitRev(Object a) {
            if (a == null) {
                logger.error("无法在null值上执行逻辑运算: a = " + null);

                throw new RuntimeException("Cannot apply bitwise operations on null values");
            }
            if (a instanceof Integer) {
                return ~((Integer) a);
            } else if (a instanceof Long) {
                return ~((Long) a);
            }
            logger.error("无法在" + a.getClass() + "上进行按位取反操作");
            throw new RuntimeException("Cannot bitwise invert: " + a.getClass());
        }

        private Object rightShift(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行位运算");
                throw new RuntimeException("Cannot apply bitwise operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) >> ((Integer) b);
            } else if (a instanceof Long && b instanceof Integer) {
                return ((Long) a) >> ((Integer) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行右移操作");
            throw new RuntimeException("Cannot right shift: " + a.getClass() + " and " + b.getClass());
        }

        private Object leftShift(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行位运算");
                throw new RuntimeException("Cannot apply bitwise operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) << ((Integer) b);
            } else if (a instanceof Long && b instanceof Integer) {
                return ((Long) a) << ((Integer) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行左移操作");
            throw new RuntimeException("Cannot left shift: " + a.getClass() + " and " + b.getClass());
        }

        private Object unsignedRightShift(Object a, Object b) {
            if (a == null || b == null) {
                logger.error("无法在null值上执行位运算");
                throw new RuntimeException("Cannot apply bitwise operations on null values");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) >>> ((Integer) b);
            } else if (a instanceof Long && b instanceof Integer) {
                return ((Long) a) >>> ((Integer) b);
            }
            logger.error("无法在" + a.getClass() + "和" + b.getClass() + "之间进行无符号右移操作");
            throw new RuntimeException("Cannot unsigned right shift: " + a.getClass() + " and " + b.getClass());
        }

    }

    public static class ClassReferenceNode extends ASTNode {
        private final String className;

        public ClassReferenceNode(String className) {
            this.className = className;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            return getClass(context);
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Class.class;
        }

        public Class<?> getClass(ExecutionContext context) throws ClassNotFoundException {
            return context.findClass(className);
        }

        public String getClassName() {
            return className;
        }
    }

    public static class VariableNode extends ASTNode {
        private final String name;

        public VariableNode(String name) {
            this.name = name;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Variable var;
            if (context.hasClass(name))
                return context.findClass(name);
            try {
                var = context.getVariable(name);
                return var.value;
            } catch (RuntimeException ignored) {
            }

            if (context.hasVariable("this")) {
                Object thisValue = context.getVariable("this").value;
                if (thisValue instanceof CustomClassInstance instance) {
                    FieldDefinition fieldDef = instance.getClassDefinition().getField(name);
                    if (fieldDef != null) {
                        return instance.getField(name);
                    }
                }
            }

            // 检查是否是 BuiltInFunction
            if (context.hasBuiltIn(name)) {
                return context.getBuiltIn(name);
            }

            logger.error("未定义变量或类" + name);
            throw new RuntimeException("Undefined variable: " + name);
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws RuntimeException {
            // 首先尝试作为变量获取
            if (context.hasVariable(name)) {
                Variable var = context.getVariable(name);
                return var.type;
            }

            // 检查是否是 BuiltInFunction
            if (context.hasBuiltIn(name)) {
                BuiltInFunction builtIn = context.getBuiltIn(name);
                if (builtIn != null) {
                    return Object.class;
                }
            }

            // 如果不是变量，尝试作为类名解析
            try {
                context.findClass(name);
                return Class.class;
            } catch (Exception e) {
                logger.error("未定义变量或类" + name);
                throw new RuntimeException("Undefined variable: " + name);
            }
        }
    }

    public static class MethodCallNode extends ASTNode {

        private final ASTNode target;
        private final String methodName;
        private final List<ASTNode> arguments;

        public MethodCallNode(ASTNode target, String methodName, List<ASTNode> arguments) {
            this.target = target;
            this.methodName = methodName;
            this.arguments = arguments;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if (target instanceof VariableNode && context.hasBuiltIn(((VariableNode) target).name)) {
                BuiltInFunction builtIn = context.getBuiltIn(((VariableNode) target).name);
                if (builtIn != null) {
                    List<Object> args = new ArrayList<>();
                    for (ASTNode arg : arguments) {
                        Object argValue = arg.evaluate(context);
                        args.add(argValue);
                    }
                    return builtIn.call(args);
                }
            }

            List<Object> argsList = new ArrayList<>();
            for (int i = 0; i < arguments.size(); i++) {
                ASTNode arg = arguments.get(i);
                Object argValue = arg.evaluate(context);
                argsList.add(argValue);
            }

            if (target != null) {
                Object targetObj = target.evaluate(context);

                if (targetObj instanceof Lambda) {
                    return ((Lambda) targetObj).apply(argsList.toArray());
                }

                if (targetObj instanceof CustomClassInstance customInstance) {
                    ClassDefinition classDef = customInstance.getClassDefinition();
                    MethodDefinition methodDef = findMethodInHierarchy(classDef, methodName, context);
                    if (methodDef != null) {
                        // 保存当前的控制流状态
                        boolean originalShouldReturn = context.shouldReturn;
                        boolean originalShouldBreak = context.shouldBreak;
                        boolean originalShouldContinue = context.shouldContinue;

                        // 保存当前上下文状态
                        context.recordScope();

                        // 设置this引用
                        context.setVariable("this", customInstance);

                        // 设置方法参数
                        List<Parameter> parameters = methodDef.getParameters();
                        for (int i = 0; i < parameters.size(); i++) {
                            Parameter param = parameters.get(i);
                            Object argValue = argsList.get(i);
                            context.setVariable(param.getName(), argValue);
                        }

                        // 执行方法体
                        Object result = null;
                        if (methodDef.getBody() != null) {
                            // 保存控制流状态的原始值
                            boolean savedShouldReturn = context.shouldReturn;
                            boolean savedShouldBreak = context.shouldBreak;
                            boolean savedShouldContinue = context.shouldContinue;
                            Object savedReturnValue = context.returnValue;
                            String savedMethodReturnType = context.getCurrentMethodReturnType();

                            // 设置当前方法的返回类型
                            context.setCurrentMethodReturnType(methodDef.getReturnType());

                            // 重置控制流状态
                            context.shouldReturn = false;
                            context.shouldBreak = false;
                            context.shouldContinue = false;

                            // 执行方法体
                            result = methodDef.getBody().evaluate(context);

                            // 检查方法体是否设置了return
                            if (context.shouldReturn) {
                                // 获取返回值（从方法体执行结果中获取）
                                Object returnValue = context.returnValue;

                                // 恢复外层的控制流状态
                                context.shouldReturn = savedShouldReturn;
                                context.shouldBreak = savedShouldBreak;
                                context.shouldContinue = savedShouldContinue;
                                context.returnValue = savedReturnValue;
                                context.setCurrentMethodReturnType(savedMethodReturnType);

                                return returnValue;
                            }

                            // 恢复外层的控制流状态
                            context.shouldReturn = savedShouldReturn;
                            context.shouldBreak = savedShouldBreak;
                            context.shouldContinue = savedShouldContinue;
                            context.returnValue = savedReturnValue;
                            context.setCurrentMethodReturnType(savedMethodReturnType);

                            return result;
                        }

                        // 如果没有方法体，返回null
                        return null;
                    } else {
                        throw new RuntimeException(
                                "Method '" + methodName + "' not found in class hierarchy " + classDef.getClassName());
                    }
                }

                return context.callMethod(targetObj, methodName, argsList);
            }

            logger.error("尝试在null上调用方法" + methodName);
            throw new RuntimeException("Method call on null object");
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            if (target instanceof VariableNode && context.hasBuiltIn(((VariableNode) target).name)) {
                return Object.class;
            }

            Object targetObj = target != null ? target.evaluate(context) : null;
            Class<?> targetClass;

            if (targetObj instanceof Class) {
                targetClass = (Class<?>) targetObj;
            } else if (targetObj != null) {
                targetClass = targetObj.getClass();
            } else if (target instanceof ClassReferenceNode) {
                targetClass = ((ClassReferenceNode) target).getClass(context);
            } else {
                logger.error("无法确定方法调用的目标: " + methodName);
                throw new RuntimeException("Cannot determine target for method call: " + methodName);
            }

            Class<?>[] argTypes = new Class<?>[arguments.size()];
            for (int i = 0; i < arguments.size(); i++) {
                argTypes[i] = arguments.get(i).getType(context);
            }

            try {
                Method method = context.findMethod(targetClass, methodName, Arrays.asList(argTypes));
                return method.getReturnType();
            } catch (NoSuchMethodException e) {
                logger.error("没有找到" + targetClass.getName() + "的方法" + methodName);
                throw new RuntimeException("Method not found: " + methodName + " on " + targetClass.getName());
            }
        }

    }

    public static MethodDefinition findMethodInHierarchy(ClassDefinition classDef, String methodName,
            ExecutionContext context) {
        // 首先在当前类中查找方法
        MethodDefinition methodDef = classDef.getMethod(methodName);
        if (methodDef != null) {
            return methodDef;
        }

        // 如果当前类没有找到，检查父类
        String superClassName = classDef.getSuperClassName();
        if (superClassName != null) {
            ClassDefinition superClassDef = context.customClasses.get(superClassName);
            if (superClassDef != null) {
                return findMethodInHierarchy(superClassDef, methodName, context);
            }
        }

        // 如果没有找到，返回null
        return null;
    }

    public static FieldDefinition findFieldInHierarchy(ClassDefinition classDef, String fieldName,
            ExecutionContext context) {

        // 同上
        FieldDefinition fieldDef = classDef.getField(fieldName);
        if (fieldDef != null) {
            return fieldDef;
        }

        String superClassName = classDef.getSuperClassName();
        if (superClassName != null) {
            ClassDefinition superClassDef = context.customClasses.get(superClassName);
            if (superClassDef != null) {
                return findFieldInHierarchy(superClassDef, fieldName, context);
            }
        }

        return null;
    }

    public static class ConstructorCallNode extends ASTNode {
        private final String className;
        private final List<ASTNode> arguments;
        private final ASTNode arrInitial;

        public ConstructorCallNode(String className, List<ASTNode> arguments, ASTNode arrInitial) {
            this.className = className;
            this.arguments = arguments;
            this.arrInitial = arrInitial;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {

            if (className.contains("[") || className.contains("]")) {
                if (!className.contains("[") || !className.contains("]")) {
                    logger.error("数组类型的类名中缺少方括号");
                    throw new RuntimeException("Array constructor class name missing brackets");
                }
                String elementType = className.substring(0, className.indexOf("["));
                List<Integer> dimensions = new ArrayList<>();
                String arrayDim = className.substring(className.indexOf("["));
                int idx;
                while ((idx = arrayDim.indexOf("[")) != -1) {
                    int rightBracket = arrayDim.indexOf("]");
                    if (rightBracket == -1) {
                        logger.error("数组类型的类名中缺少右方括号");
                        throw new RuntimeException("Array constructor class name missing right bracket");
                    }
                    String thisDim = arrayDim.substring(idx + 1, rightBracket);
                    dimensions.add(thisDim.isEmpty() ? -1 : Integer.parseInt(thisDim));
                    arrayDim = arrayDim.substring(rightBracket + 1);
                }
                return createArray(context, elementType, dimensions, arrInitial);
            } else {
                Class<?> clazz;
                try {
                    clazz = context.findClass(className);
                } catch (CustomClassException e) {
                    // 这是自定义类，创建自定义类的实例
                    ClassDefinition classDef = context.customClasses.get(className);
                    if (classDef == null) {
                        throw new RuntimeException("Custom class not found: " + className);
                    }

                    // 先创建实例（用于字段初始化）
                    CustomClassInstance instance = new CustomClassInstance(classDef, context);

                    List<ConstructorDefinition> constructors = classDef.getConstructors();
                    if (!constructors.isEmpty()) {
                        Object[] args = new Object[arguments.size()];
                        Class<?>[] argTypes = new Class<?>[arguments.size()];
                        for (int i = 0; i < arguments.size(); i++) {
                            args[i] = arguments.get(i).evaluate(context);
                            argTypes[i] = arguments.get(i).getType(context);
                        }

                        ConstructorDefinition constructorDef = classDef.getConstructor(Arrays.asList(argTypes),
                                context);
                        if (constructorDef != null) {
                            ExecutionContext constructorContext = new ExecutionContext(context.getClassLoader());

                            constructorContext.customClasses.putAll(context.customClasses);

                            constructorContext.setVariable("this", instance);

                            List<Parameter> params = constructorDef.getParameters();
                            for (int i = 0; i < params.size() && i < args.length; i++) {
                                constructorContext.setVariable(params.get(i).getName(), args[i]);
                            }

                            if (constructorDef.getBody() != null) {
                                constructorContext.setCurrentMethodReturnType("void");
                                constructorDef.getBody().evaluate(constructorContext);

                                String constructorOutput = constructorContext.getOutput();
                                if (!constructorOutput.isEmpty()) {
                                    context.print(constructorOutput);
                                }
                            }
                        }
                    }

                    return instance;
                }

                if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                    logger.error("尝试实例化一个抽象类或接口" + className);
                    throw new InstantiationException("Cannot instantiate abstract class or interface: " + className);
                }
                Object[] args = new Object[arguments.size()];
                Class<?>[] argTypes = new Class<?>[arguments.size()];

                for (int i = 0; i < arguments.size(); i++) {
                    args[i] = arguments.get(i).evaluate(context);
                    argTypes[i] = arguments.get(i).getType(context);
                }

                Constructor<?> constructor = findConstructor(clazz, argTypes);
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            }

        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            try {
                return context.findClass(className);
            } catch (CustomClassException e) {
                // 对于自定义类，返回Object.class作为类型
                return Object.class;
            }
        }

        private Constructor<?> findConstructor(Class<?> clazz, Class<?>[] argTypes)
                throws NoSuchMethodException {

            try {
                return clazz.getDeclaredConstructor(argTypes);
            } catch (NoSuchMethodException ignored) {
            }

            Constructor<?> bestMatch = null;
            StringBuilder sb = new StringBuilder();
            for (Constructor<?> item : clazz.getConstructors()) {
                if (isApplicableArgs(item.getParameterTypes(), Arrays.asList(argTypes))) {
                    bestMatch = item;
                }
                sb.append(Arrays.toString(item.getParameterTypes())).append("\n");
            }

            if (bestMatch != null) {
                return bestMatch;
            }

            if (argTypes.length == 0) {
                try {
                    return clazz.getDeclaredConstructor();
                } catch (NoSuchMethodException ignored) {
                }
            }
            logger.warn("无法找到与目标参数类型匹配的构造函数: ");
            logger.warn(" -> 访问的类型为 " + clazz.getName());
            logger.warn(" -> 要求的参数类型为 " + Arrays.toString(argTypes));

            throw new NoSuchMethodException("Constructor not found for class: " + clazz.getName() +
                    " with " + argTypes.length + " arguments\n" + "\nAvailable constructors:\n" +
                    sb + "\nProvided:\n" + Arrays.toString(argTypes) + "\n");
        }

        private Object createArray(ExecutionContext context, String elementType,
                List<Integer> dimensions, ASTNode arrInitial) throws Exception {
            Class<?> elementClass = context.findClass(elementType);
            if (elementClass.isInterface() || Modifier.isAbstract(elementClass.getModifiers())) {
                logger.error("尝试实例化一个抽象类或接口" + className);
                throw new InstantiationException("Cannot instantiate abstract class or interface: " + className);
            }
            int[] dimensionsArr = dimensions.stream().mapToInt(i -> i).toArray();
            boolean isSpecificLength = false;
            boolean isNotSpecificLength = false;
            for (int i : dimensionsArr) {
                if (i < 0) {
                    if (isSpecificLength) {
                        logger.error("不允许单个维度不指定长度");
                        throw new IllegalArgumentException(
                                "Array dimensions must be all specified if one of them is specified");
                    }
                    isNotSpecificLength = true;
                } else {
                    if (isNotSpecificLength) {
                        logger.error("不允许单个维度不指定长度");
                        throw new IllegalArgumentException(
                                "Array dimensions must be all specified if one of them is specified");
                    }
                    isSpecificLength = true;
                }
            }
            Object initialValue = arrInitial != null ? arrInitial.evaluate(context) : null;
            if (isSpecificLength) {
                Object array = Array.newInstance(elementClass, dimensionsArr);
                try {
                    if (initialValue != null) {
                        List<List<Integer>> indices = walkArrayDimensions(dimensions);
                        for (List<Integer> index : indices) {
                            Object target = array;
                            for (int i = 0; i < index.size() - 1; i++) {
                                if (target == null)
                                    throw new IndexOutOfBoundsException("Array size mismatch");
                                target = Array.get(target, index.get(i));
                            }
                            if (target == null)
                                throw new IndexOutOfBoundsException("Array size mismatch");
                            Array.set(target, index.get(index.size() - 1), getArrayElement(initialValue, index));
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    logger.error("数组的维度不对应");
                    logger.error("期待的维度：" + dimensions);
                    logger.error("实际维度： " + getArrayDimension(initialValue));
                    throw new IndexOutOfBoundsException(
                            "Array dimensions do not match: " + dimensions + " vs " + getArrayDimension(initialValue));
                }
                return array;
            } else {
                if (initialValue == null) {
                    logger.error("数组既没有指定长度也没有指定初始值");
                    throw new RuntimeException("Creating array with neither length nor initial value");
                }
                List<Integer> origDimensions = getArrayDimension(initialValue);
                if (origDimensions.size() != dimensions.size()) {
                    logger.error("数组的维度不对应");
                    logger.error("期待的维度：" + dimensions);
                    logger.error("实际维度： " + origDimensions);
                    throw new IndexOutOfBoundsException(
                            "Array dimensions do not match: " + dimensions + " vs " + origDimensions);
                }
                return initialValue;
            }
        }

        private List<List<Integer>> walkArrayDimensions(List<Integer> dimensions) {
            if (dimensions.isEmpty()) {
                return Collections.emptyList();
            } else if (dimensions.size() == 1) {
                List<List<Integer>> result = new ArrayList<>();
                for (int i = 0; i < dimensions.get(0); i++) {
                    List<Integer> index = new ArrayList<>();
                    index.add(i);
                    result.add(index);
                }
                return result;
            } else {
                int firstDim = dimensions.get(0);
                dimensions.remove(0);
                List<List<Integer>> subDimensions = walkArrayDimensions(dimensions);
                List<List<Integer>> result = new ArrayList<>();
                for (List<Integer> subDim : subDimensions) {
                    for (int i = 0; i < firstDim; i++) {
                        List<Integer> newDim = new ArrayList<>();
                        newDim.add(i);
                        newDim.addAll(subDim);
                        result.add(newDim);
                    }
                }
                return result;
            }
        }

        private List<Integer> getArrayDimension(Object arr) {
            if (arr.getClass().isArray()) {
                List<Integer> dimensions = new ArrayList<>();
                Object current = arr;
                while (true) {
                    assert current != null;
                    if (!current.getClass().isArray())
                        break;
                    dimensions.add(Array.getLength(current));
                    current = Array.get(current, 0);
                }
                return dimensions;
            } else if (arr instanceof List) {
                return Collections.singletonList(((List<?>) arr).size());
            } else {
                logger.error("尝试访问非数组或列表的维度");
                throw new RuntimeException("Attempt to access dimensions of non-array or list");
            }
        }

        private Object getArrayElement(Object arr, List<Integer> indices) {
            Object target = arr;
            if (target.getClass().isArray()) {
                for (int index : indices) {
                    assert target != null;
                    target = Array.get(target, index);
                }
                return target;
            } else if (arr instanceof List) {
                return ((List<?>) arr).get(indices.get(0));
            } else {
                logger.error("尝试访问非数组或列表的元素");
                throw new RuntimeException("Attempt to access element of non-array or list");
            }
        }
    }

    public static class FieldAccessNode extends ASTNode {
        private final ASTNode target;
        private final String fieldName;

        public FieldAccessNode(ASTNode target, String fieldName) {
            this.target = target;
            this.fieldName = fieldName;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object targetObj = target != null ? target.evaluate(context) : null;
            Class<?> targetClass;

            // 特殊处理：如果字段名是 "class"，直接返回目标类的Class对象
            if (fieldName.equals("class")) {
                if (targetObj == null) {
                    logger.error("无法在null上访问.class字段");
                    throw new NullPointerException("Cannot access .class on null");
                }
                return targetObj instanceof Class ? targetObj : targetObj.getClass();
            }

            // 处理自定义类实例的字段访问
            if (targetObj instanceof CustomClassInstance customInstance) {
                ClassDefinition classDef = customInstance.getClassDefinition();

                // 在类的继承层次结构中查找字段
                FieldDefinition fieldDef = findFieldInHierarchy(classDef, fieldName, context);
                if (fieldDef != null) {
                    // 返回字段值
                    return customInstance.getField(fieldName);
                } else {
                    throw new RuntimeException(
                            "Field '" + fieldName + "' not found in class hierarchy " + classDef.getClassName());
                }
            }

            // 确定目标类
            if (targetObj instanceof Class) {
                // 静态字段访问
                targetClass = (Class<?>) targetObj;
            } else if (targetObj != null) {
                // 实例字段访问
                targetClass = targetObj.getClass();
            } else if (target instanceof ClassReferenceNode) {
                // 静态字段访问: 类引用节点
                targetClass = ((ClassReferenceNode) target).getClass(context);
            } else {
                logger.error("无法解析所访问类成员的类型目标: " + fieldName);
                throw new RuntimeException("Cannot determine target for field access: " + fieldName);
            }

            Field field = findField(targetClass, fieldName);
            field.setAccessible(true);

            // 检查字段是否是静态的
            boolean isStatic = Modifier.isStatic(field.getModifiers());

            // 如果是静态字段，使用 null 作为接收者；否则使用 targetObj
            Object receiver = isStatic ? null : targetObj;

            if (receiver == null && !isStatic) {
                logger.error("尝试在 null 对象上访问实例字段: " + fieldName);
                throw new NullPointerException("Attempt to access instance field '" + fieldName + "' on null object");
            }

            return field.get(receiver);
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            Object targetObj = target != null ? target.evaluate(context) : null;
            Class<?> targetClass;

            if (targetObj instanceof Class) {
                targetClass = (Class<?>) targetObj;
            } else if (targetObj != null) {
                targetClass = targetObj.getClass();
            } else if (target instanceof ClassReferenceNode) {
                targetClass = ((ClassReferenceNode) target).getClass(context);
            } else {
                logger.error("无法解析所访问类成员的类型目标: " + fieldName);
                throw new RuntimeException("Cannot determine target for field access: " + fieldName);
            }

            Field field = findField(targetClass, fieldName);
            return field.getType();
        }

        private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
            // 首先尝试查找公共字段
            try {
                return clazz.getField(fieldName);
            } catch (NoSuchFieldException e) {
                // 如果公共字段不存在，查找所有声明的字段
                try {
                    return clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e2) {
                    // 尝试在父类中查找
                    Class<?> superClass = clazz.getSuperclass();
                    if (superClass != null) {
                        return findField(superClass, fieldName);
                    }
                    throw e2;
                }
            }
        }

        /**
         * 在类的继承层次结构中递归查找字段
         */
        private FieldDefinition findFieldInHierarchy(ClassDefinition classDef, String fieldName,
                ExecutionContext context) {
            // 首先在当前类中查找字段
            FieldDefinition fieldDef = classDef.getField(fieldName);
            if (fieldDef != null) {
                return fieldDef;
            }

            // 如果当前类没有找到，检查父类
            String superClassName = classDef.getSuperClassName();
            if (superClassName != null) {
                ClassDefinition superClassDef = context.customClasses.get(superClassName);
                if (superClassDef != null) {
                    return findFieldInHierarchy(superClassDef, fieldName, context);
                }
            }

            // 如果没有找到，返回null
            return null;
        }
    }

    public static class ArrayAccessNode extends ASTNode {
        private final ASTNode target;
        private final ASTNode index;

        public ArrayAccessNode(ASTNode target, ASTNode index) {
            this.target = target;
            this.index = index;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object targetObj = target.evaluate(context);
            Object indexObj = index.evaluate(context);

            if (targetObj == null) {
                logger.error("尝试在null数组上访问元素");
                throw new NullPointerException("Cannot access array element on null");
            }

            if (!targetObj.getClass().isArray()) {
                logger.error("目标不是数组类型: " + targetObj.getClass().getName());
                throw new RuntimeException("Target is not an array: " + targetObj.getClass().getName());
            }

            if (!(indexObj instanceof Number)) {
                logger.error("数组索引必须是数字类型: " + indexObj.getClass().getName());
                throw new RuntimeException("Array index must be a number: " + indexObj.getClass().getName());
            }

            int idx = ((Number) indexObj).intValue();
            int length = Array.getLength(targetObj);

            if (idx < 0 || idx >= length) {
                logger.error("数组索引越界: " + idx + ", 数组长度: " + length);
                throw new ArrayIndexOutOfBoundsException(
                        "Array index out of bounds: " + idx + ", array length: " + length);
            }

            return Array.get(targetObj, idx);
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            Object targetObj = target.evaluate(context);
            if (targetObj == null) {
                logger.error("无法解析null数组的元素类型");
                throw new NullPointerException("Cannot determine element type of null array");
            }

            if (!targetObj.getClass().isArray()) {
                logger.error("目标不是数组类型: " + targetObj.getClass().getName());
                throw new RuntimeException("Target is not an array: " + targetObj.getClass().getName());
            }

            return targetObj.getClass().getComponentType();
        }
    }

    public static class VariableAssignmentNode extends ASTNode {
        private final String variableName;
        private final ASTNode value;

        public VariableAssignmentNode(String variableName, ASTNode value) {
            this.variableName = variableName;
            this.value = value;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if (context.builtIns.containsKey(variableName)) {
                logger.error("变量" + variableName + "是内置变量，无法赋值");
                throw new RuntimeException("Shadowing built-in: " + variableName);
            }
            if (context.hasClass(variableName)) {
                logger.error("变量" + variableName + "是类名，无法赋值");
                throw new RuntimeException("Variable name conflicts with class name: " + variableName);
            }

            Object val = value.evaluate(context);
            if (!context.hasVariable(variableName)) {
                logger.error("变量" + variableName + "未声明");
                throw new RuntimeException("Variable not declared: " + variableName);
            }
            context.setVariable(variableName, val);
            return val;
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            return value.getType(context);
        }
    }

    /**
     * 字段赋值节点，用于处理自定义类实例的字段赋值
     */
    public static class FieldAssignmentNode extends ASTNode {
        private final ASTNode target;
        private final String fieldName;
        private final ASTNode value;

        public FieldAssignmentNode(ASTNode target, String fieldName, ASTNode value) {
            this.target = target;
            this.fieldName = fieldName;
            this.value = value;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object targetObj = target.evaluate(context);
            Object val = value.evaluate(context);

            if (targetObj instanceof CustomClassInstance customInstance) {
                ClassDefinition classDef = customInstance.getClassDefinition();
                FieldDefinition fieldDef = findFieldInHierarchy(classDef, fieldName, context);
                if (fieldDef == null) {
                    throw new RuntimeException(
                            "Field '" + fieldName + "' not found in class hierarchy " + classDef.getClassName());
                }
                customInstance.setField(fieldName, val);
            } else {
                Field field = targetObj.getClass().getField(fieldName);
                field.set(targetObj, val);
            }
            return val;
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            return value.getType(context);
        }

        /**
         * 在类的继承层次结构中递归查找字段
         */
        private FieldDefinition findFieldInHierarchy(ClassDefinition classDef, String fieldName,
                ExecutionContext context) {
            // 首先在当前类中查找字段
            FieldDefinition fieldDef = classDef.getField(fieldName);
            if (fieldDef != null) {
                return fieldDef;
            }

            // 如果当前类没有找到，检查父类
            String superClassName = classDef.getSuperClassName();
            if (superClassName != null) {
                ClassDefinition superClassDef = context.customClasses.get(superClassName);
                if (superClassDef != null) {
                    return findFieldInHierarchy(superClassDef, fieldName, context);
                }
            }

            // 如果没有找到，返回null
            return null;
        }
    }

    public static class ArrayAssignmentNode extends ASTNode {
        private final ASTNode target;
        private final ASTNode index;
        private final ASTNode value;

        public ArrayAssignmentNode(ASTNode target, ASTNode index, ASTNode value) {
            this.target = target;
            this.index = index;
            this.value = value;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object targetObj = target.evaluate(context);
            Object indexObj = index.evaluate(context);
            Object val = value.evaluate(context);

            if (targetObj == null) {
                logger.error("尝试在null数组上赋值");
                throw new NullPointerException("Cannot assign to null array");
            }

            if (!targetObj.getClass().isArray()) {
                logger.error("目标不是数组类型: " + targetObj.getClass().getName());
                throw new RuntimeException("Target is not an array: " + targetObj.getClass().getName());
            }

            if (!(indexObj instanceof Number)) {
                logger.error("数组索引必须是数字类型: " + indexObj.getClass().getName());
                throw new RuntimeException("Array index must be a number: " + indexObj.getClass().getName());
            }

            int idx = ((Number) indexObj).intValue();
            int length = Array.getLength(targetObj);

            if (idx < 0 || idx >= length) {
                logger.error("数组索引越界: " + idx + ", 数组长度: " + length);
                throw new ArrayIndexOutOfBoundsException(
                        "Array index out of bounds: " + idx + ", array length: " + length);
            }

            Array.set(targetObj, idx, val);
            return val;
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            return value.getType(context);
        }
    }

    public static class ClassDeclarationNode extends ASTNode {
        private final String className;
        private final ClassDefinition classDef;

        public ClassDeclarationNode(String className) {
            this.className = className;
            this.classDef = new ClassDefinition(className);
        }

        public ClassDeclarationNode(String className, ClassDefinition classDef) {
            this.className = className;
            this.classDef = classDef;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if (context.builtIns.containsKey(className)) {
                logger.error("类名" + className + "是内置成员，无法声明");
                throw new RuntimeException("Class name shadowing built-in: " + className);
            } else if (context.customClasses.containsKey(className)) {
                logger.error("类名" + className + "已声明，无法重复声明");
                throw new RuntimeException("Class already declared: " + className);
            } else if (context.hasVariable(className)) {
                logger.error("变量" + className + "已声明，无法重复声明");
                throw new RuntimeException("Variable already declared: " + className);
            } else if (context.hasClass(className)) {
                logger.error("类名" + className + "已声明，无法重复声明");
                throw new RuntimeException("Class already declared: " + className);
            } else if (isKeyword(className)) {
                logger.error(className + "是关键字，不能作为类名");
                throw new RuntimeException("Cannot use a keyword as class name: " + className);
            }
            context.customClasses.put(className, classDef);
            return null;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class VariableDeclarationNode extends ASTNode {
        private final String typeName;
        private final String variableName;
        private final ASTNode initialValue;

        public VariableDeclarationNode(String typeName, String variableName, ASTNode initialValue) {
            this.typeName = typeName;
            this.variableName = variableName;
            this.initialValue = initialValue;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if (context.builtIns.containsKey(variableName)) {
                logger.error("变量" + variableName + "是内置变量，无法声明");
                throw new RuntimeException("Variable name shadowing built-in: " + variableName);
            } else if (context.customClasses.containsKey(variableName)) {
                logger.error("变量" + variableName + "是自定义类名，无法声明");
                throw new RuntimeException("Variable name conflicts with defined class name: " + variableName);
            } else if (context.hasVariable(variableName)) {
                logger.error("变量" + variableName + "已声明，无法重复声明");
                throw new RuntimeException("Variable already declared: " + variableName);
            } else if (context.hasClass(variableName)) {
                logger.error("变量" + variableName + "是类名，无法声明");
                throw new RuntimeException("Variable name conflicts with class name: " + variableName);
            } else if (isKeyword(variableName)) {
                logger.error(variableName + "是关键字，不能作为变量名");
                throw new RuntimeException("Cannot use a keyword as variable name: " + variableName);
            }
            Object value = null;
            Class<?> clazz;
            if (initialValue != null) {
                if (initialValue instanceof LambdaNode lambdaNode) {
                    // 检查变量声明的类型
                    if (!typeName.equals("auto")) {
                        if (context.hasClass(typeName)) {
                            LambdaNode newLambdaNode = new LambdaNode(
                                        lambdaNode.parameters,
                                        lambdaNode.body,
                                        typeName);
                            value = newLambdaNode.evaluate(context);
                        } else {
                            value = initialValue.evaluate(context);
                        }
                    } else {
                        // 提供了auto类型，当作一般值解析
                        value = initialValue.evaluate(context);
                    }
                } else {
                    // 不是lambda表达式，正常解析
                    value = initialValue.evaluate(context);
                }
            }
            if (typeName.equals("auto")) {
                if (value == null) {
                    logger.error("变量" + variableName + "未指定类型且没有初始值");
                    throw new RuntimeException("Variable declaration without type and initial value: " + variableName);
                }
                clazz = value.getClass();
            } else {
                try {
                    clazz = context.findClass(typeName);
                } catch (CustomClassException e) {
                    // 对于自定义类，跳过类型检查，因为自定义类没有对应的Java类
                    clazz = Object.class;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Cannot resolve type for variable declaration: " + typeName);
                }
            }

            try {
                if (!clazz.equals(Object.class)) {
                    value = context.castObject(value, clazz);
                }
            } catch (ClassCastException e) {
                String s = value == null ? "null" : value.getClass().getName();
                throw new RuntimeException("Initial value type mismatch (declared " + clazz.getName() +
                        ", got " + s + ") for variable declaration: " + variableName);
            }

            context.setVariable(variableName, value);
            return value;
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            if (typeName.equals("auto")) {
                return Object.class;
            }
            return context.findClass(getArrayTypeNameWithoutLength(typeName));
        }
    }

    public static class BlockNode extends ASTNode {
        private final List<ASTNode> statements = new ArrayList<>();

        public void addStatement(ASTNode statement) {
            statements.add(statement);
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object lastResult = null;
            for (int i = 0; i < statements.size(); i++) {
                ASTNode statement = statements.get(i);
                lastResult = statement.evaluate(context);
                if (context.shouldReturn) {
                    return context.returnValue;
                }
                if (context.shouldBreak || context.shouldContinue) {
                    break;
                }
            }
            return lastResult;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class IfNode extends ASTNode {
        private final ASTNode condition;
        private final ASTNode thenBranch;
        private final ASTNode elseBranch;

        public IfNode(ASTNode condition, ASTNode thenBranch, ASTNode elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            context.recordScope();

            Object condValue = condition.evaluate(context);
            boolean boolValue = toBoolean(condValue);

            if (boolValue) {
                Object result = thenBranch != null ? thenBranch.evaluate(context) : null;
                if (context.shouldReturn) {
                    Object returnValue = context.returnValue;
                    context.restoreScope();
                    return returnValue;
                }
                if (context.shouldBreak || context.shouldContinue) {
                    context.restoreScope();
                    return null;
                }
                context.restoreScope();
                return result;
            } else if (elseBranch != null) {
                Object result = elseBranch.evaluate(context);
                if (context.shouldReturn) {
                    Object returnValue = context.returnValue;
                    context.restoreScope();
                    return returnValue;
                }
                if (context.shouldBreak || context.shouldContinue) {
                    context.restoreScope();
                    return null;
                }
                context.restoreScope();
                return result;
            }
            context.restoreScope();
            return null;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class WhileNode extends ASTNode {

        private final ASTNode condition;
        private final ASTNode body;
        public static int MAX_LOOPS = 1024;

        public WhileNode(ASTNode condition, ASTNode body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object lastResult = null;
            int loopCount = 0;

            // 保存控制流状态
            boolean originalBreak = context.shouldBreak;
            boolean originalContinue = context.shouldContinue;
            boolean originalReturn = context.shouldReturn;

            try {
                context.shouldBreak = false;
                context.shouldContinue = false;
                context.shouldReturn = false;
                context.recordScope();

                while (loopCount < MAX_LOOPS) {

                    // 评估条件（每次循环都要重新评估）
                    if (!toBoolean(condition.evaluate(context)))
                        break;

                    // 执行循环体
                    lastResult = body.evaluate(context);

                    if (context.shouldBreak) {
                        context.shouldBreak = false;
                        break;
                    } else if (context.shouldContinue) {
                        context.shouldContinue = false; // continue应该已经被处理了，所以这里不需要重置
                    } else if (context.shouldReturn) {
                        return context.returnValue; // 如果遇到return语句，直接返回实际的返回值并退出循环
                    }
                    loopCount++;

                    if (loopCount >= MAX_LOOPS) {
                        logger.warn("警告: while循环达到最大限制 (" + MAX_LOOPS + ")，自动退出");
                        context.printWarn("While loop reached its limit(" + MAX_LOOPS + "), force quited" + "\n");
                        break;
                    }
                }
                return lastResult;
            } finally {
                // 恢复控制流状态
                context.shouldBreak = originalBreak;
                context.shouldContinue = originalContinue;
                context.restoreScope();
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class ForNode extends ASTNode {
        private final ASTNode init;
        private final ASTNode condition;
        private final ASTNode update;
        private final ASTNode body;
        public static int MAX_LOOPS = 1024;

        public ForNode(ASTNode init, ASTNode condition, ASTNode update, ASTNode body) {
            this.init = init;
            this.condition = condition;
            this.update = update;
            this.body = body;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object lastResult = null;
            int loopCount = 0;

            boolean origBreak = context.shouldBreak;
            boolean origContinue = context.shouldContinue;
            context.recordScope();

            context.shouldBreak = false;
            context.shouldContinue = false;

            try {
                if (init != null) {
                    init.evaluate(context);
                }

                while (loopCount < MAX_LOOPS) {
                    if (condition != null && !toBoolean(condition.evaluate(context))) {
                        break;
                    }

                    lastResult = body.evaluate(context);

                    if (context.shouldBreak) {
                        context.shouldBreak = false;
                        break;
                    }

                    if (context.shouldReturn) {
                        return context.returnValue;
                    }

                    if (update != null) {
                        update.evaluate(context);
                    }

                    if (context.shouldContinue) {
                        context.shouldContinue = false;
                    }

                    loopCount++;
                }

                if (loopCount >= MAX_LOOPS) {
                    logger.warn("For循环达到最大限制 (" + MAX_LOOPS + ")，可能陷入无限循环，已经强制退出");
                    context.printWarn("For loop reached its limit (" + MAX_LOOPS + "), force quited" + "\n");
                }
                return lastResult;

            } finally {
                context.shouldBreak = origBreak;
                context.shouldContinue = origContinue;
                context.restoreScope();
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class ForEachNode extends ASTNode {
        private final String className;
        private final String itemName;
        private final ASTNode collection;
        private final ASTNode body;
        public static int MAX_LOOPS = 1024;

        public ForEachNode(String className, String itemName, ASTNode collection, ASTNode body) {
            this.className = className;
            this.itemName = itemName;
            this.collection = collection;
            this.body = body;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object coll = collection.evaluate(context);
            Object lastResult = null;
            int loopCount = 0;

            boolean origBreak = context.shouldBreak;
            boolean origContinue = context.shouldContinue;

            context.shouldBreak = false;
            context.shouldContinue = false;

            try {
                context.recordScope();
                Class<?> clazz = context.findClass(className);
                
                if (coll == null) {
                    logger.error("for-each 的集合为 null");
                    throw new NullPointerException("Cannot iterate over null collection");
                }
                
                if (coll instanceof Object[]) {
                    for (Object item : (Object[]) coll) {
                        if (loopCount >= MAX_LOOPS)
                            break;

                        context.castObject(item, clazz);

                        context.setVariable(itemName, item);

                        lastResult = body.evaluate(context);

                        if (context.shouldBreak) {
                            context.shouldBreak = false;
                            break;
                        }

                        if (context.shouldContinue) {
                            context.shouldContinue = false;
                        }

                        if (context.shouldReturn) {
                            return context.returnValue;
                        }

                        loopCount++;
                    }
                } else if (coll instanceof Iterable) {
                    for (Object item : (Iterable<?>) coll) {
                        if (loopCount >= MAX_LOOPS)
                            break;

                        context.castObject(item, clazz);

                        context.setVariable(itemName, item);

                        lastResult = body.evaluate(context);

                        if (context.shouldBreak) {
                            context.shouldBreak = false;
                            break;
                        }

                        if (context.shouldContinue) {
                            context.shouldContinue = false;
                        }

                        if (context.shouldReturn) {
                            return context.returnValue;
                        }

                        loopCount++;
                    }
                } else if (coll instanceof String) {
                    for (char ch : ((String) coll).toCharArray()) {
                        if (loopCount >= MAX_LOOPS)
                            break;

                        context.setVariable(itemName, ch);
                        lastResult = body.evaluate(context);

                        if (context.shouldBreak) {
                            context.shouldBreak = false;
                            break;
                        }

                        if (context.shouldContinue) {
                            context.shouldContinue = false;
                        }

                        if (context.shouldReturn) {
                            return context.returnValue;
                        }

                        loopCount++;
                    }
                } else {
                    logger.error("无法在类型" + coll.getClass().getName() + "上迭代");
                    throw new RuntimeException("Cannot iterate over: " +
                            coll.getClass().getName());
                }

                if (loopCount >= MAX_LOOPS) {
                    logger.warn("For-each循环达到最大限制 (" + MAX_LOOPS + ")");
                    context.printWarn("For-each loop reached its limit (" + MAX_LOOPS + "), force quited" + "\n");
                }

                return lastResult;
            } finally {
                context.shouldBreak = origBreak;
                context.shouldContinue = origContinue;
                context.restoreScope();
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class DoWhileNode extends ASTNode {
        private final ASTNode condition;
        private final ASTNode body;
        public static int MAX_LOOPS = 1024;

        public DoWhileNode(ASTNode condition, ASTNode body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object lastResult = null;
            int loopCount = 0;

            boolean originalBreak = context.shouldBreak;
            boolean originalContinue = context.shouldContinue;
            boolean originalReturn = context.shouldReturn;

            try {
                context.shouldBreak = false;
                context.shouldContinue = false;
                context.shouldReturn = false;

                do {
                    if (loopCount >= MAX_LOOPS) {
                        logger.warn("警告: do-while循环达到最大限制 (" + MAX_LOOPS + ")，自动退出");
                        context.printWarn("Do-while loop reached its limit(" + MAX_LOOPS + "), force quited" + "\n");
                        break;
                    }

                    lastResult = body.evaluate(context);

                    if (context.shouldBreak) {
                        context.shouldBreak = false;
                        break;
                    } else if (context.shouldContinue) {
                        context.shouldContinue = false;
                    } else if (context.shouldReturn) {
                        return context.returnValue;
                    }
                    loopCount++;
                } while (toBoolean(condition.evaluate(context)));

                return lastResult;
            } finally {
                context.shouldBreak = originalBreak;
                context.shouldContinue = originalContinue;
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class SwitchNode extends ASTNode {
        private final ASTNode expression;
        private final List<CaseNode> cases;
        private final ASTNode defaultCase;

        public SwitchNode(ASTNode expression, List<CaseNode> cases, ASTNode defaultCase) {
            this.expression = expression;
            this.cases = cases;
            this.defaultCase = defaultCase;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object value = expression.evaluate(context);
            boolean matched = false;

            for (CaseNode caseNode : cases) {
                if (matched || isValueEqual(value, caseNode.getValue().evaluate(context))) {
                    matched = true;
                    Object result = caseNode.getBody().evaluate(context);
                    if (context.shouldBreak) {
                        context.shouldBreak = false;
                        break;
                    }
                    if (context.shouldReturn) {
                        return context.returnValue;
                    }
                }
            }

            if (!matched && defaultCase != null) {
                Object result = defaultCase.evaluate(context);
                if (context.shouldReturn) {
                    return context.returnValue;
                }
            }

            return null;
        }

        private boolean isValueEqual(Object value1, Object value2) {
            if (value1 == null && value2 == null) {
                return true;
            }
            if (value1 == null || value2 == null) {
                return false;
            }

            if (value1 instanceof Number n1 && value2 instanceof Number n2) {
                return n1.doubleValue() == n2.doubleValue();
            }

            return value1.equals(value2);
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class CaseNode extends ASTNode {
        private final ASTNode value;
        private final ASTNode body;

        public CaseNode(ASTNode value, ASTNode body) {
            this.value = value;
            this.body = body;
        }

        public ASTNode getValue() {
            return value;
        }

        public ASTNode getBody() {
            return body;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            return body.evaluate(context);
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class ControlNode extends ASTNode {
        private final String type; // "break", "continue" 或者 "return"
        private final ASTNode value; // 如果是return类型包含返回值

        public ControlNode(String type, ASTNode value) {
            this.type = type;
            this.value = value;
        }

        public ControlNode(String type) {
            this.type = type;
            this.value = null;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if ("break".equals(type)) {
                context.shouldBreak = true;
                return null;
            } else if ("continue".equals(type)) {
                context.shouldContinue = true;
                return null;
            } else if ("return".equals(type)) {
                context.shouldReturn = true;
                if (value != null) {
                    Object returnValue = value.evaluate(context);
                    context.returnValue = returnValue;
                    String expectedReturnType = context.getCurrentMethodReturnType();
                    if (expectedReturnType != null && !isReturnTypeMatch(expectedReturnType, returnValue, context)) {
                        String actualType = returnValue != null ? returnValue.getClass().getSimpleName() : "null";
                        logger.error("返回值类型不匹配: 期望 '" + expectedReturnType + "', 实际 '" + actualType + "'");
                        throw new RuntimeException(
                                "Return value type mismatch: expected " + expectedReturnType + ", got " + actualType);
                    }

                    return returnValue;
                } else {
                    context.returnValue = null;

                    String expectedReturnType = context.getCurrentMethodReturnType();
                    if (expectedReturnType != null && !"void".equals(expectedReturnType)) {
                        throw new RuntimeException(
                                "Return value type mismatch: expected " + expectedReturnType + ", got void");
                    }

                    return null;
                }
            } else {
                logger.error("未知的控制类型: " + type);
                throw new RuntimeException("Unknown control type: " + type);
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class TryCatchNode extends ASTNode {
        private final ASTNode tryBlock;
        private final List<CatchBlock> catchBlocks;
        private final ASTNode finallyBlock;

        public static class CatchBlock {
            private final String exceptionType;
            private final String exceptionName;
            private final ASTNode catchBlock;

            public CatchBlock(String exceptionType, String exceptionName, ASTNode catchBlock) {
                this.exceptionType = exceptionType;
                this.exceptionName = exceptionName;
                this.catchBlock = catchBlock;
            }

            public String getExceptionType() {
                return exceptionType;
            }

            public String getExceptionName() {
                return exceptionName;
            }

            public ASTNode getCatchBlock() {
                return catchBlock;
            }
        }

        public TryCatchNode(ASTNode tryBlock, List<CatchBlock> catchBlocks, ASTNode finallyBlock) {
            this.tryBlock = tryBlock;
            this.catchBlocks = catchBlocks;
            this.finallyBlock = finallyBlock;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object result = null;
            Throwable caughtException = null;
            boolean shouldExecuteFinally = true;
            boolean matchedCatch = false;

            context.recordScope();

            try {
                result = tryBlock.evaluate(context);

                if (context.shouldReturn) {
                    Object returnValue = context.returnValue;
                    if (finallyBlock != null) {
                        context.shouldReturn = false;
                        finallyBlock.evaluate(context);
                        context.shouldReturn = true;
                    }
                    context.restoreScope();
                    return returnValue;
                }

                if (context.shouldBreak || context.shouldContinue) {
                    if (finallyBlock != null) {
                        boolean originalShouldBreak = context.shouldBreak;
                        boolean originalShouldContinue = context.shouldContinue;
                        context.shouldBreak = false;
                        context.shouldContinue = false;
                        finallyBlock.evaluate(context);
                        context.shouldBreak = originalShouldBreak;
                        context.shouldContinue = originalShouldContinue;
                    }
                    context.restoreScope();
                    return null;
                }
            } catch (Throwable e) {
                caughtException = e;

                for (CatchBlock catchBlock : catchBlocks) {
                    try {
                        Class<?> exceptionClass = context.findClass(catchBlock.getExceptionType());
                        if (exceptionClass != null && exceptionClass.isAssignableFrom(e.getClass())) {
                            matchedCatch = true;

                            context.restoreScope();
                            context.recordScope();

                            context.setVariable(catchBlock.getExceptionName(), e);

                            Object catchResult = catchBlock.getCatchBlock().evaluate(context);

                            if (context.shouldReturn) {
                                Object returnValue = context.returnValue;
                                if (finallyBlock != null) {
                                    context.shouldReturn = false;
                                    finallyBlock.evaluate(context);
                                    context.shouldReturn = true;
                                }
                                context.restoreScope();
                                return returnValue;
                            }

                            if (context.shouldBreak || context.shouldContinue) {
                                if (finallyBlock != null) {
                                    boolean originalShouldBreak = context.shouldBreak;
                                    boolean originalShouldContinue = context.shouldContinue;
                                    context.shouldBreak = false;
                                    context.shouldContinue = false;
                                    finallyBlock.evaluate(context);
                                    context.shouldBreak = originalShouldBreak;
                                    context.shouldContinue = originalShouldContinue;
                                }
                                context.restoreScope();
                                return null;
                            }

                            result = catchResult;
                            break;
                        }
                    } catch (ClassNotFoundException ex) {
                        logger.error("无法找到异常类: " + catchBlock.getExceptionType());
                        throw new RuntimeException("Exception class not found: " + catchBlock.getExceptionType(), ex);
                    }
                }

                if (!matchedCatch) {
                    shouldExecuteFinally = false;
                }
            }

            if (finallyBlock != null) {
                if (matchedCatch) {
                    context.restoreScope();
                    context.recordScope();
                }

                finallyBlock.evaluate(context);

                if (context.shouldReturn) {
                    Object returnValue = context.returnValue;
                    context.restoreScope();

                    // 如果没有匹配的 catch 块，需要重新抛出异常
                    if (!matchedCatch && caughtException != null) {
                        if (caughtException instanceof Exception) {
                            throw (Exception) caughtException;
                        } else if (caughtException instanceof Error) {
                            throw (Error) caughtException;
                        } else {
                            throw new RuntimeException(caughtException);
                        }
                    }

                    return returnValue;
                }

                if (context.shouldBreak || context.shouldContinue) {
                    context.restoreScope();

                    // 如果没有匹配的 catch 块，需要重新抛出异常
                    if (!matchedCatch && caughtException != null) {
                        if (caughtException instanceof Exception) {
                            throw (Exception) caughtException;
                        } else if (caughtException instanceof Error) {
                            throw (Error) caughtException;
                        } else {
                            throw new RuntimeException(caughtException);
                        }
                    }

                    return null;
                }
            }

            context.restoreScope();

            if (!matchedCatch && caughtException != null) {
                if (caughtException instanceof Exception) {
                    throw (Exception) caughtException;
                } else if (caughtException instanceof Error) {
                    throw (Error) caughtException;
                } else {
                    throw new RuntimeException(caughtException);
                }
            }

            return result;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class ThrowNode extends ASTNode {
        private final ASTNode exception;

        public ThrowNode(ASTNode exception) {
            this.exception = exception;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object exceptionObj = exception.evaluate(context);

            if (exceptionObj == null) {
                logger.error("throw 语句不能抛出 null");
                throw new RuntimeException("Cannot throw null");
            }

            if (exceptionObj instanceof Throwable) {
                Throwable t = (Throwable) exceptionObj;
                if (t instanceof Exception) {
                    throw (Exception) t;
                } else if (t instanceof Error) {
                    throw (Error) t;
                } else {
                    throw new RuntimeException(t);
                }
            } else {
                logger.error("throw 语句只能抛出 Throwable 类型，实际类型: " + exceptionObj.getClass().getName());
                throw new RuntimeException("Can only throw Throwable, got: " + exceptionObj.getClass().getName());
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            return Void.class;
        }
    }

    public static class IncrementNode extends ASTNode {
        private final String variableName;
        private final boolean isPre; // 是前置还是后置
        private final boolean isIncrement; // 是递增还是递减

        public IncrementNode(String variableName, boolean isPre, boolean isIncrement) {
            this.variableName = variableName;
            this.isPre = isPre;
            this.isIncrement = isIncrement;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws RuntimeException {
            Variable var = context.getVariable(variableName);
            if (var == null) {
                logger.error("尝试在一个未定义的变量" + variableName + "上使用自加或者自减运算");
                throw new RuntimeException("Undefined variable: " + variableName);
            }

            if (!(var.value instanceof Number oldValue)) {
                logger.error("变量" + variableName + "的类型是" + var.getClass().getName() + "，不能进行自加或者自减运算");
                throw new RuntimeException("Cannot increment/decrement non-numeric variable: " + variableName);
            }

            Number newValue;

            if (var.value instanceof Integer) {
                int val = oldValue.intValue();
                newValue = isIncrement ? val + 1 : val - 1;
            } else if (var.value instanceof Long) {
                long val = oldValue.longValue();
                newValue = isIncrement ? val + 1L : val - 1L;
            } else if (var.value instanceof Float) {
                float val = oldValue.floatValue();
                newValue = isIncrement ? val + 1.0f : val - 1.0f;
            } else if (var.value instanceof Double) {
                double val = oldValue.doubleValue();
                newValue = isIncrement ? val + 1.0 : val - 1.0;
            } else {
                logger.error("不支持的数字类型: " + var.value.getClass().getName());
                throw new RuntimeException("Unsupported numeric type: " + var.value.getClass());
            }

            context.setVariable(variableName, newValue);
            return isPre ? newValue : oldValue;
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            Variable var = context.getVariable(variableName);
            if (var == null) {
                logger.error("尝试在一个未定义的变量" + variableName + "上使用自加或者自减运算");
                throw new RuntimeException("Undefined variable: " + variableName);
            }
            return var.value.getClass();
        }
    }

    public static class FieldIncrementNode extends ASTNode {
        private final ASTNode target;
        private final String fieldName;
        private final boolean isPre; // 是前置还是后置
        private final boolean isIncrement; // 是递增还是递减

        public FieldIncrementNode(ASTNode target, String fieldName, boolean isPre, boolean isIncrement) {
            this.target = target;
            this.fieldName = fieldName;
            this.isPre = isPre;
            this.isIncrement = isIncrement;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Object targetObj = target != null ? target.evaluate(context) : null;

            if (targetObj == null) {
                logger.error("尝试在null对象上访问字段" + fieldName + "进行自增/自减操作");
                throw new NullPointerException("Cannot access field " + fieldName + " on null object");
            }

            // 处理自定义类实例的字段访问
            if (targetObj instanceof CustomClassInstance customInstance) {
                ClassDefinition classDef = customInstance.getClassDefinition();

                // 在类的继承层次结构中查找字段
                FieldDefinition fieldDef = findFieldInHierarchy(classDef, fieldName, context);
                if (fieldDef != null) {
                    Object oldValue = customInstance.getField(fieldDef.fieldName);

                    if (!(oldValue instanceof Number)) {
                        logger.error("字段" + fieldName + "的类型是" + oldValue.getClass().getName() + "，不能进行自加或者自减运算");
                        throw new RuntimeException("Cannot increment/decrement non-numeric field: " + fieldName);
                    }

                    Number newValue;
                    if (oldValue instanceof Integer) {
                        int val = ((Number) oldValue).intValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Long) {
                        long val = ((Number) oldValue).longValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Float) {
                        float val = ((Number) oldValue).floatValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Double) {
                        double val = ((Number) oldValue).doubleValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Byte) {
                        byte val = ((Number) oldValue).byteValue();
                        newValue = isIncrement ? (byte) (val + 1) : (byte) (val - 1);
                    } else if (oldValue instanceof Short) {
                        short val = ((Number) oldValue).shortValue();
                        newValue = isIncrement ? (short) (val + 1) : (short) (val - 1);
                    } else {
                        logger.error("不支持的数值类型: " + oldValue.getClass().getName());
                        throw new RuntimeException("Unsupported numeric type: " + oldValue.getClass().getName());
                    }

                    customInstance.setField(fieldDef.fieldName, newValue);
                    return isPre ? newValue : oldValue;
                } else {
                    logger.error("字段" + fieldName + "在类" + classDef.className + "中未定义");
                    throw new RuntimeException("Field " + fieldName + " not found in class " + classDef.className);
                }
            } else {
                // 处理普通Java对象的字段访问
                try {
                    Field field = targetObj.getClass().getField(fieldName);
                    Object oldValue = field.get(targetObj);

                    if (!(oldValue instanceof Number)) {
                        logger.error("字段" + fieldName + "的类型是" +
                                (oldValue == null ? "null" : oldValue.getClass().getName()) + "，不能进行自加或者自减运算");
                        throw new RuntimeException("Cannot increment/decrement non-numeric field: " + fieldName);
                    }

                    Number newValue;
                    if (oldValue instanceof Integer) {
                        int val = ((Number) oldValue).intValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Long) {
                        long val = ((Number) oldValue).longValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Float) {
                        float val = ((Number) oldValue).floatValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Double) {
                        double val = ((Number) oldValue).doubleValue();
                        newValue = isIncrement ? val + 1 : val - 1;
                    } else if (oldValue instanceof Byte) {
                        byte val = ((Number) oldValue).byteValue();
                        newValue = isIncrement ? (byte) (val + 1) : (byte) (val - 1);
                    } else if (oldValue instanceof Short) {
                        short val = ((Number) oldValue).shortValue();
                        newValue = isIncrement ? (short) (val + 1) : (short) (val - 1);
                    } else {
                        logger.error("不支持的数值类型: " + oldValue.getClass().getName());
                        throw new RuntimeException("Unsupported numeric type: " + oldValue.getClass().getName());
                    }

                    field.set(targetObj, newValue);
                    return isPre ? newValue : oldValue;
                } catch (NoSuchFieldException e) {
                    logger.error("字段" + fieldName + "在对象" + targetObj.getClass().getName() + "中未找到");
                    throw new RuntimeException("Field " + fieldName + " not found in " + targetObj.getClass().getName(),
                            e);
                } catch (IllegalAccessException e) {
                    logger.error("无法访问字段" + fieldName + "：" + e.getMessage());
                    throw new RuntimeException("Cannot access field " + fieldName, e);
                }
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            Object targetObj = target != null ? target.evaluate(context) : null;

            if (targetObj == null) {
                throw new NullPointerException("Cannot determine type of field " + fieldName + " on null object");
            }

            if (targetObj instanceof CustomClassInstance customInstance) {
                ClassDefinition classDef = customInstance.getClassDefinition();
                FieldDefinition fieldDef = findFieldInHierarchy(classDef, fieldName, context);
                if (fieldDef != null) {
                    return context.findClass(fieldDef.typeName);
                } else {
                    throw new RuntimeException("Field " + fieldName + " not found in class " + classDef.className);
                }
            } else {
                try {
                    Field field = targetObj.getClass().getField(fieldName);
                    return field.getType();
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Field " + fieldName + " not found in " + targetObj.getClass().getName(),
                            e);
                }
            }
        }
    }

    public static class TernaryExprNode extends ASTNode {

        private final ASTNode condition;
        private final ASTNode thenExpr;
        private final ASTNode elseExpr;

        public TernaryExprNode(ASTNode condition, ASTNode thenExpr, ASTNode elseExpr) {
            this.condition = condition;
            this.thenExpr = thenExpr;
            this.elseExpr = elseExpr;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            if (!isApplicableArgs(new Class[] { thenExpr.getType(context) },
                    new Class[] { elseExpr.getType(context) })) {
                logger.error(
                        "三元表达式中的两个表达式类型不匹配，类型分别为：" + thenExpr.getType(context) + " 和 " + elseExpr.getType(context));
                throw new RuntimeException("Types of two expressions in ternary expression do not match: "
                        + thenExpr.getType(context) + " and " + elseExpr.getType(context));
            }
            if (toBoolean(condition.evaluate(context))) {
                return thenExpr.evaluate(context);
            } else {
                return elseExpr.evaluate(context);
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            return thenExpr.getType(context);
        }
    }

    public static class ParenthesizedExpressionNode extends ASTNode {
        private final ASTNode expression;

        public ParenthesizedExpressionNode(ASTNode expression) {
            this.expression = expression;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            return expression.evaluate(context);
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            return expression.getType(context);
        }

        @NonNull
        @Override
        public String toString() {
            return "(" + expression + ")";
        }
    }

    public static class LambdaNode extends ASTNode {
        private final List<String> parameters;
        private final ASTNode body;
        private final String functionalInterfaceName; // 函数式接口名

        public LambdaNode(List<String> parameters, ASTNode body, String functionalInterfaceName) {
            this.parameters = parameters;
            this.body = body;
            this.functionalInterfaceName = functionalInterfaceName;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            Lambda lambda = createLambda(context);

            if (functionalInterfaceName == null || functionalInterfaceName.isEmpty()
                    || functionalInterfaceName.equals("Lambda")) {
                return lambda;
            }

            Class<?> functionalInterfaceClass = context.findClass(functionalInterfaceName);
            return toFunctionalInterface(lambda, functionalInterfaceClass);

        }

        private Object toFunctionalInterface(Lambda lambda, Class<?> functionalInterfaceClass) throws Exception {
            // 对于常见的函数式接口，提供直接转换
            if (functionalInterfaceClass == Supplier.class) {
                checkParameterCount(lambda, 0, Supplier.class.getName(), "get");
                return (Supplier<Object>) () -> lambda.call();
            } else if (functionalInterfaceClass == Function.class) {
                checkParameterCount(lambda, 1, Function.class.getName(), "apply");
                return (Function<Object, Object>) (arg) -> lambda.call(arg);
            } else if (functionalInterfaceClass == Consumer.class) {
                checkParameterCount(lambda, 1, Consumer.class.getName(), "accept");
                return (Consumer<Object>) (arg) -> lambda.call(arg);
            } else if (functionalInterfaceClass == Predicate.class) {
                checkParameterCount(lambda, 1, Predicate.class.getName(), "test");
                return (Predicate<Object>) (arg) -> (Boolean) lambda.call(arg);
            }
            
            // 对于其他函数式接口用Proxy
            Method[] methods = functionalInterfaceClass.getMethods();
            Method functionalMethod = null;
            
            for (Method method : methods) {
                if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (functionalMethod == null) {
                    functionalMethod = method;
                } else {
                    throw new IllegalArgumentException("不是函数式接口: " + functionalInterfaceClass.getName());
                }
            }
            
            if (functionalMethod == null) {
                throw new IllegalArgumentException("没有找到抽象方法: " + functionalInterfaceClass.getName());
            }

            // 检查参数数量是否匹配
            int expectedParamCount = functionalMethod.getParameterCount();
            checkParameterCount(lambda, expectedParamCount, functionalInterfaceClass.getName(), functionalMethod.getName());

            final Method finalFunctionalMethod = functionalMethod;
            logger.info("当前尝试把Lambda转为" + functionalInterfaceClass.getName() 
                            + "，将会识别" + functionalMethod.getName() + "为函数执行接口");
            
            return Proxy.newProxyInstance(
                functionalInterfaceClass.getClassLoader(),
                new Class<?>[] { functionalInterfaceClass },
                (proxy, method, args) -> {
                    if (method.equals(finalFunctionalMethod)) {
                        return lambda.call(args);
                    } else if (method.getName().equals("toString")) {
                        return lambda.toString();
                    } else if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    } else if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    } else {
                        throw new UnsupportedOperationException("方法不支持: " + method.getName());
                    }
                }
            );
        }

        private void checkParameterCount(Lambda lambda, int expectedCount, String interfaceName, String methodName) {
            // 通过LambdaNode获取参数数量
            if (this.parameters.size() != expectedCount) {
                logger.error("Lambda转换失败，表达式需要" + this.parameters.size() 
                        + "个参数，但" + interfaceName + "接口的" + methodName + "方法需要" + expectedCount + "个参数");
                throw new RuntimeException("Cannot cast Lambda, parameter count mismatch (" +
                        "expected " + expectedCount + "(for " + interfaceName + "." + methodName + "), got " + this.parameters.size() + ")"
                );
            }
        }

        private Lambda createLambda(ExecutionContext context) {
            return (args) -> {
                try {
                    context.recordScope();
                    Object result = null;
                    if (args == null) {
                        if (parameters.size() > 0) {
                            throw new RuntimeException(
                                    "Lambda需要 " + parameters.size() + " 个参数，但调用提供了 0 个参数");
                        }
                    } else if (args.length != parameters.size()) {
                        throw new RuntimeException(
                                "Lambda需要 " + parameters.size() + " 个参数，但调用提供了 " + args.length + " 个参数");
                    } else {
                        for (int i = 0; i < parameters.size(); i++) {
                            context.setVariable(parameters.get(i), args[i]);
                        }
                        result = body.evaluate(context);
                        context.restoreScope();
                    }
                    return result;
                } catch (Exception e) {
                    throw new RuntimeException("Lambda execution failed: " + e.getMessage(), e);
                }
            };
        }

        @Override
        public Class<?> getType(ExecutionContext context) {
            if (functionalInterfaceName == null || functionalInterfaceName.isEmpty()
                    || functionalInterfaceName.equals("Lambda")) {
                return Lambda.class;
            }
            try {
                return context.findClass(functionalInterfaceName);
            } catch (Exception e) {
                return Lambda.class;
            }
        }
    }

    public static class DirectFunctionCallNode extends ASTNode {
        private final ASTNode function;
        private final List<ASTNode> arguments;

        public DirectFunctionCallNode(ASTNode function, List<ASTNode> arguments) {
            this.function = function;
            this.arguments = arguments;
        }

        @Override
        public Object evaluate(ExecutionContext context) throws Exception {
            String methodName = ((VariableNode) function).name;

            if (function instanceof VariableNode) {
                if (context.hasVariable("this")) {
                    Object thisValue = context.getVariable("this").value;
                    if (thisValue instanceof CustomClassInstance thisInstance) {
                        ClassDefinition classDef = thisInstance.getClassDefinition();
                        MethodDefinition methodDef = findMethodInHierarchy(classDef, methodName, context);
                        if (methodDef != null) {
                            return new MethodCallNode(new VariableNode("this"), methodName, arguments)
                                    .evaluate(context);
                        }
                    }
                }
            }

            Object funcObj = function.evaluate(context);
            List<Object> args = new ArrayList<>();
            for (ASTNode arg : arguments) {
                args.add(arg.evaluate(context));
            }

            if (funcObj instanceof Lambda) {
                return ((Lambda) funcObj).call(args.toArray());
            } else if (funcObj instanceof Function) {
                return ((Function<Object[], ?>) funcObj).apply(args.toArray());
            } else if (funcObj instanceof Supplier) {
                return ((Supplier<?>) funcObj).get();
            } else if (funcObj instanceof Callable) {
                return ((Callable<?>) funcObj).call();
            } else if (funcObj instanceof Runnable) {
                ((Runnable) funcObj).run();
                return null;
            } else if (funcObj instanceof Method) {
                return ((Method) funcObj).invoke(null, args.toArray());
            } else if (funcObj instanceof Predicate) {
                return ((Predicate<Object>) funcObj).test(args.get(0));
            } else if (funcObj instanceof Consumer) {
                ((Consumer<Object>) funcObj).accept(args.get(0));
                return null;
            } else {
                logger.error("对象" + funcObj + "无法被调用");
                throw new RuntimeException("Object not callable: " + funcObj);
            }
        }

        @Override
        public Class<?> getType(ExecutionContext context) throws Exception {
            Object funcObj = function.evaluate(context);
            if (funcObj instanceof Function) {
                return Object.class;
            } else if (funcObj instanceof Supplier) {
                return Object.class;
            } else if (funcObj instanceof Predicate) {
                return Boolean.class;
            } else if (funcObj instanceof Lambda) {
                return Object.class;
            } else {
                return Void.class;
            }
        }
    }

    public static class DeleteNode extends ASTNode {
        private final String variableName;

        public DeleteNode(String variableName) {
            this.variableName = variableName;
        }

        public Object evaluate(ExecutionContext context) throws Exception {
            if (variableName == "*") {
                for (Map.Entry<String, Variable> entry : context.getAllVariables().entrySet()) {
                    if (!entry.getKey().equals("this")) {
                        Variable val = context.getVariable(entry.getKey());
                        try {
                            if (val.value.getClass().getMethod("finalize") != null) {
                                context.callMethod(val.value, "finalize", new ArrayList<>());
                            }
                        } catch (NoSuchMethodException e) {
                        }
                        context.deleteVariable(entry.getKey());
                    }
                }

            } else {
                if (variableName.equals("this")) {
                    logger.error("不允许删除this引用");
                    throw new RuntimeException("Cannot delete 'this' reference");
                } else if (isKeyword(variableName)) {
                    logger.error("不允许删除关键字: " + variableName);
                    throw new RuntimeException("Cannot delete keyword: " + variableName);
                }

                Variable val = context.getVariable(variableName);
                // 如果有析构函数(finalize)就调用
                try {
                    if (val.value.getClass().getMethod("finalize") != null) {
                        context.callMethod(val.value, "finalize", new ArrayList<>());
                    }
                } catch (NoSuchMethodException e) {
                }
                context.deleteVariable(variableName);
            }
            return null;
        }

        public Class<?> getType(ExecutionContext context) throws Exception {
            return Void.class;
        }
    }

    /**
     * 代码解析器。
     */
    public static class Parser {

        private final String input;
        private int position;
        private final ExecutionContext context;
        private final Stack<Integer> savedPositions;
        private ASTNode lastUncompleted = null;

        public Parser(String input, ExecutionContext context) {
            this.input = input.trim();
            this.position = 0;
            this.savedPositions = new Stack<>();
            this.context = context;
        }

        /**
         * 查看当前字符但不移动位置。
         *
         * @return 当前字符，或者如果到达末尾则返回'\0'
         */
        private char peek() {
            return position < input.length() ? input.charAt(position) : '\0';
        }

        /**
         * 保存当前位置，以便以后恢复。
         *
         * @return 保存的位置
         */
        private int savePosition() {
            savedPositions.push(position);
            return position;
        }

        /**
         * 恢复到上次保存的位置。
         *
         * @return 返回到的位置
         */
        private int restorePosition() {
            position = savedPositions.pop();
            return position;
        }

        /**
         * 恢复到上次保存的位置，但是不把位置从记录中消去。
         *
         * @return 返回到的位置
         */
        private int restoreAndKeepPosition() {
            position = savedPositions.peek();
            return position;
        }

        /**
         * 释放上一次保存的位置，不返回。
         *
         * @return 上一次保存的位置
         */
        private int releasePosition() {
            return savedPositions.pop();
        }

        /**
         * 查看当前字符但不移动位置。
         *
         * @param size 查看多少个字符
         * @return 当前字符，或者如果到达末尾则返回'\0'
         */
        private String peek(int size) {
            return input.substring(position, Math.min(input.length(), position + size));
        }

        /**
         * 预读下一个单词（标识符）。
         * 跳过空白字符后，读取连续的字母数字下划线字符。
         *
         * @return 下一个单词，如果到达末尾则返回空字符串
         */
        private String peekWord() {
            int savedPosition = position;
            int start;
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
            start = position;
            while (position < input.length() && (Character.isJavaIdentifierPart(input.charAt(position)))) {
                position++;
            }
            String word = position > start ? input.substring(start, position) : "";
            position = savedPosition;
            return word;
        }

        private String peekWord(int length) {
            if (length <= 0)
                return "";
            int savedPosition = position;
            int start;
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
            start = position;
            int end = Math.min(position + length, input.length());
            while (position < end && Character.isJavaIdentifierPart(input.charAt(position))) {
                position++;
            }
            String word = position > start ? input.substring(start, position) : "";
            position = savedPosition;
            return word;
        }

        /**
         * 跳过空白字符和注释。
         *
         */
        private void advanceToNextMeaningful() {
            while (position < input.length()) {
                char c = input.charAt(position);
                if (Character.isWhitespace(c)) {
                    position++;
                } else if (c == '/' && position + 1 < input.length()) {
                    char next = input.charAt(position + 1);
                    if (next == '/') {
                        while (position < input.length() && input.charAt(position) != '\n'
                                && input.charAt(position) != '\r') {
                            position++;
                        }
                    } else if (next == '*') {
                        position += 2;
                        while (position + 1 < input.length()
                                && !(input.charAt(position) == '*' && input.charAt(position + 1) == '/')) {
                            position++;
                        }
                        if (position + 1 < input.length()) {
                            position += 2;
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        /**
         * 移动到下一个字符。
         *
         * @return 下一个字符，或者如果到达末尾则返回'\0'
         */
        private char advance() {
            return isAtEnd() ? '\0' : input.charAt(position++);
        }

        /**
         * 移动指定数量的字符。
         *
         * @param count 移动的字符数量
         */
        private String advance(int count) {
            StringBuilder sb = new StringBuilder();
            while (count-- > 0 && !isAtEnd()) {
                sb.append(advance());
            }
            return sb.toString();
        }

        /**
         * 匹配当前字符是否为预期字符，如果是则移动位置并返回true。否则返回false。
         *
         * @param expected 预期字符
         * @return 如果匹配则返回true，否则返回false
         */
        private boolean match(char expected) {
            if (peek() == expected) {
                advance();
                return true;
            }
            return false;
        }

        private boolean matchExact(char expected) {
            return matchExact(expected, "");
        }

        private boolean matchExact(char expected, String whiteList) {
            savePosition();
            if (!match(expected)) {
                restorePosition();
                return false;
            }
            if (!Character.isWhitespace(peek()) && !isAtEnd() && !whiteList.contains(peek() + "")) {
                restorePosition();
                return false;
            }
            releasePosition();
            return true;
        }

        private boolean matchKeywordExact(String expected) {
            return matchKeywordExact(expected, "");
        }

        /**
         * 通过全字匹配关键字。
         *
         * @param expected 关键字
         * @return 结果
         */
        private boolean matchKeywordExact(String expected, String whiteList) {
            savePosition();
            if (!matchKeyword(expected)) {
                restorePosition();
                return false;
            }
            if (!Character.isWhitespace(peek()) && !isAtEnd() && !whiteList.contains(peek() + "")) {
                restorePosition();
                return false;
            }
            releasePosition();
            return true;
        }

        /**
         * 判断接下来的字符是不是指定的关键字。
         *
         * @param expected 关键字
         * @return 结果
         */
        private boolean isTargetWord(String expected) {
            return input.substring(position).startsWith(expected);
        }

        /**
         * 判断接下来的字符是不是指定的关键字，如果是，则返回true并且向前移动。
         *
         * @param expected 关键字
         * @return 结果
         */
        private boolean matchKeyword(String expected) {
            if (input.substring(position).startsWith(expected)) {
                position += expected.length();
                return true;
            }
            return false;
        }

        /**
         * 跳过接下来的所有空白字符，直到遇到不是空白的字符。
         */
        private void skipWhitespace() {
            while (Character.isWhitespace(peek())) {
                advance();
            }
        }

        /**
         * 检查是否到达输入的末尾。
         *
         * @return 如果到达末尾则返回true，否则返回false
         */
        private boolean isAtEnd() {
            return position >= input.length();
        }

        /**
         * 断定预期遇到指定字符，否则抛出异常。
         *
         * @param expected 预期字符
         */
        private char expect(char expected) throws RuntimeException {
            skipWhitespace();
            if (peek() != expected) {
                throw new RuntimeException("Expected '" + expected + "' but found '" + peek() + "'");
            }
            return peek();
        }

        private String expectWord(String expected) throws RuntimeException {
            skipWhitespace();
            if (input.substring(position).startsWith(expected)) {
                return expected;
            }
            throw new RuntimeException(
                    "Expected '" + expected + "' at position " + position + " but found '" + peek() + "'");
        }

        /**
         * 断定预期遇到指定字符并移动到下一个字符，否则抛出异常。
         *
         * @param expected 预期字符
         * @return 遇到的实际字符
         * @throws RuntimeException 如果失配
         */
        private char expectToMove(char expected) throws RuntimeException {
            skipWhitespace();
            if (peek() != expected) {
                throw new RuntimeException(
                        "Expected '" + expected + "' at position " + position + " but found '" + peek() + "'");
            }
            return advance();
        }

        private String expectWordToMove(String expected) throws RuntimeException {
            skipWhitespace();
            if (input.substring(position).startsWith(expected)) {
                position += expected.length();
                return expected;
            }
            throw new RuntimeException(
                    "Expected '" + expected + "' at position " + position + " but found '" + peek() + "'");
        }

        /**
         * 断定预期遇到指定字符，否则抛出异常。
         *
         * @param expected 预期字符
         */
        private char expect(char... expected) throws RuntimeException {
            skipWhitespace();
            for (char c : expected)
                if (peek() == c)
                    return peek();
            throw new RuntimeException("Expected one of '" + String.valueOf(expected) +
                    "' at position " + position + " but found '" + peek() + "'");
        }

        /**
         * 断定预期遇到指定字符并移动到下一个字符，否则抛出异常。
         *
         * @param expected 预期字符
         * @return 遇到的实际字符
         * @throws RuntimeException 如果失配
         */
        private char expectToMove(char... expected) throws RuntimeException {
            skipWhitespace();
            for (char c : expected)
                if (peek() == c) {
                    advance();
                    return c;
                }
            throw new RuntimeException("Expected one of '" + String.valueOf(expected) +
                    "' at position " + position + " but found '" + peek() + "'");
        }

        /**
         * 设置最后一个没有被完成的语句。
         *
         * @param node 语句节点
         */
        private void setLastUncompletedStmt(ASTNode node) {
            this.lastUncompleted = node;
        }

        /**
         * 获取最后一个没有被完成的语句。
         *
         * @return 语句节点
         */
        private ASTNode getLastUncompletedStmt() {
            return this.lastUncompleted;
        }

        /**
         * 清除最后一个没有被完成的语句。
         * 注意了，如果一个语句已经被完成了，一定要调用这个。
         */
        private void clearLastUncompletedStmt() {
            this.lastUncompleted = null;
        }

        private void unexpectedToken() throws RuntimeException {
            throw new RuntimeException("Unexpected token '" + peek() + "' at position " + position);
        }

        private void unexpectedToken(String hint) throws RuntimeException {
            throw new RuntimeException(hint + "Unexpected token '" + peek() + "' at position " + position);
        }

        /**
         * 解析一个字符字面量。
         *
         * @return 解析到的字符
         */
        private ASTNode parseChar() throws RuntimeException {
            skipWhitespace();
            expectToMove('\'');
            char c = advance();
            expectToMove('\'');
            return new LiteralNode(c, Character.class);
        }

        private String parseStringCharWithEscape() throws RuntimeException {
            if (peek() == '\\') {
                advance();
                char escaped = advance();
                if (Character.isDigit(escaped)) {
                    // 八进制转义序列
                    String format = "" + escaped + advance() + advance();
                    try {
                        int octalValue = Integer.parseInt(format, 8);
                        return String.valueOf((char) octalValue);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid octal escape sequence \\" + format);
                    }
                } else if (escaped == 'x') {
                    // 十六进制转义序列
                    String format = "" + advance() + advance();
                    try {
                        int hexValue = Integer.parseInt(format, 16);
                        return String.valueOf((char) hexValue);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid hexadecimal escape sequence \\x" + format);
                    }
                } else if (Character.toLowerCase(escaped) == 'u') {
                    // Unicode转义序列
                    String format = "" + advance() + advance() + advance() + advance();
                    try {
                        int unicodeValue = Integer.parseInt(format, 16);
                        return String.valueOf((char) unicodeValue);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid Unicode escape sequence \\u" + format);
                    }
                } else {
                    return String.valueOf(
                            switch (escaped) {
                                case 'b' -> '\b';
                                case 't' -> '\t';
                                case 'n' -> '\n';
                                case 'f' -> '\f';
                                case 'r' -> '\r';
                                case '"' -> '"';
                                case '\'' -> '\'';
                                case '\\' -> '\\';
                                default -> {
                                    logger.warn("未知的转义序列: \\" + escaped);
                                    context.printWarn("Invalid escape sequence \\" + escaped + "\n");
                                    yield "" + '\\' + escaped;
                                }
                            });
                }
            } else {
                return String.valueOf(advance());
            }
        }

        private ASTNode parseString() throws RuntimeException {
            skipWhitespace();
            expectToMove('"');
            StringBuilder sb = new StringBuilder();
            while (!match('"')) {
                sb.append(parseStringCharWithEscape());
                if (peek() == '\0' || peek() == '\n' || peek() == '\r') {
                    throw new RuntimeException("Unterminated string literal");
                }
            }
            return new LiteralNode(sb.toString(), String.class);
        }

        private ASTNode parseNumber() throws RuntimeException {
            StringBuilder sb = new StringBuilder();
            while (Character.isDigit(peek()) ||
                    peek() == '.' ||
                    (peek() == '+' && sb.length() == 0) ||
                    (peek() == '-' && sb.length() == 0) || peek() == 'o' || peek() == 'O' ||
                    peek() == 'l' || peek() == 'L' || peek() == 'x' || peek() == 'X' ||
                    (peek() >= 'a' && peek() <= 'f') || (peek() >= 'A' && peek() <= 'F'))
                sb.append(advance());
            String numberStr = sb.toString();
            if (numberStr.startsWith("0") && numberStr.length() >= 2 && Character.isDigit(numberStr.charAt(1))) {
                // 八进制数
                try {
                    return new LiteralNode(Integer.parseInt(numberStr, 8), Integer.class);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid octal number: " + numberStr);
                }
            } else if (numberStr.startsWith("0b")) {
                try {
                    return new LiteralNode(Integer.parseInt(numberStr.substring(2), 2), Integer.class);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid binary number: " + numberStr);
                }
            } else if (numberStr.startsWith("0x")) {
                try {
                    return new LiteralNode(Integer.parseInt(numberStr.substring(2), 16), Integer.class);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid hexadecimal number: " + numberStr);
                }
            } else if (numberStr.startsWith("0o")) {
                try {
                    return new LiteralNode(Integer.parseInt(numberStr.substring(2), 8), Integer.class);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid octal number: " + numberStr);
                }

            } else if (numberStr.endsWith("f") || numberStr.endsWith("F")) {
                try {
                    return new LiteralNode(Float.parseFloat(numberStr.substring(0, numberStr.length() - 1)),
                            Float.class);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid float number: " + numberStr);
                }
            } else if (numberStr.endsWith("d") || numberStr.endsWith("D")) {
                try {
                    return new LiteralNode(Double.parseDouble(numberStr.substring(0, numberStr.length() - 1)),
                            Double.class);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid double number: " + numberStr);
                }
            } else if (numberStr.contains("e") || numberStr.contains("E") || numberStr.contains(".")) {
                try {
                    return new LiteralNode(Double.parseDouble(numberStr), Double.class);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid double number: " + numberStr);
                }
            } else if (numberStr.endsWith("l") || numberStr.endsWith("L")) {
                try {
                    return new LiteralNode(Long.parseLong(numberStr.substring(0, numberStr.length() - 1)), Long.class);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid long number: " + numberStr);
                }
            } else {
                try {
                    return new LiteralNode(Integer.parseInt(numberStr), Integer.class);
                } catch (NumberFormatException e) {
                    try {
                        return new LiteralNode(Long.parseLong(numberStr), Long.class);
                    } catch (NumberFormatException e2) {
                        throw new RuntimeException("Invalid number: " + numberStr);
                    }
                }
            }
        }

        private ASTNode parseArrayOrMap() throws RuntimeException {
            skipWhitespace();
            savePosition();
            try {
                ASTNode result = parseMap();
                releasePosition();
                return result;
            } catch (RuntimeException e) {
                restorePosition();
                savePosition();
                try {
                    ASTNode result = parseArray();
                    releasePosition();
                    return result;
                } catch (RuntimeException e2) {
                    restorePosition();
                    throw new RuntimeException("Cannot parse as array or map: " + e + ", " + e2);
                }
            }
        }

        private ASTNode parseArray() throws RuntimeException {
            skipWhitespace();
            expectToMove('{', '[');
            List<ASTNode> elements = new ArrayList<>();

            while (peek() != '}' && peek() != ']') {
                char currentChar = peek();
                if (currentChar == ',' || currentChar == '}' || currentChar == ']') {
                    throw new RuntimeException("Array element cannot be empty, position: " + position
                            + ", current character: '" + currentChar + "'");
                }

                elements.add(parseLiteral());
                skipWhitespace();
                if (peek() == ',') {
                    advance();
                    skipWhitespace();
                } else {
                    break;
                }
            }
            expectToMove('}', ']');
            return new ArrayNode(elements);
        }

        private ASTNode parseMap() throws RuntimeException {
            skipWhitespace();
            expectToMove('{');
            Map<String, ASTNode> entries = new HashMap<>();

            while (peek() != '}') {
                if (peek() != '"' && peek() != '\'') {
                    throw new RuntimeException("Map key must be a string, position: " + position
                            + ", current character: '" + peek() + "'");
                }

                String key = (String) ((LiteralNode) parseString()).value;
                skipWhitespace();
                expectToMove(':');
                ASTNode value = parseLiteral();
                entries.put(key, value);
                skipWhitespace();
                if (peek() == ',') {
                    advance();
                    skipWhitespace();
                } else {
                    break;
                }
            }
            expectToMove('}');
            return new MapNode(entries, Map.class);
        }

        // 从下面开始就是需要用到 save/restore/release Position 的地方了

        private ASTNode parseLiteral() throws RuntimeException {
            skipWhitespace();
            savePosition();
            boolean failure = false;
            try {
                ASTNode expr = parsePrimary();

                while (true) {
                    skipWhitespace();
                    if (match('.')) {
                        skipWhitespace();

                        String member = parseIdentifier();
                        skipWhitespace();

                        // 只有在第一个表达式是变量节点且是内置函数时才检查
                        // 对于链式调用如System.out.println，println不应该被检查是否为内置函数
                        if (expr instanceof VariableNode && context.hasBuiltIn(((VariableNode) expr).name)
                                && context.hasBuiltIn(member)) {
                            // 内置函数不能通过点操作符调用
                            throw new RuntimeException("Built-in function cannot be accessed with dot operator");
                        }

                        // 特殊处理：如果当前表达式是类引用，尝试解析嵌套类
                        if (expr instanceof ClassReferenceNode) {
                            ClassReferenceNode classRef = (ClassReferenceNode) expr;
                            String nestedClassName = classRef.className + "." + member;
                            if (context.hasClass(nestedClassName)) {
                                // 是嵌套类，创建新的类引用节点
                                expr = new ClassReferenceNode(nestedClassName);
                                continue;
                            }
                        }

                        if (peek() == '(') {
                            expectToMove('(');
                            List<ASTNode> args = parseArguments();
                            expectToMove(')');
                            expr = new MethodCallNode(expr, member, args);
                        } else {
                            expr = new FieldAccessNode(expr, member);
                        }
                    } else if (match('[')) {
                        skipWhitespace();
                        ASTNode index = parseAdditive();
                        expectToMove(']');
                        expr = new ArrayAccessNode(expr, index);
                    } else {
                        break;
                    }
                }

                return expr;

            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析表达式失败: " + e.getMessage());
                throw new RuntimeException("Error parsing expression: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parsePrimary() throws RuntimeException {
            skipWhitespace();
            savePosition(); // 被最外侧try处理过了
            boolean failure = false;

            try {
                // 处理字面量
                if (peek() == '\'') {
                    return parseChar();
                } else if (peek() == '"') {
                    return parseString();
                } else if (peek() == '-' || peek() == '+' || Character.isDigit(peek())) {
                    return parseNumber();
                } else if (peek() == '{') {
                    return parseArrayOrMap();
                }

                if (peek() == '(') {
                    savePosition();
                    try {
                        ASTNode lambda = parseLambdaExpression();
                        releasePosition();
                        return lambda;
                    } catch (RuntimeException e) {
                        restorePosition();
                    }
                }

                if (peek() == '(') {
                    expectToMove('(');
                    ASTNode expr = parseExpression();
                    expectToMove(')');
                    return new ParenthesizedExpressionNode(expr);
                }
                String identifier;
                try {
                    identifier = parseIdentifier();

                } catch (RuntimeException e) {
                    throw new RuntimeException(
                            "Expected identifier or literal at position " + position + ", but found '" + peek() + "'");
                }

                switch (identifier) {
                    case "true":
                        return new LiteralNode(true, Boolean.class);
                    case "false":
                        return new LiteralNode(false, Boolean.class);
                    case "null":
                        return new LiteralNode(null, Void.class);
                    case "new":
                        return parseConstructorCall();
                }

                if (context.hasBuiltIn(identifier)) {
                    skipWhitespace();
                    if (peek() == '(') {
                        expectToMove('(');
                        List<ASTNode> args = parseArguments();
                        expectToMove(')');
                        return new MethodCallNode(new VariableNode(identifier), identifier, args);
                    } else {
                        return new VariableNode(identifier);
                    }
                }

                try {
                    if (!context.hasClass(identifier))
                        throw new ClassNotFoundException("Class not found: " + identifier);
                    return new ClassReferenceNode(identifier);
                } catch (ClassNotFoundException e) {
                    // 尝试解析更完整的类名
                    skipWhitespace();
                    savePosition();
                    if (peek() == '.') {
                        savePosition();
                        StringBuilder fullName = new StringBuilder(identifier);
                        String lastValidName = null; // 尽量匹配最长的类名，然后后面解析为字段 （貌似可行？）
                        boolean parseSucceedOnce = false;
                        while (peek() == '.') {
                            savePosition();
                            expectToMove('.');
                            String nextPart = parseIdentifier();
                            fullName.append('.').append(nextPart);
                            
                            if (context.hasClass(fullName.toString())) {
                                lastValidName = fullName.toString();
                                parseSucceedOnce = true;
                                releasePosition();
                            } else {
                                if (parseSucceedOnce) { // 后面就是类字段引用了
                                    restorePosition();
                                    break;
                                }
                            }
                        }
                        
                        if (lastValidName != null) {
                            releasePosition();
                            // 在这里添加下后面的字段解析 (理论这里已经到.DigitOnes了)
                            return new ClassReferenceNode(lastValidName);
                        } else {
                            // 没有匹配到类，回滚到原始位置
                            restorePosition();
                        }
                    }
                    
                    ASTNode varNode = new VariableNode(identifier);

                    if (peek() == '(') {
                        expectToMove('(');
                        List<ASTNode> args = parseArguments();
                        expectToMove(')');
                        return new DirectFunctionCallNode(varNode, args);
                    }

                    return varNode;
                }

            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析基本表达式失败: " + e.getMessage());
                throw e;
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseWhileStatement() {
            savePosition();
            boolean failure = false;
            try {
                expectWordToMove("while");
                skipWhitespace();
                expectToMove('(');
                ASTNode condition = parseExpression();
                expectToMove(')');

                skipWhitespace();
                ASTNode body;
                if (peek() == '{') {
                    body = parseBlock();
                } else {
                    BlockNode block = new BlockNode();
                    ASTNode stmt = parseStatement();
                    block.addStatement(stmt);
                    body = block;
                }

                return new WhileNode(condition, body);
            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析while语句失败: " + e.getMessage());
                throw new RuntimeException("Error parsing while statement: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseDoWhileStatement() {
            savePosition();
            boolean failure = false;
            try {
                expectWordToMove("do");
                skipWhitespace();
                ASTNode body;
                if (peek() == '{') {
                    body = parseBlock();
                } else {
                    BlockNode block = new BlockNode();
                    ASTNode stmt = parseStatement();
                    block.addStatement(stmt);
                    body = block;
                }

                skipWhitespace();
                expectWordToMove("while");
                skipWhitespace();
                expectToMove('(');
                ASTNode condition = parseExpression();
                expectToMove(')');
                skipWhitespace();
                expectToMove(';');

                return new DoWhileNode(condition, body);
            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析do-while语句失败: " + e.getMessage());
                throw new RuntimeException("Error parsing do-while statement: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseSwitchStatement() {
            savePosition();
            boolean failure = false;
            try {
                expectWordToMove("switch");
                skipWhitespace();
                expectToMove('(');
                ASTNode expression = parseExpression();
                expectToMove(')');
                skipWhitespace();
                expectToMove('{');

                List<CaseNode> cases = new ArrayList<>();
                ASTNode defaultCase = null;

                skipWhitespace();
                while (peek() != '}' && position < input.length()) {
                    if (isTargetWord("case")) {
                        expectWordToMove("case");
                        skipWhitespace();
                        ASTNode caseValue = parseExpression();
                        expectToMove(':');
                        skipWhitespace();

                        BlockNode caseBody = new BlockNode();
                        while (peek() != '}' && !isTargetWord("case") && !isTargetWord("default")) {
                            ASTNode stmt = parseCompletedStatement();
                            if (stmt != null) {
                                caseBody.addStatement(stmt);
                            } else {
                                char currentChar = peek();
                                if (currentChar != '}' && !Character.isWhitespace(currentChar)) {
                                    throw new RuntimeException("Unparseable case statement, position: " + position
                                            + ", current character: '" + currentChar + "'");
                                }
                                advance();
                            }
                            skipWhitespace();
                        }
                        cases.add(new CaseNode(caseValue, caseBody));
                    } else if (isTargetWord("default")) {
                        expectWordToMove("default");
                        expectToMove(':');
                        skipWhitespace();

                        BlockNode defaultBody = new BlockNode();
                        while (peek() != '}' && !isTargetWord("case")) {
                            ASTNode stmt = parseCompletedStatement();
                            if (stmt != null) {
                                defaultBody.addStatement(stmt);
                            } else {
                                char currentChar = peek();
                                if (currentChar != '}' && !Character.isWhitespace(currentChar)) {
                                    throw new RuntimeException("Cannot parse default statement, position: " + position
                                            + ", current character: '" + currentChar + "'");
                                }
                                advance();
                            }
                            skipWhitespace();
                        }
                        defaultCase = defaultBody;
                    } else {
                        char currentChar = peek();
                        if (currentChar != '}' && !Character.isWhitespace(currentChar)) {
                            throw new RuntimeException("Cannot parse switch statement, position: " + position
                                    + ", current character: '" + currentChar + "'");
                        }
                        advance();
                        skipWhitespace();
                    }
                }

                expectToMove('}');
                return new SwitchNode(expression, cases, defaultCase);
            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析switch语句失败: " + e.getMessage());
                throw new RuntimeException("Error parsing switch statement: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseReturnStatement() {
            skipWhitespace();
            expectWordToMove("return");
            if (peek() == ';') {
                return null;
            } else {
                ASTNode value = parseExpression();
                // expectToMove(';');
                return new ControlNode("return", value);
            }
        }

        private ASTNode parseTryStatement() {
            skipWhitespace();
            savePosition();
            boolean failure = false;
            try {
                expectWordToMove("try");
                skipWhitespace();

                ASTNode tryBlock;
                if (peek() == '{') {
                    tryBlock = parseBlock();
                } else {
                    throw new RuntimeException("try block must be enclosed in braces");
                }

                List<TryCatchNode.CatchBlock> catchBlocks = new ArrayList<>();

                skipWhitespace();
                while (isTargetWord("catch")) {
                    expectWordToMove("catch");
                    skipWhitespace();
                    expectToMove('(');

                    skipWhitespace();
                    String exceptionType = parseClassIdentifier();
                    skipWhitespace();
                    String exceptionName = parseIdentifier();
                    skipWhitespace();
                    expectToMove(')');

                    ASTNode catchBlock;
                    skipWhitespace();
                    if (peek() == '{') {
                        catchBlock = parseBlock();
                    } else {
                        catchBlock = parseCompletedStatement();
                    }

                    catchBlocks.add(new TryCatchNode.CatchBlock(exceptionType, exceptionName, catchBlock));
                    skipWhitespace();
                }

                ASTNode finallyBlock = null;
                if (isTargetWord("finally")) {
                    expectWordToMove("finally");
                    skipWhitespace();
                    if (peek() == '{') {
                        finallyBlock = parseBlock();
                    } else {
                        finallyBlock = parseCompletedStatement();
                    }
                }

                if (catchBlocks.isEmpty() && finallyBlock == null) {
                    throw new RuntimeException("try statement must have at least one catch block or a finally block");
                }

                return new TryCatchNode(tryBlock, catchBlocks, finallyBlock);
            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析try语句失败: " + e.getMessage());
                throw new RuntimeException("Error parsing try statement: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseThrowStatement() {
            skipWhitespace();
            savePosition();
            boolean failure = false;
            try {
                expectWordToMove("throw");
                skipWhitespace();
                ASTNode exception = parseExpression();
                return new ThrowNode(exception);
            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析throw语句失败: " + e.getMessage());
                throw new RuntimeException("Error parsing throw statement: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseImportStatement() {
            savePosition();
            skipWhitespace();
            boolean failure = false;
            try {
                expectWordToMove("import");
                skipWhitespace();
                String pkgName = parseImportClassIdentifier();
                return new ImportNode(pkgName);
            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析Import失败: " + e.getMessage());
                throw new RuntimeException("Error parsing import statement: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseDeleteStatement() {
            savePosition();
            skipWhitespace();
            boolean failure = false;
            try {
                expectWordToMove("delete");
                skipWhitespace();
                String varName;
                if (peek() == '*') {
                    advance();
                    varName = "*";
                } else {
                    varName = parseIdentifier();
                }
                return new DeleteNode(varName);
            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析delete失败: " + e.getMessage());
                throw new RuntimeException("Error parsing delete statement: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseBlock() {
            savePosition();
            skipWhitespace();
            boolean failure = false;
            try {
                expectToMove('{');
                BlockNode block = new BlockNode();
                skipWhitespace();

                while (peek() != '}' && position < input.length()) {
                    ASTNode stmt = parseCompletedStatement();
                    if (stmt != null) {
                        block.addStatement(stmt);
                    } else {
                        char currentChar = peek();
                        if (currentChar != '}' && !Character.isWhitespace(currentChar)) {
                            throw new RuntimeException("Unparseable code block statement, position: " + position
                                    + ", current character: '" + currentChar + "'");
                        }
                        advance();
                    }
                    skipWhitespace();
                }
                expectToMove('}');
                return block;
            } catch (RuntimeException e) {
                failure = true;
                throw new RuntimeException("Error parsing block: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseForStatement() {
            savePosition();
            boolean failure = false;
            try {

                expectWordToMove("for");
                skipWhitespace();
                expectToMove('(');
                skipWhitespace();

                try {
                    parseClassIdentifier();
                    skipWhitespace();
                    parseIdentifier();
                    skipWhitespace();
                    if (peek() == ':') {
                        restorePosition();
                        savePosition();
                        return parseForEachStatement();
                    }
                } catch (Exception ignored) {
                }
                restorePosition();
                savePosition();
                return parseTraditionalForLoop();
            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析for语句失败: " + e.getMessage());
                throw new RuntimeException("Error parsing for statement: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseTraditionalForLoop() {
            savePosition();
            boolean failure = false;
            try {
                expectWordToMove("for");
                skipWhitespace();
                expectToMove('(');
                skipWhitespace();
                ASTNode init = null;
                if (peek() != ';') {
                    try {
                        init = parseVariableDeclaration();
                    } catch (RuntimeException e) {
                        try {
                            init = parseVariableAssignment();
                        } catch (RuntimeException e2) {
                            init = parseExpression();
                        }
                    }
                }
                skipWhitespace();
                expectToMove(';');
                skipWhitespace();

                ASTNode condition = null;
                if (peek() != ';')
                    condition = parseExpression();

                skipWhitespace();
                expectToMove(';');
                skipWhitespace();

                skipWhitespace();
                ASTNode update = null;
                if (peek() != ')') {
                    update = parseStatement();
                }

                skipWhitespace();
                expectToMove(')');

                skipWhitespace();
                ASTNode body;

                if (peek() == '{') {
                    body = parseBlock();
                } else {
                    BlockNode block = new BlockNode();
                    ASTNode stmt = parseStatement();
                    block.addStatement(stmt);
                    body = block;
                }

                return new ForNode(init, condition, update, body);
            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析for循环失败: " + e.getMessage());
                throw new RuntimeException("Error parsing for loop: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseForEachStatement() {
            savePosition();
            boolean failure = false;
            try {
                expectWordToMove("for");
                skipWhitespace();
                expectToMove('(');
                skipWhitespace();

                String typeName = parseClassIdentifier();
                skipWhitespace();
                String itemName = parseIdentifier();
                skipWhitespace();

                expectToMove(':');

                skipWhitespace();
                ASTNode collection = parseExpression();
                expectToMove(')');

                skipWhitespace();
                ASTNode body;
                if (peek() == '{') {
                    body = parseBlock();
                } else {
                    BlockNode block = new BlockNode();
                    ASTNode stmt = parseStatement();
                    block.addStatement(stmt);
                    body = block;
                }

                return new ForEachNode(typeName, itemName, collection, body);
            } catch (RuntimeException e) {
                failure = true;
                logger.error("解析for-each循环失败: " + e.getMessage());
                throw new RuntimeException("Error parsing for-each statement: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseConstructorCall() {
            skipWhitespace();
            List<ASTNode> args = null;
            ASTNode arrInitial = null;
            String className = parseClassIdentifier();
            if (className.endsWith("]")) {
                skipWhitespace();
                if (peek() == '{') {
                    arrInitial = parseArray();
                } else {
                    try {
                        arrInitial = parseExpression();
                    } catch (RuntimeException ignored) {
                    }
                }
            } else {
                skipWhitespace();
                expectToMove('(');
                skipWhitespace();
                args = parseArguments();
                skipWhitespace();
                expectToMove(')');
            }

            return new ConstructorCallNode(className, args, arrInitial);
        }

        private void skipComment() {
            expectWordToMove("//");
            while (position < input.length() && peek() != '\n' && peek() != '\r')
                advance();
        }

        private void skipMultipleLineComment() {
            expectWordToMove("/*");
            while (position < input.length() && !isTargetWord("*/"))
                advance();
            if (position >= input.length()) {
                logger.error("多行注释未闭合");
                throw new RuntimeException("Unterminated multi-line comment");
            }
            expectWordToMove("*/");
        }

        public ASTNode parseStatement() {
            skipWhitespace();
            while (isTargetWord("//")) {
                skipComment();
            }
            while (isTargetWord("/*")) {
                skipMultipleLineComment();
            }
            if (isAtEnd())
                return null;
            ASTNode result;
            skipWhitespace();

            if (isTargetWord("class")) {
                result = parseClassDeclaration();
                clearLastUncompletedStmt();
                return result;
            } else if (isTargetWord("interface")) {
                // return parseInterfaceDeclaration();
                throw new RuntimeException("Interface declarations are not yet supported");
            } else if (isTargetWord("enum")) {
                // return parseEnumDeclaration();
                throw new RuntimeException("Enum declarations are not yet supported");
            }

            if (isTargetWord("import")) {
                result = parseImportStatement();
                clearLastUncompletedStmt();
                return result;
            } else if (isTargetWord("delete")) {
                result = parseDeleteStatement();
                clearLastUncompletedStmt();
                return result;
            } else if (isTargetWord("if")) {
                result = parseIfStatement();
                clearLastUncompletedStmt();
                return result;
            } else if (isTargetWord("while")) {
                result = parseWhileStatement();
                clearLastUncompletedStmt();
                return result;
            } else if (isTargetWord("do")) {
                result = parseDoWhileStatement();
                clearLastUncompletedStmt();
                return result;
            } else if (isTargetWord("switch")) {
                result = parseSwitchStatement();
                clearLastUncompletedStmt();
                return result;
            } else if (isTargetWord("for")) {
                result = parseForStatement();
                clearLastUncompletedStmt();
                return result;
            } else if (isTargetWord("break")) {
                expectWordToMove("break");
                skipWhitespace();
                clearLastUncompletedStmt();
                return new ControlNode("break");
            } else if (isTargetWord("continue")) {
                expectWordToMove("continue");
                skipWhitespace();
                clearLastUncompletedStmt();
                return new ControlNode("continue");
            } else if (isTargetWord("return")) {
                result = parseReturnStatement();
                clearLastUncompletedStmt();
                return result;
            } else if (isTargetWord("try")) {
                result = parseTryStatement();
                clearLastUncompletedStmt();
                return result;
            } else if (isTargetWord("throw")) {
                result = parseThrowStatement();
                clearLastUncompletedStmt();
                return result;
            }

            try {
                result = parseVariableDeclaration();
                setLastUncompletedStmt(result);
                return result;
            } catch (Exception ignored) {
            }

            if (peek() == '{') {
                result = parseBlock();
                clearLastUncompletedStmt();
                return result;
            }

            int savePos = position;
            try {
                result = parseAssignment();
                setLastUncompletedStmt(result);
                return result;
            } catch (Exception e) {
                position = savePos;
                result = parseExpression();
                setLastUncompletedStmt(result);
                skipWhitespace();
                if (peek() == ';') {
                    advance();
                    clearLastUncompletedStmt();
                }
                return result;
            }
        }

        public ASTNode parseCompletedStatement() {
            ASTNode result = parseStatement();

            if (isAtEnd())
                return result;

            while (!(getLastUncompletedStmt() == null) && !(peek() == ';')) {
                result = parseStatement();
                if (result == null)
                    return null;
            }
            if (match(';'))
                clearLastUncompletedStmt();
            return result;
        }

        private String parseIdentifier() {
            StringBuilder sb = new StringBuilder();
            savePosition();
            while (position < input.length()) {
                char c = input.charAt(position);
                // 标识符可以包含字母, 数字, 下划线和美元符号
                if (Character.isLetterOrDigit(c) || c == '_' || c == '$') {
                    sb.append(c);
                    position++;
                } else {
                    break;
                }
            }
            String result = sb.toString();

            if (result.isEmpty()) {
                restorePosition();
                throw new RuntimeException("Cannot parse identifier at position " + position);
            } else {
                if (Character.isDigit(result.charAt(0))) {
                    restorePosition();
                    unexpectedToken("Invalid identifier: ");
                } else if (result.charAt(0) == '.') {
                    restorePosition();
                    unexpectedToken("Invalid identifier: ");
                }
            }
            releasePosition();
            return result;
        }

        private String parseClassIdentifier() {
            StringBuilder sb = new StringBuilder();
            savePosition();
            while (position < input.length()) {
                char c = input.charAt(position);
                // 类名可以包含字母, 数字, 下划线, 小数点(完整的类名), 方括号(数组), 大小于号(模板类), 问号(泛型)和美元符号
                if (Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '?'
                        || c == '.' || c == '[' || c == ']' || c == '<' || c == '>') {
                    sb.append(c);
                    position++;
                } else {
                    break;
                }
            }
            String result = sb.toString();
            if (result.isEmpty()) {
                restorePosition();
                throw new RuntimeException("Cannot parse identifier at position " + position);
            } else {
                if (Character.isDigit(result.charAt(0))) {
                    restorePosition();
                    unexpectedToken("Invalid class name: ");
                } else if (result.charAt(0) == '.' || result.charAt(0) == '<' || result.charAt(0) == '>'
                        || result.charAt(0) == '[' || result.charAt(0) == ']') {
                    restorePosition();
                    unexpectedToken("Invalid class name: ");
                }
            }
            validateClassIdentifier(result);
            releasePosition();
            return result;
        }

        private void validateClassIdentifier(String className) {
            if (className == null || className.isEmpty()) {
                return;
            }

            int bracketDepth = 0;
            int angleBracketDepth = 0;

            for (int i = 0; i < className.length(); i++) {
                char c = className.charAt(i);
                if (c == '[') {
                    bracketDepth++;
                } else if (c == ']') {
                    bracketDepth--;
                    if (bracketDepth < 0) {
                        throw new RuntimeException("Invalid class name: unmatched closing bracket ']' in " + className);
                    }
                } else if (c == '<') {
                    angleBracketDepth++;
                } else if (c == '>') {
                    angleBracketDepth--;
                    if (angleBracketDepth < 0) {
                        throw new RuntimeException(
                                "Invalid class name: unmatched closing angle bracket '>' in " + className);
                    }
                }
            }

            if (bracketDepth != 0) {
                throw new RuntimeException("Invalid class name: unmatched opening bracket '[' in " + className);
            }

            if (angleBracketDepth != 0) {
                throw new RuntimeException("Invalid class name: unmatched opening angle bracket '<' in " + className);
            }
        }

        private String parseImportClassIdentifier() {
            StringBuilder sb = new StringBuilder();
            savePosition();
            boolean usedStar = false;
            while (position < input.length()) {
                char c = input.charAt(position);
                // 类名可以包含字母, 数字, 下划线, 小数点(完整的类名), 美元符号和星号（通配符)
                if (Character.isLetterOrDigit(c) || c == '.' || c == '$' || c == '_' || c == '*') {
                    if (usedStar)
                        unexpectedToken();
                    if (c == '*')
                        usedStar = true;
                    sb.append(c);
                    position++;
                } else {
                    break;
                }
            }
            String result = sb.toString();
            if (result.isEmpty()) {
                restorePosition();
                throw new RuntimeException("Cannot parse identifier at position " + position);
            } else {
                if (Character.isDigit(result.charAt(0))) {
                    restorePosition();
                    unexpectedToken("Invalid class name: ");
                } else if (result.charAt(0) == '.' || result.charAt(0) == '<' || result.charAt(0) == '>'
                        || result.charAt(0) == '[' || result.charAt(0) == ']') {
                    restorePosition();
                    unexpectedToken("Invalid class name: ");
                }
            }
            validateImportClassIdentifier(result);
            releasePosition();
            return result;
        }

        private void validateImportClassIdentifier(String className) {
            if (className == null || className.isEmpty()) {
                return;
            }

            if (className.contains("<") || className.contains(">")) {
                throw new RuntimeException(
                        "Invalid import: generic types not allowed in import statement: " + className);
            }

            if (className.contains("[") || className.contains("]")) {
                throw new RuntimeException("Invalid import: array types not allowed in import statement: " + className);
            }
        }

        private ConstructorDefinition tryParseConstructor(String className) {
            skipWhitespace();
            savePosition();
            try {
                ConstructorDefinition constructorDef = parseConstructorDefinition(className, false);
                releasePosition();
                return constructorDef;
            } catch (RuntimeException e) {
                restorePosition();
                return null;
            }
        }

        private FieldDefinition tryParseField() {
            skipWhitespace();
            savePosition();
            try {
                FieldDefinition fieldDef = parseFieldDeclaration();
                if (fieldDef != null) {
                    releasePosition();
                    return fieldDef;
                }
                restorePosition();
                return null;
            } catch (RuntimeException e) {
                restorePosition();
                return null;
            }
        }

        private MethodDefinition tryParseMethod() {
            skipWhitespace();
            savePosition();
            try {
                MethodDefinition methodDef = parseMethodDeclaration();
                if (methodDef != null) {
                    releasePosition();
                    return methodDef;
                }
                restorePosition();
                return null;
            } catch (RuntimeException e) {
                restorePosition();
                return null;
            }
        }

        private ClassDeclarationNode parseClassDeclaration() {
            skipWhitespace();

            if (!matchKeyword("class")) {
                throw new RuntimeException("Expected 'class' keyword at position " + position);
            }
            skipWhitespace();

            String className = parseIdentifier();
            ClassDefinition classDef = new ClassDefinition(className);
            skipWhitespace();

            if (matchKeyword("extends")) {
                skipWhitespace();
                String superClassName = parseIdentifier();
                classDef.setSuperClassName(superClassName);
                skipWhitespace();
            }

            if (matchKeyword("implements")) {
                skipWhitespace();

                while (true) {
                    String interfaceName = parseIdentifier();
                    classDef.addInterfaceName(interfaceName);
                    skipWhitespace();

                    if (peek() == ',') {
                        expectToMove(',');
                        skipWhitespace();
                    } else {
                        break;
                    }
                }
            }

            expectToMove('{');
            skipWhitespace();

            int lastPosition = position;
            int stagnantCount = 0;
            while (peek() != '}') {
                skipWhitespace();
                if (peek() == '}')
                    break;

                int startPos = position;
                String firstWord = peekWord();

                if (firstWord.isEmpty()) {
                    advanceToNextMeaningful();
                    continue;
                }

                boolean parsed = false;

                if (firstWord.equals(className)) {
                    ConstructorDefinition constructorDef = tryParseConstructor(className);
                    if (constructorDef != null) {
                        classDef.addConstructor(constructorDef);
                        parsed = true;
                    }
                }

                if (!parsed && isAccessModifier(firstWord)) {
                    expectWordToMove(firstWord);
                    skipWhitespace();
                    int afterModifierPos = position;

                    String secondWord = peekWord();

                    if (secondWord.equals(className)) {
                        ConstructorDefinition constructorDef = parseConstructorDefinition(className, false);
                        if (constructorDef != null) {
                            classDef.addConstructor(constructorDef);
                            parsed = true;
                        } else {
                            position = afterModifierPos;
                        }
                    }

                    if (!parsed) {
                        FieldDefinition fieldDef = tryParseField();
                        if (fieldDef != null) {
                            classDef.addField(fieldDef.getFieldName(), fieldDef);
                            parsed = true;
                        } else {
                            position = afterModifierPos;
                        }
                    }

                    if (!parsed) {
                        MethodDefinition methodDef = tryParseMethod();
                        if (methodDef != null) {
                            classDef.addMethod(methodDef.getMethodName(), methodDef);
                            parsed = true;
                        } else {
                            position = afterModifierPos;
                        }
                    }
                } else if (!parsed
                        && (isTypeOrVoid(firstWord) || firstWord.equals("static") || firstWord.equals("final"))) {
                    if (firstWord.equals("static") || firstWord.equals("final")) {
                        String nextWord = peekWordAfterWhitespace();
                        if (isTypeOrVoid(nextWord)) {
                            expectWordToMove(nextWord);
                            skipWhitespace();

                            String thirdWord = peekWord();

                            if (!thirdWord.isEmpty() && !isKeyword(thirdWord) && peekAfterWord(thirdWord) == '(') {
                                MethodDefinition methodDef = parseMethodDeclarationWithType(nextWord);
                                if (methodDef != null) {
                                    classDef.addMethod(methodDef.getMethodName(), methodDef);
                                    parsed = true;
                                }
                            }

                            if (!parsed) {
                                FieldDefinition fieldDef = tryParseField();
                                if (fieldDef != null) {
                                    classDef.addField(fieldDef.getFieldName(), fieldDef);
                                    parsed = true;
                                }
                            }

                            if (!parsed) {
                                MethodDefinition methodDef = tryParseMethod();
                                if (methodDef != null) {
                                    classDef.addMethod(methodDef.getMethodName(), methodDef);
                                    parsed = true;
                                }
                            }
                        } else {
                            FieldDefinition fieldDef = tryParseField();
                            if (fieldDef != null) {
                                classDef.addField(fieldDef.getFieldName(), fieldDef);
                                parsed = true;
                            }

                            if (!parsed) {
                                MethodDefinition methodDef = tryParseMethod();
                                if (methodDef != null) {
                                    classDef.addMethod(methodDef.getMethodName(), methodDef);
                                    parsed = true;
                                }
                            }
                        }
                    } else {
                        FieldDefinition fieldDef = tryParseField();
                        if (fieldDef != null) {
                            classDef.addField(fieldDef.getFieldName(), fieldDef);
                            parsed = true;
                        }

                        if (!parsed) {
                            MethodDefinition methodDef = tryParseMethod();
                            if (methodDef != null) {
                                classDef.addMethod(methodDef.getMethodName(), methodDef);
                                parsed = true;
                            }
                        }
                    }
                }

                if (!parsed) {
                    logger.warn("无法识别的类成员 at " + startPos + ": "
                            + input.substring(startPos, Math.min(startPos + 50, input.length())));
                    position = startPos;
                    advanceToNextMeaningful();
                    if (position == startPos) {
                        position++;
                    }
                }

                if (position == lastPosition) {
                    stagnantCount++;
                    if (stagnantCount > 100) {
                        throw new RuntimeException(
                                "Class body parsing stuck in infinite loop, position stuck at: " + position);
                    }
                } else {
                    stagnantCount = 0;
                    lastPosition = position;
                }
            }

            expectToMove('}');

            return new ClassDeclarationNode(className, classDef);
        }

        private ASTNode parseVariableDeclaration() {
            skipWhitespace();
            savePosition();
            boolean failure = false;
            try {
                StringBuilder className = new StringBuilder(parseClassIdentifier());
                skipWhitespace();
                String variableName = parseIdentifier();
                skipWhitespace();

                while (peek() == '[') { // 也有把方括号写在变量名后面的
                    expectToMove('[');
                    expectToMove(']');
                    className.append("[]");
                    skipWhitespace();
                }

                if (peek() == '=') {
                    expectToMove('=');
                    ASTNode value = parseExpression();
                    expect(';');
                    return new VariableDeclarationNode(className.toString(), variableName, value);
                } else {
                    expect(';');
                    return new VariableDeclarationNode(className.toString(), variableName, null);
                }
            } catch (RuntimeException e) {
                failure = true;
                throw new RuntimeException("Invalid variable declaration");
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseVariableAssignment() {
            skipWhitespace();
            savePosition();
            boolean failure = false;
            try {
                String variableName = parseIdentifier();
                skipWhitespace();
                expectToMove('=');
                ASTNode value = parseExpression();
                expectToMove(';');
                return new VariableAssignmentNode(variableName, value);
            } catch (RuntimeException e) {
                failure = true;
                throw new RuntimeException("Invalid variable assignment");
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseLambdaExpression() {
            skipWhitespace();
            savePosition();
            boolean failure = false;

            try {
                if (peek() != '(') {
                    throw new RuntimeException("Lambda expression missing '('");
                }

                expectToMove('(');
                List<String> parameters = new ArrayList<>();

                skipWhitespace();
                while (peek() != ')') {
                    parameters.add(parseIdentifier());
                    skipWhitespace();
                    if (peek() == ',') {
                        advance();
                        skipWhitespace();
                    } else if (peek() != ')') {
                        throw new RuntimeException("Error parsing lambda argument list, expected ',' or ')'");
                    }
                }

                expectToMove(')');
                skipWhitespace();

                if (!matchKeyword("->")) {
                    throw new RuntimeException("Lambda expression missing '->'");
                }

                skipWhitespace();

                ASTNode body;
                if (peek() == '{') {
                    body = parseBlock();
                } else {
                    body = parseExpression();
                }

                return new LambdaNode(parameters, body, "Lambda");

            } catch (RuntimeException e) {
                failure = true;
                throw e;
            } finally {
                if (failure) {
                    restorePosition();
                } else {
                    releasePosition();
                }
            }
        }

        private ASTNode parseExpression() {
            ASTNode expr = parseAssignment();

            // 检查是否是直接函数调用：expr(args)
            skipWhitespace();
            if (peek() == '(') {
                // 这是一个直接函数调用
                expectToMove('(');
                List<ASTNode> args = parseArguments();
                expectToMove(')');

                // 根据expr的类型决定如何调用
                return new DirectFunctionCallNode(expr, args);
            }

            return expr;
        }

        private ASTNode parseAssignment() {
            savePosition();
            boolean failure = false;
            try {
                // 解析左侧表达式（可能是变量或其他表达式）
                ASTNode left = parseTernary();
                skipWhitespace();

                // 是否是赋值操作
                if (peek() == '=') {
                    advance();
                    skipWhitespace();
                    if (left instanceof VariableNode) {
                        String varName = ((VariableNode) left).name;
                        ASTNode right = parseAssignment(); // 递归解析右侧表达式
                        return new VariableAssignmentNode(varName, right);
                    } else if (left instanceof FieldAccessNode fieldAccess) {
                        // 字段赋值：obj.field = value
                        ASTNode right = parseAssignment(); // 递归解析右侧表达式
                        return new FieldAssignmentNode(fieldAccess.target, fieldAccess.fieldName, right);
                    } else if (left instanceof ArrayAccessNode arrayAccess) {
                        // 数组赋值：arr[index] = value
                        ASTNode right = parseAssignment(); // 递归解析右侧表达式
                        return new ArrayAssignmentNode(arrayAccess.target, arrayAccess.index, right);
                    } else {
                        throw new RuntimeException(
                                "Left side of assignment must be a variable, field access, or array access");
                    }
                } else {
                    String assignmentOp = null;
                    String nextTwo = peek(2);
                    String nextThree = peek(3);
                    String nextFour = peek(4);

                    if (nextTwo.equals("+=")) {
                        assignmentOp = "+";
                        advance(2);
                    } else if (nextTwo.equals("-=")) {
                        assignmentOp = "-";
                        advance(2);
                    } else if (nextTwo.equals("*=")) {
                        assignmentOp = "*";
                        advance(2);
                    } else if (nextTwo.equals("/=")) {
                        assignmentOp = "/";
                        advance(2);
                    } else if (nextTwo.equals("%=")) {
                        assignmentOp = "%";
                        advance(2);
                    } else if (nextTwo.equals("^=")) {
                        assignmentOp = "^";
                        advance(2);
                    } else if (nextTwo.equals("&=")) {
                        assignmentOp = "&";
                        advance(2);
                    } else if (nextTwo.equals("|=")) {
                        assignmentOp = "|";
                        advance(2);
                    } else if (nextThree.equals("<<=")) {
                        assignmentOp = "<<";
                        advance(3);
                    } else if (nextThree.equals(">>=")) {
                        assignmentOp = ">>";
                        advance(3);
                    } else if (nextFour.equals(">>>=")) {
                        assignmentOp = ">>>";
                        advance(4);
                    }

                    if (assignmentOp != null) {
                        skipWhitespace();
                        if (left instanceof VariableNode) {
                            String varName = ((VariableNode) left).name;
                            ASTNode right = parseAssignment();
                            return new VariableAssignmentNode(varName,
                                    new BinaryOperatorNode(assignmentOp, left, right));
                        } else if (left instanceof ArrayAccessNode arrayAccess) {
                            ASTNode right = parseAssignment();
                            return new ArrayAssignmentNode(arrayAccess.target, arrayAccess.index,
                                    new BinaryOperatorNode(assignmentOp, left, right));
                        } else {
                            logger.error("赋值操作的左侧不是变量，是" + left.getClass().getName());
                            throw new RuntimeException("Left side of assignment must be a variable or array access");
                        }
                    }
                }
                return left;
            } catch (RuntimeException e) {
                failure = true;
                logger.warn("无法解析赋值或者表达式: " + e.getMessage());
                throw e;
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private ASTNode parseIfStatement() {
            skipWhitespace();
            savePosition();
            boolean failure = false;
            try {
                expectWordToMove("if");
                skipWhitespace();
                expectToMove('(');
                ASTNode condition = parseExpression();
                skipWhitespace();
                expectToMove(')');
                ASTNode thenBlock;

                // 首先检查是否有块结构 { }
                skipWhitespace();
                if (peek() == '{') {
                    thenBlock = parseBlock();
                } else {
                    // 如果没有块结构，尝试解析单条语句
                    thenBlock = parseCompletedStatement();
                }

                skipWhitespace();
                ASTNode elseBlock = null;
                if (matchKeyword("else")) {
                    skipWhitespace();
                    if (peek() == '{') {
                        elseBlock = parseBlock();
                    } else {
                        elseBlock = parseCompletedStatement();
                    }
                }
                return new IfNode(condition, thenBlock, elseBlock);
            } catch (RuntimeException e) {
                failure = true;
                logger.warn("无法解析if语句: " + e.getMessage());
                throw new RuntimeException("Invalid if statement: " + e.getMessage());
            } finally {
                if (failure)
                    restorePosition();
                else
                    releasePosition();
            }
        }

        private List<ASTNode> parseArguments() {
            List<ASTNode> args = new ArrayList<>();
            skipWhitespace();

            if (peek() == '(')
                advance();
            if (peek() == ')')
                return args;

            while (true) {
                ASTNode expr = parseExpression();
                args.add(expr);
                skipWhitespace();
                if (peek() == ')')
                    break;

                if (peek() != ',') {
                    char next = peek();
                    if (next == ')') {
                        break;
                    }
                    if (next == '"' || Character.isLetterOrDigit(next) || next == '(' || next == '[') {
                        logger.warn("警告: 参数列表中位置" + position + "可能缺少逗号");
                        continue;
                    }
                    throw new RuntimeException("Expected ',' or ')' in arguments, found '" + next + "'");
                }

                advance();
                skipWhitespace();
            }

            return args;
        }

        private ASTNode parseTernary() {
            ASTNode condition = parseLogicalOr();
            skipWhitespace();
            if (match('?')) {
                skipWhitespace();
                ASTNode thenExpr = parseTernary();
                skipWhitespace();
                expectToMove(':');
                ASTNode elseExpr = parseTernary();
                return new TernaryExprNode(condition, thenExpr, elseExpr);
            }

            return condition;
        }

        private ASTNode parseLogicalOr() {
            ASTNode left = parseLogicalAnd();
            skipWhitespace();
            while (matchKeyword("||")) {
                skipWhitespace();
                ASTNode right = parseLogicalAnd();
                left = new BinaryOperatorNode("||", left, right);
            }

            return left;
        }

        private ASTNode parseLogicalAnd() {
            ASTNode left = parseBitwiseOr();
            skipWhitespace();
            while (matchKeyword("&&")) {
                skipWhitespace();
                ASTNode right = parseBitwiseOr();
                left = new BinaryOperatorNode("&&", left, right);
            }
            return left;
        }

        private ASTNode parseBitwiseOr() {
            ASTNode left = parseBitwiseXor();
            skipWhitespace();
            while (isTargetWord("|") && !isTargetWord("|=") && !isTargetWord("||")) {
                expectToMove('|');
                skipWhitespace();
                ASTNode right = parseBitwiseXor();
                left = new BinaryOperatorNode("|", left, right);
            }
            return left;
        }

        private ASTNode parseBitwiseXor() {
            ASTNode left = parseBitwiseAnd();
            skipWhitespace();
            while (isTargetWord("^") && !isTargetWord("^=")) {
                expectToMove('^');
                skipWhitespace();
                ASTNode right = parseBitwiseAnd();
                left = new BinaryOperatorNode("^", left, right);
            }
            return left;
        }

        private ASTNode parseBitwiseAnd() {
            ASTNode left = parseEquality();
            skipWhitespace();
            while (isTargetWord("&") && !isTargetWord("&=") && !isTargetWord("&&")) {
                expectToMove('&');
                skipWhitespace();
                ASTNode right = parseEquality();
                left = new BinaryOperatorNode("&", left, right);
                releasePosition();
                savePosition();
            }
            return left;
        }

        private ASTNode parseEquality() {
            ASTNode left = parseRelational();
            skipWhitespace();
            while (true) {
                skipWhitespace();
                if (matchKeyword("instanceof")) {
                    ASTNode right = parseRelational();
                    left = new BinaryOperatorNode("instanceof", left, right);
                } else if (matchKeyword("==")) {
                    ASTNode right = parseRelational();
                    left = new BinaryOperatorNode("==", left, right);
                } else if (matchKeyword("!=")) {
                    ASTNode right = parseRelational();
                    left = new BinaryOperatorNode("!=", left, right);
                } else {
                    break;
                }
            }
            return left;
        }

        private ASTNode parseRelational() {
            ASTNode left = parseBitwiseShift();
            skipWhitespace();
            while (true) {
                skipWhitespace();
                if (match('<')) {
                    if (match('=')) {
                        ASTNode right = parseBitwiseShift();
                        left = new BinaryOperatorNode("<=", left, right);
                    } else {
                        ASTNode right = parseBitwiseShift();
                        left = new BinaryOperatorNode("<", left, right);
                    }
                } else if (match('>')) {
                    if (match('=')) {
                        ASTNode right = parseBitwiseShift();
                        left = new BinaryOperatorNode(">=", left, right);
                    } else {
                        ASTNode right = parseBitwiseShift();
                        left = new BinaryOperatorNode(">", left, right);
                    }
                } else {
                    break;
                }
            }

            return left;
        }

        private ASTNode parseBitwiseShift() {
            ASTNode left = parseAdditive();
            skipWhitespace();
            while (true) {
                skipWhitespace();
                if (isTargetWord("<<") && !isTargetWord("<<=")) {
                    expectWordToMove("<<");
                    ASTNode right = parseAdditive();
                    left = new BinaryOperatorNode("<<", left, right);
                } else if (isTargetWord(">>") && !isTargetWord(">>=")) {
                    expectWordToMove(">>");
                    ASTNode right = parseAdditive();
                    left = new BinaryOperatorNode(">>", left, right);
                } else {
                    break;
                }
            }

            return left;

        }

        private ASTNode parseAdditive() {
            ASTNode left = parseMultiplicative();
            skipWhitespace();
            while (true) {
                skipWhitespace();
                if (isTargetWord("+") && !isTargetWord("+=")) {
                    expectToMove('+');
                    ASTNode right = parseMultiplicative();
                    left = new BinaryOperatorNode("+", left, right);
                } else if (isTargetWord("-") && !isTargetWord("-=")) {
                    expectToMove('-');
                    ASTNode right = parseMultiplicative();
                    left = new BinaryOperatorNode("-", left, right);
                } else {
                    break;
                }
            }

            return left;
        }

        private ASTNode parseMultiplicative() {
            ASTNode left = parseUnary();
            skipWhitespace();
            while (true) {
                skipWhitespace();
                if (isTargetWord("*") && !isTargetWord("*=")) {
                    expectToMove('*');
                    ASTNode right = parseUnary();
                    left = new BinaryOperatorNode("*", left, right);
                } else if (isTargetWord("/") && !isTargetWord("/=")) {
                    expectToMove('/');
                    ASTNode right = parseUnary();
                    left = new BinaryOperatorNode("/", left, right);
                } else if (isTargetWord("%") && !isTargetWord("%=")) {
                    expectToMove('%');
                    ASTNode right = parseUnary();
                    left = new BinaryOperatorNode("%", left, right);
                } else {
                    break;
                }
            }
            return left;
        }

        private ASTNode parseUnary() {
            skipWhitespace();
            if (matchKeyword("++")) {
                ASTNode operand = parseUnary();
                if (operand instanceof VariableNode) {
                    return new IncrementNode(((VariableNode) operand).name, true, true);
                } else if (operand instanceof FieldAccessNode fieldAccess) {
                    return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, true, true);
                } else {
                    logger.error("前置++只能用在变量或字段访问上");
                    throw new RuntimeException("++ can only be applied to variables or field access");
                }
            } else if (matchKeyword("--")) {
                ASTNode operand = parseUnary();
                if (operand instanceof VariableNode) {
                    return new IncrementNode(((VariableNode) operand).name, true, false);
                } else if (operand instanceof FieldAccessNode fieldAccess) {
                    return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, true, false);
                } else {
                    logger.error("前置--只能用在变量或字段访问上");
                    throw new RuntimeException("-- can only be applied to variables or field access");
                }
            } else if (match('+')) {
                return parseUnary();
            } else if (match('-')) {
                ASTNode operand = parseUnary();
                return new BinaryOperatorNode("-", new LiteralNode(0), operand);
            } else if (match('!')) {
                ASTNode operand = parseUnary();
                return new BinaryOperatorNode("!", operand, null);
            } else if (match('~')) {
                ASTNode operand = parseUnary();
                return new BinaryOperatorNode("~", operand, null);
            }

            ASTNode result = parseLiteral();

            if (matchKeyword("++")) {
                if (result instanceof ParenthesizedExpressionNode paren) {
                    if (paren.expression instanceof VariableNode) {
                        return new IncrementNode(((VariableNode) paren.expression).name, false, true);
                    } else if (paren.expression instanceof FieldAccessNode fieldAccess) {
                        return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, false, true);
                    }
                    logger.error("后置++不能用在括号表达式上");
                    throw new RuntimeException(
                            "++ can only be applied to variables or field access, not parenthesized expressions");
                } else if (result instanceof VariableNode) {
                    return new IncrementNode(((VariableNode) result).name, false, true);
                } else if (result instanceof FieldAccessNode fieldAccess) {
                    return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, false, true);
                } else {
                    logger.error("后置++只能用在变量或字段访问上");
                    throw new RuntimeException("++ can only be applied to variables or field access");
                }
            } else if (matchKeyword("--")) {
                if (result instanceof ParenthesizedExpressionNode paren) {
                    if (paren.expression instanceof VariableNode) {
                        return new IncrementNode(((VariableNode) paren.expression).name, false, false);
                    } else if (paren.expression instanceof FieldAccessNode fieldAccess) {
                        return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, false, false);
                    }
                    logger.error("后置--不能用在括号表达式上");
                    throw new RuntimeException(
                            "-- can only be applied to variables or field access, not parenthesized expressions");
                } else if (result instanceof VariableNode) {
                    return new IncrementNode(((VariableNode) result).name, false, false);
                } else if (result instanceof FieldAccessNode fieldAccess) {
                    return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, false, false);
                } else {
                    logger.error("后置--只能用在变量或字段访问上");
                    throw new RuntimeException("-- can only be applied to variables or field access");
                }
            }

            return result;
        }

        /**
         * 解析字段声明
         */
        private FieldDefinition parseFieldDeclaration() {
            try {
                skipWhitespace();
                String type = peekWord();
                expectWordToMove(type);
                skipWhitespace();
                String name = peekWord();
                expectWordToMove(name);
                skipWhitespace();

                if (peek() == '(') {
                    return null;
                }

                if (peek() == '=') {
                    expectToMove('=');
                    skipWhitespace();
                    ASTNode value = parseExpression();
                    if (value != null) {
                        expectToMove(';');
                        return new FieldDefinition(name, type, value);
                    }
                } else if (peek() == ';') {
                    expectToMove(';');
                    return new FieldDefinition(name, type, null);
                } else if (peek() == ',') {
                    List<String> names = new ArrayList<>();
                    names.add(name);
                    while (peek() == ',') {
                        expectToMove(',');
                        skipWhitespace();
                        String nextName = peekWord();
                        expectWordToMove(nextName);
                        names.add(nextName);
                        skipWhitespace();
                        if (peek() == '=') {
                            expectToMove('=');
                            skipWhitespace();
                            parseExpression();
                        }
                    }
                    expectToMove(';');
                    return new FieldDefinition(name, type, null);
                }
                return new FieldDefinition(name, type, null);
            } catch (Exception e) {
                logger.warn("解析字段声明失败: " + e.getMessage());
                return null;
            }
        }

        /**
         * 解析方法声明（假设当前位置在访问修饰符之后）
         */
        private MethodDefinition parseMethodDeclaration() {
            try {
                skipWhitespace();
                String returnType = peekWord();
                if (!isTypeOrVoid(returnType)) {
                    return null;
                }
                expectWordToMove(returnType);
                skipWhitespace();

                String methodName = peekWord();
                if (methodName.isEmpty() || isKeyword(methodName)) {
                    return null;
                }
                expectWordToMove(methodName);
                skipWhitespace();

                expectToMove('(');
                skipWhitespace();

                List<Parameter> parameters = new ArrayList<>();
                while (peek() != ')') {
                    char currentChar = peek();
                    if (currentChar == ',' || currentChar == ')') {
                        throw new RuntimeException("Method parameter cannot be empty, position: " + position
                                + ", current character: '" + currentChar + "'");
                    }

                    String paramType = peekWord();
                    if (paramType.isEmpty()) {
                        throw new RuntimeException("Method parameter type cannot be empty, position: " + position);
                    }
                    expectWordToMove(paramType);
                    skipWhitespace();
                    String paramName = peekWord();
                    if (paramName.isEmpty()) {
                        throw new RuntimeException("Method parameter name cannot be empty, position: " + position);
                    }
                    expectWordToMove(paramName);
                    parameters.add(new Parameter(paramType, paramName));
                    skipWhitespace();
                    if (peek() == ',') {
                        expectToMove(',');
                        skipWhitespace();
                    }
                }

                expectToMove(')');
                skipWhitespace();
                expectToMove('{');
                skipWhitespace();
                // TODO: 用BlockNode自带的解析

                BlockNode methodBody = new BlockNode();
                while (peek() != '}' && position < input.length()) {
                    int stmtStart = position;
                    ASTNode stmt = parseCompletedStatement();
                    if (stmt != null) {
                        methodBody.addStatement(stmt);
                        skipWhitespace();
                        if (position == stmtStart) {
                            logger.error("解释器在解析方法时开始原地踏步");
                            throw new RuntimeException(
                                    "Cannot parse statement in method definition; are there any syntax error?");
                        }
                    } else {
                        logger.error("解析器在解析方法时parseCompletedStatement返回null");
                        unexpectedToken();
                    }
                    skipWhitespace();
                }

                expectToMove('}');

                return new MethodDefinition(methodName, returnType, parameters, methodBody);
            } catch (Exception e) {
                logger.warn("解析方法声明失败: " + e.getMessage());
                return null;
            }
        }

        /**
         * 解析方法声明（带已知类型）
         */
        private MethodDefinition parseMethodDeclarationWithType(String returnType) {
            try {
                expectWordToMove(returnType);
                skipWhitespace();

                String methodName = peekWord();
                if (methodName.isEmpty() || isKeyword(methodName)) {
                    return null;
                }
                expectWordToMove(methodName);
                skipWhitespace();

                expectToMove('(');
                skipWhitespace();

                List<Parameter> parameters = new ArrayList<>();
                while (peek() != ')') {
                    char currentChar = peek();
                    if (currentChar == ',' || currentChar == ')') {
                        throw new RuntimeException("Method parameter cannot be empty, position: " + position
                                + ", current character: '" + currentChar + "'");
                    }

                    String paramType = peekWord();
                    if (paramType.isEmpty()) {
                        throw new RuntimeException("Method parameter type cannot be empty, position: " + position);
                    }
                    expectWordToMove(paramType);
                    skipWhitespace();
                    String paramName = peekWord();
                    if (paramName.isEmpty()) {
                        throw new RuntimeException("Method parameter name cannot be empty, position: " + position);
                    }
                    expectWordToMove(paramName);
                    parameters.add(new Parameter(paramType, paramName));
                    skipWhitespace();
                    if (peek() == ',') {
                        expectToMove(',');
                        skipWhitespace();
                    }
                }

                expectToMove(')');
                skipWhitespace();
                expectToMove('{');
                skipWhitespace();

                BlockNode methodBody = new BlockNode();
                while (peek() != '}' && position < input.length()) {
                    ASTNode stmt = parseCompletedStatement();
                    if (stmt != null) {
                        methodBody.addStatement(stmt);
                    }
                    skipWhitespace();
                }

                expectToMove('}');

                return new MethodDefinition(methodName, returnType, parameters, methodBody);
            } catch (Exception e) {
                logger.warn("解析方法声明失败: " + e.getMessage());
                return null;
            }
        }

        private String peekWordAfterWhitespace() {
            int savedPosition = position;
            skipWhitespace();
            String word = peekWord();
            position = savedPosition;
            return word;
        }

        private char peekAfterWord(String word) {
            int savedPosition = position;
            expectWordToMove(word);
            skipWhitespace();
            char next = peek();
            position = savedPosition;
            return next;
        }

        /**
         * 解析构造函数定义
         *
         * @param className            类名
         * @param expectAccessModifier 是否期望解析访问修饰符（false表示访问修饰符已经被解析过了）
         */
        private ConstructorDefinition parseConstructorDefinition(String className, boolean expectAccessModifier) {
            skipWhitespace();
            int savedPos = position;
            if (expectAccessModifier) {
                savePosition();
            }

            try {
                // 解析访问修饰符（可选）
                String accessModifier = "public"; // 默认public
                if (expectAccessModifier) {
                    if (isTargetWord("public")) {
                        expectWordToMove("public");
                        accessModifier = "public";
                    } else if (isTargetWord("private")) {
                        expectWordToMove("private");
                        accessModifier = "private";
                    } else if (isTargetWord("protected")) {
                        expectWordToMove("protected");
                        accessModifier = "protected";
                    }
                    skipWhitespace();
                }

                // 构造函数名应该与类名相同
                String constructorName = peekWord();
                if (!constructorName.equals(className)) {
                    if (expectAccessModifier) {
                        restorePosition();
                    } else {
                        position = savedPos;
                    }
                    return null;
                }
                expectWordToMove(constructorName);
                skipWhitespace();

                // 解析参数列表
                expectToMove('(');
                skipWhitespace();

                List<Parameter> parameters = new ArrayList<>();
                while (peek() != ')') {
                    char currentChar = peek();
                    if (currentChar == ',' || currentChar == ')') {
                        throw new RuntimeException("Constructor parameter cannot be empty, position: " + position
                                + ", current character: '" + currentChar + "'");
                    }

                    // 解析参数类型和名称 - 使用peekWord而不是parseClassIdentifier以支持原始类型
                    int paramStartPos = position;
                    String paramType = peekWord();
                    if (paramType.isEmpty()
                            || (!TYPES.contains(paramType) && !Character.isUpperCase(paramType.charAt(0)))) {
                        throw new RuntimeException(
                                "Invalid parameter type at position " + paramStartPos + ": " + paramType);
                    }
                    expectWordToMove(paramType);
                    skipWhitespace();
                    String paramName = peekWord();
                    if (paramName.isEmpty()) {
                        throw new RuntimeException("Constructor parameter name cannot be empty, position: " + position);
                    }
                    expectWordToMove(paramName);
                    parameters.add(new Parameter(paramType, paramName));

                    skipWhitespace();
                    if (peek() == ',') {
                        expectToMove(',');
                        skipWhitespace();
                    }
                }

                expectToMove(')');
                skipWhitespace();

                // 解析构造函数体
                expectToMove('{');
                skipWhitespace();

                // 解析构造函数体中的语句序列
                BlockNode constructorBody = new BlockNode();
                if (peek() != '}') {
                    // 解析构造函数体中的所有语句
                    while (peek() != '}' && position < input.length()) {
                        int stmtStart = position;
                        ASTNode stmt = parseCompletedStatement();
                        if (stmt != null) {
                            constructorBody.addStatement(stmt);
                            skipWhitespace();
                            if (position == stmtStart) {
                                logger.error("解释器在解析构造函数时开始原地踏步");
                                throw new RuntimeException("Cannot parse statement; are there any syntax error?");
                            }
                        } else {
                            logger.error("解释器在解析构造函数时parseCompletedStatement解析出null");
                            unexpectedToken();
                        }
                        skipWhitespace();
                    }
                }

                expectToMove('}');

                return new ConstructorDefinition(constructorName, parameters, constructorBody);

            } catch (RuntimeException e) {
                if (expectAccessModifier) {
                    restorePosition();
                }
                logger.warn("解析构造函数定义失败: " + e.getMessage());
                return null;
            }
        }
    }

    public static class ScriptRunner {
        private ExecutionContext context;

        public ScriptRunner(ClassLoader classLoader) {
            this.context = new ExecutionContext(classLoader);
        }

        public ExecutionContext getContext() {
            return context;
        }

        public ExecutionContext setContext(ExecutionContext context) {
            this.context = context;
            return context;
        }

        public Object executeWithResult(String code) {
            context.clearOutput();
            context.clearWarnMessages();
            Parser parser = new Parser(code, context);
            ASTNode node;
            Object lastResult = null;

            try {
                while ((node = parser.parseCompletedStatement()) != null)
                    lastResult = node.evaluate(context);
            } catch (Exception e) {
                System.err.println(getStackTraceString(e));
            }
            return lastResult;
        }

        public void execute(String code) {
            context.clearOutput();
            context.clearWarnMessages();
            Parser parser = new Parser(code, context);
            ASTNode node;
            try {
                while ((node = parser.parseCompletedStatement()) != null) {
                    node.evaluate(context);
                }
            } catch (Exception e) {
                System.err.println(getStackTraceString(e));
            }
        }

        public void execute(String code,
                IOutputHandler builtInOutStream,
                IOutputHandler builtInErrStream) {
            context.setBuiltInOutputBuffer(builtInOutStream);
            context.setBuiltInErrorBuffer(builtInErrStream);
            // context.clearVariables();
            context.clearOutput();
            context.clearWarnMessages();
            Parser parser = new Parser(code, context);
            ASTNode node;
            try {
                while ((node = parser.parseCompletedStatement()) != null) {
                    node.evaluate(context);
                }
            } catch (Exception e) {
                System.err.println(getStackTraceString(e));
            }
        }

        public Map<String, Object> getAllVariablesAsObject() {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<String, Variable> entry : context.getAllVariables().entrySet()) {
                result.put(entry.getKey(), entry.getValue().value);
            }
            return result;
        }

        public void clearVariables() {
            context.clearVariables();
        }

        public List<ASTNode> tryEvaluate(String code) {
            context.clearOutput();
            context.clearWarnMessages();
            Parser parser = new Parser(code, context);
            List<ASTNode> nodes = new ArrayList<>();
            ASTNode node;

            try {
                while ((node = parser.parseCompletedStatement()) != null) {
                    nodes.add(node);
                }
            } catch (Exception e) {
                logger.warn("解析代码时出错: " + e.getMessage());
                return nodes;
            }
            return nodes;
        }

    }

    public static void main(String[] args) {
        TestInterpreter.ScriptRunner runner = new ScriptRunner(Thread.currentThread().getContextClassLoader());
        while (true) {
            String code;
            try {
                System.out.print(">>> ");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                code = reader.readLine();
                if (code == null || code.trim().equals("exit")) {
                    break;
                }
                Object result = runner.executeWithResult(code);
                if (result != null) {
                    System.out.println(result);
                }
            } catch (Exception e) {
                System.err.println("执行出错: " + e.getMessage());
                System.err.println(getStackTraceString(e));
            }
        }
    }

}
