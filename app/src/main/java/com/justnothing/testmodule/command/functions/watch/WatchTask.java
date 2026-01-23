package com.justnothing.testmodule.command.functions.watch;

import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
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
    private Field targetField;
    private Method targetMethod;
    private Object lastValue;
    private XC_MethodHook.Unhook methodHook;
    
    public WatchTask(int id, WatchType type, Class<?> targetClass, String memberName, String signature, long interval, int maxOutputSize) {
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
        
        WatchLogger logger = new WatchLogger();
        
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
                logger.debug("查找方法: " + memberName + " 在类 " + targetClass.getName() + (signature != null ? " (签名: " + signature + ")" : ""));
                this.targetMethod = findMethod(targetClass, memberName, signature);
                this.targetMethod.setAccessible(true);
                logger.debug("找到方法: " + targetMethod);
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
    
    private Method findMethod(Class<?> clazz, String methodName, String signature) throws NoSuchMethodException {
        WatchLogger logger = new WatchLogger();
        logger.debug("在类 " + clazz.getName() + " 中查找方法: " + methodName + (signature != null ? " (签名: " + signature + ")" : ""));
        
        Method[] methods = clazz.getDeclaredMethods();
        Method[] candidateMethods = new Method[0];
        List<Method> candidates = new java.util.ArrayList<>();
        
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                candidates.add(method);
                logger.debug("找到候选方法: " + method + " (参数: " + java.util.Arrays.toString(method.getParameterTypes()) + ")");
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
            throw new NoSuchMethodException("未找到方法: " + methodName + " 在类 " + clazz.getName() + "\n" + availableMethods);
        }
        
        if (signature == null || signature.isEmpty()) {
            logger.debug("未指定签名，使用第一个匹配的方法: " + candidates.get(0));
            return candidates.get(0);
        }
        
        for (Method method : candidates) {
            String methodSignature = getMethodSignature(method);
            logger.debug("比较签名: " + methodSignature + " == " + signature);
            if (methodSignature.equals(signature)) {
                logger.debug("找到匹配的方法: " + method);
                return method;
            }
        }
        
        String errorMsg = "未找到匹配签名的方法: " + methodName + " (签名: " + signature + ")\n";
        errorMsg += "候选方法:\n";
        for (Method m : candidates) {
            errorMsg += "  " + getMethodSignature(m) + "\n";
        }
        logger.error(errorMsg);
        throw new NoSuchMethodException(errorMsg);
    }
    
    private String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i].getSimpleName());
            if (i < params.length - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
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
            
            methodHook = XposedHelpers.findAndHookMethod(targetClass, memberName, new XC_MethodHook() {
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
                            if (i < param.args.length - 1) argsStr.append(", ");
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
            
            logger.info("方法hook成功: " + memberName);
        } catch (Exception e) {
            WatchLogger logger = new WatchLogger();
            logger.error("Hook方法失败: " + memberName, e);
            addOutput("警告: Hook方法失败，将使用轮询模式: " + e.getMessage());
        }
    }
    
    private void cleanup() {
        if (methodHook != null) {
            try {
                methodHook.unhook();
                WatchLogger logger = new WatchLogger();
                logger.info("取消方法hook: " + memberName);
            } catch (Exception e) {
                WatchLogger logger = new WatchLogger();
                logger.error("取消方法hook失败", e);
            }
        }
    }
    
    private void monitorField() throws IllegalAccessException {
        if (targetField == null) return;
        
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
