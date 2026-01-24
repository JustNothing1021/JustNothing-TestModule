package com.justnothing.testmodule.command.functions.trace;

import com.justnothing.testmodule.hooks.XposedBasicHook;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class TraceTask implements Runnable {

    public static class TraceLogger extends Logger {
        @Override
        public String getTag() {
            return "TraceTask";
        }
    }

    private static final TraceLogger logger = new TraceLogger();
    private final int id;
    private final Class<?> targetClass;
    private final String methodName;
    private final String signature;
    private final int maxCallRecords;
    private final AtomicBoolean running;
    private final AtomicInteger callCount;
    private final LinkedList<CallRecord> callRecords;
    private final ClassLoader classLoader;
    private final List<Method> targetMethods = new ArrayList<>();
    private final List<XC_MethodHook.Unhook> methodHooks = new ArrayList<>();
    private final Map<String, CallNode> callTree = new HashMap<>();
    private List<String> imports = new ArrayList<>(Arrays.asList("java.lang.*", "java.util.*"));

    public TraceTask(int id, Class<?> targetClass, String methodName, String signature,
            int maxCallRecords, ClassLoader classLoader) {
        this.id = id;
        this.targetClass = targetClass;
        this.methodName = methodName;
        this.signature = signature;
        this.maxCallRecords = maxCallRecords;
        this.running = new AtomicBoolean(false);
        this.callCount = new AtomicInteger(0);
        this.callRecords = new LinkedList<>();
        this.classLoader = classLoader;

        try {
            logger.debug("查找方法: " + methodName + " 在类 " + targetClass.getName()
                    + (signature != null ? " (签名: " + signature + ")" : ""));
            this.targetMethods.addAll(Arrays.asList(findMethod(targetClass, methodName, signature)));
            for (Method method : this.targetMethods) {
                method.setAccessible(true);
            }
            logger.debug("找到方法: " + targetMethods);
        } catch (Exception e) {
            logger.error("初始化trace任务失败", e);
            String errorMsg = "初始化trace任务失败: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            if (e.getCause() != null) {
                errorMsg += "\n原因: " + e.getCause().getMessage();
            }
            throw new RuntimeException(errorMsg, e);
        }
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

    public Class<?> findClassInternal(String className) throws ClassNotFoundException {
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

        Class<?> clazz;
        if (className.contains(".")) {
            logger.debug("尝试完整类名: " + className);
            clazz = findClassThroughApi(className, classLoader);

            if (clazz != null) {
                logger.debug("通过完整类名找到类: " + clazz.getName());
                return clazz;
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

    private static boolean isPrimitiveWrapperMatch(Class<?> target, Class<?> source) {
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

    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, Class<?>[] usingArgTypes) {
        return isApplicableArgs(methodArgsTypes, Arrays.asList(usingArgTypes));
    }

    public Class<?>[] getSignatureFromString(String signature) throws ClassNotFoundException {
        if (signature == null || signature.isEmpty()) {
            return new Class<?>[0];
        }
        String[] parts = signature.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        Class<?>[] types = new Class<?>[parts.length];
        for (int i = 0; i < parts.length; i++) {
            types[i] = findClassInternal(parts[i].trim());
        }
        return types;
    }

    private Method[] findMethod(Class<?> clazz, String methodName, String signature)
            throws NoSuchMethodException, ClassNotFoundException {
        TraceLogger logger = new TraceLogger();
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
                return findMethod(superClass, methodName, signature);
            }

            String availableMethods = "可用方法:\n";
            for (Method m : clazz.getDeclaredMethods()) {
                availableMethods += "  " + m + "\n";
            }
            logger.error("未找到方法 " + methodName + "\n" + availableMethods);
            throw new NoSuchMethodException(
                    "未找到方法: " + methodName + " 在类 " + clazz.getName() + "\n" + availableMethods);
        }

        if (signature == null || signature.isEmpty()) {
            logger.debug("未指定签名，返回所有方法");
            return candidates.toArray(new Method[0]);
        }

        for (Method method : candidates) {
            if (isApplicableArgs(method.getParameterTypes(), getSignatureFromString(signature))) {
                return new Method[] { method };
            }
        }

        String errorMsg = "未找到匹配签名的方法: " + methodName + " (签名: " + signature + ")\n";
        errorMsg += "候选方法:\n";
        for (Method m : candidates) {
            errorMsg += "  " + m + "\n";
        }
        logger.error(errorMsg);
        throw new NoSuchMethodException(errorMsg);
    }

    @Override
    public void run() {
        running.set(true);
        TraceLogger logger = new TraceLogger();
        logger.info("Trace任务 " + id + " 开始运行");

        try {
            hookMethod();
            
            while (running.get()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.info("Trace任务 " + id + " 被中断");
                    break;
                }
            }
        } finally {
            cleanup();
            running.set(false);
            logger.info("Trace任务 " + id + " 已停止");
        }
    }

    private void hookMethod() {
        try {
            TraceLogger logger = new TraceLogger();
            logger.info("Hook方法: " + targetClass.getName() + "." + methodName);

            for (Method method : targetMethods) {
                Class<?>[] paramTypes = method.getParameterTypes();
                XposedHelpers.findAndHookMethod(targetClass, methodName, paramTypes, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
                        
                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                        int depth = 0;
                        for (StackTraceElement element : stackTrace) {
                            if (!element.getClassName().startsWith("com.justnothing.testmodule") &&
                                !element.getClassName().startsWith("de.robv.android.xposed")) {
                                depth++;
                            }
                        }
                        
                        CallRecord record = new CallRecord(
                            timestamp,
                            targetClass.getName(),
                            methodName,
                            depth,
                            param.args
                        );
                        
                        addCallRecord(record);
                        updateCallTree(record);
                        
                        callCount.incrementAndGet();
                    }
                });
                logger.info("方法hook成功: " + method);
            }
        } catch (Exception e) {
            TraceLogger logger = new TraceLogger();
            logger.error("Hook方法失败: " + methodName, e);
            throw new RuntimeException("Hook方法失败: " + e.getMessage(), e);
        }
    }

    private void cleanup() {
        if (methodHooks != null) {
            try {
                for (XC_MethodHook.Unhook methodHook : methodHooks) {
                    methodHook.unhook();
                    logger.info("取消hook方法" + methodHook.getHookedMethod());
                }
                methodHooks.clear();
                TraceLogger logger = new TraceLogger();
                logger.info("取消方法hook完成: " + methodName);
            } catch (Exception e) {
                TraceLogger logger = new TraceLogger();
                logger.error("取消方法hook失败", e);
            }
        }
    }

    private void addCallRecord(CallRecord record) {
        synchronized (callRecords) {
            callRecords.addLast(record);
            if (callRecords.size() > maxCallRecords) {
                callRecords.removeFirst();
            }
        }
    }

    private void updateCallTree(CallRecord record) {
        synchronized (callTree) {
            String key = record.className + "." + record.methodName;
            CallNode node = callTree.get(key);
            if (node == null) {
                node = new CallNode(record.className, record.methodName);
                callTree.put(key, node);
            }
            node.incrementCallCount();
            node.updateMaxDepth(record.depth);
        }
    }

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getId() {
        return id;
    }

    public String getClassName() {
        return targetClass.getName();
    }

    public String getMethodName() {
        return methodName;
    }

    public String getSignature() {
        return signature;
    }

    public int getCallCount() {
        return callCount.get();
    }

    public String getCallTree() {
        synchronized (callTree) {
            if (callTree.isEmpty()) {
                return "暂无调用记录";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== 调用树 ===\n");
            sb.append("总调用次数: ").append(callCount.get()).append("\n\n");
            
            for (CallNode node : callTree.values()) {
                sb.append(node.toString()).append("\n");
            }
            
            return sb.toString();
        }
    }

    public boolean exportToFile(String filePath) {
        synchronized (callRecords) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write("=== Trace 调用记录 ===\n");
                writer.write("任务ID: " + id + "\n");
                writer.write("目标方法: " + targetClass.getName() + "." + methodName + "\n");
                writer.write("签名: " + (signature != null ? signature : "所有") + "\n");
                writer.write("总调用次数: " + callCount.get() + "\n");
                writer.write("记录时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n\n");
                
                writer.write("=== 调用树 ===\n");
                for (CallNode node : callTree.values()) {
                    writer.write(node.toString() + "\n");
                }
                writer.write("\n");
                
                writer.write("=== 详细调用记录 ===\n");
                for (CallRecord record : callRecords) {
                    writer.write(record.toString() + "\n");
                }
                
                return true;
            } catch (IOException e) {
                logger.error("导出trace记录失败", e);
                return false;
            }
        }
    }

    public static class CallRecord {
        private final String timestamp;
        private final String className;
        private final String methodName;
        private final int depth;
        private final Object[] args;

        public CallRecord(String timestamp, String className, String methodName, int depth, Object[] args) {
            this.timestamp = timestamp;
            this.className = className;
            this.methodName = methodName;
            this.depth = depth;
            this.args = args;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(timestamp).append("] ");
            sb.append(className).append(".").append(methodName);
            sb.append(" (深度: ").append(depth).append(")");
            
            if (args != null && args.length > 0) {
                sb.append(" 参数: [");
                for (int i = 0; i < args.length; i++) {
                    sb.append(args[i] != null ? args[i].toString() : "null");
                    if (i < args.length - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("]");
            }
            
            return sb.toString();
        }
    }

    public static class CallNode {
        private final String className;
        private final String methodName;
        private final AtomicInteger callCount;
        private int maxDepth;

        public CallNode(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
            this.callCount = new AtomicInteger(0);
            this.maxDepth = 0;
        }

        public void incrementCallCount() {
            callCount.incrementAndGet();
        }

        public void updateMaxDepth(int depth) {
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }

        public int getCallCount() {
            return callCount.get();
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        @Override
        public String toString() {
            return String.format("%s.%s (调用次数: %d, 最大深度: %d)",
                    className, methodName, callCount.get(), maxDepth);
        }
    }
}
