package com.justnothing.testmodule.command.functions.watch;

import com.justnothing.testmodule.command.functions.script.TestInterpreter.CustomClassException;
import com.justnothing.testmodule.hooks.XposedBasicHook;
import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class WatchTask implements Runnable {

    public enum WatchType {
        FIELD,
        METHOD
    }

    public static class WatchLogger extends Logger {
        @Override
        public String getTag() {
            return "WatchTask";
        }
    }

    private static final WatchLogger logger = new WatchLogger();
    private final int id;
    private final WatchType type;
    private final Class<?> targetClass;
    private final String memberName;
    private final String signature;
    private final long interval;
    private final int maxOutputSize;
    private final AtomicBoolean running;
    private final AtomicInteger outputCount;
    private final LinkedList<String> outputBuffer;
    private final ClassLoader classLoader;
    private final List<Method> targetMethods = new ArrayList<>();
    private final List<XC_MethodHook.Unhook> methodHooks = new ArrayList<>();
    private Object lastValue;
    private Field targetField;
    private List<String> imports = new ArrayList<>(Arrays.asList("java.lang.*", "java.util.*"));

    public WatchTask(int id, WatchType type, Class<?> targetClass, String memberName, String signature, long interval,
            int maxOutputSize, ClassLoader classLoader) {
        this.id = id;
        this.type = type;
        this.targetClass = targetClass;
        this.memberName = memberName;
        this.signature = signature;
        this.interval = interval;
        this.maxOutputSize = maxOutputSize;
        this.running = new AtomicBoolean(false);
        this.outputCount = new AtomicInteger(0);
        this.outputBuffer = new LinkedList<>();
        this.classLoader = classLoader;

        try {
            if (type == WatchType.FIELD) {
                logger.debug("查找字段: " + memberName + " 在类 " + targetClass.getName());
                this.targetField = targetClass.getDeclaredField(memberName);
                this.targetField.setAccessible(true);
                if (Modifier.isStatic(this.targetField.getModifiers())) {
                    this.lastValue = this.targetField.get(null);
                    logger.debug("字段初始值: " + lastValue);
                }
            } else if (type == WatchType.METHOD) {
                logger.debug("查找方法: " + memberName + " 在类 " + targetClass.getName()
                        + (signature != null ? " (签名: " + signature + ")" : ""));
                this.targetMethods.addAll(Arrays.asList(findMethod(targetClass, memberName, signature)));
                for (Method method : this.targetMethods) {
                    method.setAccessible(true);
                }
                logger.debug("找到方法: " + targetMethods);
            }
        } catch (Exception e) {
            logger.error("初始化watch任务失败", e);
            String errorMsg = "初始化watch任务失败: " + e.getClass().getSimpleName() + ": " + e.getMessage();
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
        // TODO: 支持泛型（从TestInterpreter复制来的，不过这个TODO大概率是要被鸽掉了）
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
            parts[i] = parts[i].trim(); // TODO: 虽然没用，真要处理空格等以后改吧
        }
        Class<?>[] types = new Class<?>[parts.length];
        for (int i = 0; i < parts.length; i++) {
            types[i] = findClassInternal(parts[i].trim());
        }
        return types;
    }

    private Method[] findMethod(Class<?> clazz, String methodName, String signature)
            throws NoSuchMethodException, ClassNotFoundException {
        WatchLogger logger = new WatchLogger();
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
        } else {

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
        WatchLogger logger = new WatchLogger();
        logger.info("Watch任务 " + id + " 开始运行");

        try {
            if (type == WatchType.METHOD) {
                hookMethod();
            }

            while (running.get()) {
                try {
                    if (type == WatchType.FIELD) {
                        monitorField();
                    }
                } catch (Exception e) {
                    addOutput("监控出错: " + e.getMessage());
                    logger.error("监控出错", e);
                }

                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    logger.info("Watch任务 " + id + " 被中断");
                    break;
                }
            }
        } finally {
            cleanup();
            running.set(false);
            logger.info("Watch任务 " + id + " 已停止");
        }
    }

    private void hookMethod() {
        try {
            WatchLogger logger = new WatchLogger();
            logger.info("Hook方法: " + targetClass.getName() + "." + memberName);

            for (Method method : targetMethods) {
                Class<?>[] paramTypes = method.getParameterTypes();
                XposedHelpers.findAndHookMethod(targetClass, memberName, paramTypes, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
                        String output = String.format("[%s] 方法 %s.%s 被调用",
                                timestamp,
                                targetClass.getSimpleName(),
                                memberName);
                        addOutput(output);

                        if (param.args.length > 0) {
                            StringBuilder argsStr = new StringBuilder("  参数: ");
                            for (int i = 0; i < param.args.length; i++) {
                                argsStr.append(param.args[i] != null ? param.args[i].toString() : "null");
                                if (i < param.args.length - 1)
                                    argsStr.append(", ");
                            }
                            addOutput(argsStr.toString());
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
                        Object result = param.getResult();
                        String output = String.format("[%s] 方法 %s.%s 返回: %s",
                                timestamp,
                                targetClass.getSimpleName(),
                                memberName,
                                result != null ? result.toString() : "void");
                        addOutput(output);
                    }
                });
                logger.info("方法hook成功: " + method);
            }
        } catch (Exception e) {
            WatchLogger logger = new WatchLogger();
            logger.error("Hook方法失败: " + memberName, e);
            addOutput("警告: Hook方法失败，将使用轮询模式: " + e.getMessage());
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
                WatchLogger logger = new WatchLogger();
                logger.info("取消方法hook完成: " + memberName);
            } catch (Exception e) {
                WatchLogger logger = new WatchLogger();
                logger.error("取消方法hook失败", e);
            }
        }
    }

    private void monitorField() throws IllegalAccessException {
        if (targetField == null)
            return;

        Object currentValue;
        if (java.lang.reflect.Modifier.isStatic(targetField.getModifiers())) {
            currentValue = targetField.get(null);
        } else {
            addOutput("警告: 非静态字段，无法监控值变化");
            return;
        }

        if (lastValue == null || !lastValue.equals(currentValue)) {
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
            String output = String.format("[%s] %s.%s: %s -> %s",
                    timestamp,
                    targetClass.getSimpleName(),
                    memberName,
                    lastValue,
                    currentValue);
            addOutput(output);
            lastValue = currentValue;
        }
    }

    private void addOutput(String output) {
        synchronized (outputBuffer) {
            outputBuffer.addLast(output);
            if (outputBuffer.size() > maxOutputSize) {
                outputBuffer.removeFirst();
            }
            outputCount.incrementAndGet();
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

    public WatchType getType() {
        return type;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public String getMemberName() {
        return memberName;
    }

    public long getInterval() {
        return interval;
    }

    public int getOutputCount() {
        return outputCount.get();
    }

    public LinkedList<String> getOutputBuffer() {
        synchronized (outputBuffer) {
            return new LinkedList<>(outputBuffer);
        }
    }

    public String getOutput(int limit) {
        synchronized (outputBuffer) {
            if (outputBuffer.isEmpty()) {
                return "暂无输出";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== Watch ").append(id).append(" 输出 (最近").append(limit).append("条) ===\n");

            int startIndex = Math.max(0, outputBuffer.size() - limit);
            for (int i = startIndex; i < outputBuffer.size(); i++) {
                sb.append(outputBuffer.get(i)).append("\n");
            }

            sb.append("总计: ").append(outputCount.get()).append(" 条记录\n");
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return String.format("Watch[%d] %s.%s%s (%s, 间隔=%dms, 输出=%d条)",
                id,
                targetClass.getSimpleName(),
                memberName,
                (type == WatchType.METHOD && signature != null) ? "(" + signature + ")" : "",
                type == WatchType.FIELD ? "字段" : "方法",
                interval,
                outputCount.get());
    }
}
