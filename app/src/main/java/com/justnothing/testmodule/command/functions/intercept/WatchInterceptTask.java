package com.justnothing.testmodule.command.functions.intercept;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XC_MethodHook;

public class WatchInterceptTask extends AbstractInterceptTask {

    public enum WatchType {
        FIELD,
        METHOD
    }

    private final WatchType watchType;
    private final long interval;
    private final int maxOutputSize;
    private final LinkedList<String> outputBuffer = new LinkedList<>();
    private final AtomicInteger outputCount = new AtomicInteger(0);

    private Field targetField;
    private Object lastValue;
    private ScheduledFuture<?> scheduledFuture;

    public WatchInterceptTask(int id, String className, String memberName, String signature,
                              ClassLoader classLoader, WatchType watchType, long interval, int maxOutputSize) {
        super(id, className, memberName, signature, classLoader, TaskType.WATCH);
        this.watchType = watchType;
        this.interval = interval;
        this.maxOutputSize = maxOutputSize;
    }

    @Override
    protected void resolveTargetMethods() {
        if (watchType == WatchType.METHOD) {
            super.resolveTargetMethods();
        }
    }

    protected void resolveTargetField() {
        try {
            targetField = targetClass.getDeclaredField(methodName);
            targetField.setAccessible(true);
            if (Modifier.isStatic(targetField.getModifiers())) {
                lastValue = targetField.get(null);
                logger.debug("字段初始值: " + lastValue);
            }
        } catch (Exception e) {
            logger.error("查找字段失败: " + methodName, e);
            throw new RuntimeException("查找字段失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void start() {
        if (running.get()) {
            logger.warn("任务已经在运行: " + id);
            return;
        }

        running.set(true);
        logger.info("启动Watch任务: " + id + " (" + getDisplayName() + ")");

        try {
            resolveTargetClass();
            
            if (watchType == WatchType.METHOD) {
                resolveTargetMethods();
                installHooks();
            } else {
                resolveTargetField();
                if (!Modifier.isStatic(targetField.getModifiers())) {
                    throw new UnsupportedOperationException("无法监控一个非静态的字段");
                }
                startFieldMonitoring();
            }
            
            logger.info("Watch任务启动成功: " + id);
        } catch (Exception e) {
            running.set(false);
            logger.error("启动Watch任务失败: " + id, e);
            throw new RuntimeException("启动Watch任务失败: " + e.getMessage(), e);
        }
    }

    private void startFieldMonitoring() {
        scheduledFuture = ThreadPoolManager.scheduleWithFixedDelayUntil(
                () -> {
                    try {
                        monitorField();
                    } catch (Exception e) {
                        addOutput("监控出错: " + e.getMessage());
                        logger.error("监控出错", e);
                    }
                },
                0,
                interval, TimeUnit.MILLISECONDS,
                () -> !running.get()
        );
    }

    private void monitorField() throws IllegalAccessException {
        if (targetField == null) return;

        Object currentValue;
        if (Modifier.isStatic(targetField.getModifiers())) {
            currentValue = targetField.get(null);
        } else {
            addOutput("警告: 非静态字段，无法监控值变化");
            return;
        }

        if (!Objects.equals(lastValue, currentValue)) {
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
            String output = String.format("[%s] %s.%s: %s -> %s",
                    timestamp,
                    targetClass.getSimpleName(),
                    methodName,
                    lastValue,
                    currentValue);
            addOutput(output);
            lastValue = currentValue;
        }
    }

    @Override
    protected XC_MethodHook createMethodHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
                String output = String.format("[%s] 方法 %s.%s 被调用",
                        timestamp,
                        targetClass.getSimpleName(),
                        methodName);
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
            protected void afterHookedMethod(MethodHookParam param) {
                String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
                Object result = param.getResult();
                String output = String.format("[%s] 方法 %s.%s 返回: %s",
                        timestamp,
                        targetClass.getSimpleName(),
                        methodName,
                        result != null ? result.toString() : "void");
                addOutput(output);
            }
        };
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

    @Override
    public void stop() {
        if (!running.get()) {
            logger.warn("任务未在运行: " + id);
            return;
        }

        running.set(false);
        logger.info("停止Watch任务: " + id);

        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }

        for (XC_MethodHook.Unhook unhook : activeHooks) {
            try {
                unhook.unhook();
                logger.debug("Hook已移除: " + unhook.getHookedMethod());
            } catch (Exception e) {
                logger.error("移除Hook失败", e);
            }
        }
        activeHooks.clear();
        logger.info("Watch任务已停止: " + id);
    }

    public WatchType getWatchType() {
        return watchType;
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
            if (outputBuffer.isEmpty()) return "暂无输出";

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

    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.getDefault(),
                "Watch[%d] %s%s (%s, 间隔=%dms, 输出=%d条)",
                id,
                getDisplayName(),
                watchType == WatchType.FIELD ? " [字段]" : " [方法]",
                enabled ? "运行中" : "已暂停",
                interval,
                outputCount.get());
    }
}
