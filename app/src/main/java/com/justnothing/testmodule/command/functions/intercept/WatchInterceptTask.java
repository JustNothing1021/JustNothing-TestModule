package com.justnothing.testmodule.command.functions.intercept;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.intercept.base.AbstractInterceptTask;
import com.justnothing.testmodule.command.functions.intercept.base.TaskType;
import com.justnothing.testmodule.hooks.api.HookParam;
import com.justnothing.testmodule.hooks.api.MethodHook;
import com.justnothing.testmodule.hooks.api.UnhookHandle;
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

    // 这个任务的输出不是命令线程产出的（定时监控在线程池上跑、方法watch 的回调在被 hook 的 app
    // 线程上跑），那些线程上没有请求上下文，Text.text() 会回落到本进程 Locale —— 也就是目标 app 的
    // 语言，而不是发起这条 watch 的那个客户的界面语言。所以在构造时（一定在命令线程上）把语言记下来，
    // 产出文案时用 withLanguage 包一层。缓冲的字符串是按产出时的语言定死的，事后回显改不了。
    private final String language = CliMessages.language();

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
            throw new RuntimeException(Text.zhEn("查找字段失败: %s", "Failed to resolve field: %s")
                    .format(e.getMessage()), e);
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
                    throw new UnsupportedOperationException(Text.zhEn("无法监控一个非静态的字段", "Cannot watch a non-static field").text());
                }
                startFieldMonitoring();
            }
            
            logger.info("Watch任务启动成功: " + id);
        } catch (Exception e) {
            running.set(false);
            logger.error("启动Watch任务失败: " + id, e);
            throw new RuntimeException(Text.zhEn("启动Watch任务失败: %s", "Failed to start watch task: %s")
                    .format(e.getMessage()), e);
        }
    }

    private void startFieldMonitoring() {
        scheduledFuture = ThreadPoolManager.scheduleWithFixedDelayUntil(
                () -> CliMessages.withLanguage(language, () -> {
                    try {
                        monitorField();
                    } catch (Exception e) {
                        addOutput(Text.zhEn("监控出错: %s", "Monitoring failed: %s").format(e.getMessage()));
                        logger.error("监控出错", e);
                    }
                }),
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
            addOutput(Text.zhEn("警告: 非静态字段，无法监控值变化", "Warning: not a static field, cannot monitor value changes").text());
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
    protected MethodHook createMethodHook() {
        return new MethodHook() {
            @Override
            protected void beforeHookedMethod(HookParam param) {
                CliMessages.withLanguage(language, () -> {
                    String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
                    String output = Text.zhEn("[%s] 方法 %s.%s 被调用", "[%s] method %s.%s called")
                            .format(timestamp, targetClass.getSimpleName(), methodName);
                    addOutput(output);

                    Object[] args = param.getArgs();
                    if (args.length > 0) {
                        StringBuilder argsStr = new StringBuilder(Text.zhEn("  参数: ", "  args: ").text());
                        for (int i = 0; i < args.length; i++) {
                            argsStr.append(args[i] != null ? args[i].toString() : "null");
                            if (i < args.length - 1) argsStr.append(", ");
                        }
                        addOutput(argsStr.toString());
                    }
                });
            }

            @Override
            protected void afterHookedMethod(HookParam param) {
                CliMessages.withLanguage(language, () -> {
                    String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
                    Object result = param.getResult();
                    String output = Text.zhEn("[%s] 方法 %s.%s 返回: %s", "[%s] method %s.%s returned: %s")
                            .format(timestamp, targetClass.getSimpleName(), methodName, result != null ? result.toString() : "void");
                    addOutput(output);
                });
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

        for (UnhookHandle unhook : activeHooks) {
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
            if (outputBuffer.isEmpty()) return Text.zhEn("暂无输出", "No output yet").text();

            StringBuilder sb = new StringBuilder();
            sb.append(Text.zhEn("=== Watch %s 输出 (最近%s条) ===\n", "=== Watch %s output (last %s) ===\n").format(id, limit));

            int startIndex = Math.max(0, outputBuffer.size() - limit);
            for (int i = startIndex; i < outputBuffer.size(); i++) {
                sb.append(outputBuffer.get(i)).append("\n");
            }

            sb.append(Text.zhEn("总计: %s 条记录\n", "Total: %s records\n").format(outputCount.get()));
            return sb.toString();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return Text.zhEn("Watch[%d] %s%s (%s, 间隔=%dms, 输出=%d条)",
                        "Watch[%d] %s%s (%s, interval=%dms, output=%d)")
                .format(
                        id,
                        getDisplayName(),
                        watchType == WatchType.FIELD ? Text.zhEn(" [字段]", " [field]").text() : Text.zhEn(" [方法]", " [method]").text(),
                        enabled ? InterceptTexts.STATUS_RUNNING.text() : InterceptTexts.STATUS_PAUSED.text(),
                        interval,
                        outputCount.get());
    }
}
