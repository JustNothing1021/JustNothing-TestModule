package com.justnothing.testmodule.command.functions.memory.impl;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.memory.MemoryTexts;
import com.justnothing.testmodule.command.functions.memory.util.MemoryUtils;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractMemoryCommand<Req extends CommandRequest<?>, Res extends CommandResult>
        extends AbstractCommand<Req, Res> {

    protected static final Logger logger = Logger.getLoggerForName("AbstractMemoryCommand");

    protected CommandExecutor.CmdExecContext<Req> context;

    protected AbstractMemoryCommand(String commandName, Class<Req> requestType, Class<Res> responseType) {
        super(commandName, requestType, responseType);
    }

    @Override
    protected Res executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception {
        this.context = context;
        Req request = context.getRequest();
        if (request == null) {
            @SuppressWarnings("unchecked")
            Res errorResult = (Res) new CommandResult();
            errorResult.setSuccess(false);
            errorResult.setMessage(Text.zhEn("请求对象不能为空", "Request object must not be null").text());
            return errorResult;
        }
        return executeMemoryCommand(request);
    }

    protected abstract Res executeMemoryCommand(Req request) throws Exception;

    protected Context getApplicationContext() {
        try {
            @SuppressLint("PrivateApi") Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);

            Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
            return (Context) getApplicationMethod.invoke(activityThread);
        } catch (Exception e) {
            logger.error("获取应用上下文失败", e);
            return null;
        }
    }

    protected void printMeminfoColored() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 20) {
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    context.print(key, Colors.CYAN);
                    context.print(": ", Colors.GRAY);

                    if (value.contains("kB")) {
                        String numStr = value.replace("kB", "").trim();
                        try {
                            long kb = Long.parseLong(numStr);
                            MemoryUtils.printBytes(context, kb * 1024);
                        } catch (NumberFormatException e) {
                            context.print(value, Colors.YELLOW);
                        }
                    } else {
                        context.print(value, Colors.YELLOW);
                    }
                    context.println("", Colors.DEFAULT);
                } else {
                    context.println(line, Colors.GRAY);
                }
                count++;
            }
        } catch (IOException e) {
            context.print(MemoryTexts.READ_MEMINFO_FAILED.text(), Colors.RED);
            context.println(
                Objects.requireNonNullElse(e.getMessage(), MemoryTexts.UNKNOWN_ERROR.text()), Colors.YELLOW
            );
        }
    }

    protected void printProcessMemoryStatsColored() {
        try {
            int pid = android.os.Process.myPid();
            try (BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/statm"))) {
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 7) {
                        long pageSize = 4096;
                        long size = Long.parseLong(parts[0]) * pageSize;
                        long resident = Long.parseLong(parts[1]) * pageSize;
                        long shared = Long.parseLong(parts[2]) * pageSize;
                        long text = Long.parseLong(parts[3]) * pageSize;
                        long data = Long.parseLong(parts[5]) * pageSize;

                        MemoryUtils.printMemoryValue(context, Text.zhEn("总大小: ", "Total size: ").text(), size);
                        MemoryUtils.printMemoryValue(context, Text.zhEn("驻留内存: ", "Resident memory: ").text(), resident);
                        MemoryUtils.printMemoryValue(context, Text.zhEn("共享内存: ", "Shared memory: ").text(), shared);
                        MemoryUtils.printMemoryValue(context, Text.zhEn("代码段: ", "Code segment: ").text(), text);
                        MemoryUtils.printMemoryValue(context, Text.zhEn("数据段: ", "Data segment: ").text(), data);
                    }
                }
            }
        } catch (IOException e) {
            context.print(Text.zhEn("无法读取进程内存统计: ", "Failed to read process memory stats: ").text(), Colors.RED);
            context.println(
                Objects.requireNonNullElse(e.getMessage(), MemoryTexts.UNKNOWN_ERROR.text()), Colors.YELLOW
            );
        }
    }

    protected static String readMeminfo() {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 20) {
                result.append(line).append("\n");
                count++;
            }
        } catch (IOException e) {
            result.append(MemoryTexts.READ_MEMINFO_FAILED.text()).append(e.getMessage()).append("\n");
        }
        return result.toString();
    }

    protected void dumpHeapInfoColored() {
        context.println(Text.zhEn("=== 堆内存信息 ===", "=== Heap memory information ===").text(), Colors.CYAN);
        context.println("");

        Runtime runtime = Runtime.getRuntime();
        context.println(Text.zhEn("Java运行时内存:", "Java runtime memory:").text(), Colors.CYAN);
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_MAX.text(), runtime.maxMemory());
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_ALLOCATED.text(), runtime.totalMemory());
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_FREE.text(), runtime.freeMemory());
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_USED.text(), runtime.totalMemory() - runtime.freeMemory());
        context.println("");

        context.println(Text.zhEn("原生堆内存:", "Native heap memory:").text(), Colors.CYAN);
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_ALLOCATED.text(), android.os.Debug.getNativeHeapAllocatedSize());
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_USED.text(), android.os.Debug.getNativeHeapSize());
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_FREE.text(), android.os.Debug.getNativeHeapFreeSize());
        context.println("");

        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager =
                (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                context.println(MemoryTexts.LABEL_SYSTEM_MEMORY.text(), Colors.CYAN);
                MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_AVAILABLE.text(), memoryInfo.availMem);
                MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_TOTAL.text(), memoryInfo.totalMem);
                MemoryUtils.printMemoryValue(context, Text.zhEn("  阈值: ", "  Threshold: ").text(), memoryInfo.threshold);
                context.print(MemoryTexts.LABEL_INDENTED_LOW_MEMORY.text(), Colors.GRAY);
                context.println(memoryInfo.lowMemory ? MemoryTexts.VALUE_YES.text() : MemoryTexts.VALUE_NO.text(),
                    memoryInfo.lowMemory ? Colors.RED : Colors.LIGHT_GREEN);
                context.println("");
            }
        }

        context.println(Text.zhEn("=== 内存详细信息 ===", "=== Detailed memory information ===").text(), Colors.CYAN);
        context.println("");
        printMeminfoColored();
    }

    protected void dumpThreadInfoColored() {
        context.println(Text.zhEn("=== 线程信息 ===", "=== Thread information ===").text(), Colors.CYAN);
        context.println("");

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        context.print(MemoryTexts.LABEL_THREAD_COUNT.text(), Colors.GRAY);
        context.println(String.valueOf(allStackTraces.size()), Colors.YELLOW);
        context.println("");

        context.println(Text.zhEn("=== 线程详情 ===", "=== Thread details ===").text(), Colors.CYAN);
        context.println("");

        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();

            context.print(MemoryTexts.LABEL_THREAD.text(), Colors.CYAN);
            context.println(thread.getName(), Colors.LIGHT_GREEN);
            context.print("  ID: ", Colors.GRAY);
            context.println(String.valueOf(thread.getId()), Colors.YELLOW);
            context.print(MemoryTexts.LABEL_THREAD_STATE.text(), Colors.GRAY);
            context.println(thread.getState().toString(), Colors.LIGHT_GREEN);
            context.print(Text.zhEn("  优先级: ", "  Priority: ").text(), Colors.GRAY);
            context.println(String.valueOf(thread.getPriority()), Colors.YELLOW);
            context.print(Text.zhEn("  守护: ", "  Daemon: ").text(), Colors.GRAY);
            context.println(thread.isDaemon() ? MemoryTexts.VALUE_YES.text() : MemoryTexts.VALUE_NO.text(),
                thread.isDaemon() ? Colors.MAGENTA : Colors.LIGHT_GREEN);
            context.print(Text.zhEn("  中断: ", "  Interrupted: ").text(), Colors.GRAY);
            context.println(thread.isInterrupted() ? MemoryTexts.VALUE_YES.text() : MemoryTexts.VALUE_NO.text(),
                thread.isInterrupted() ? Colors.RED : Colors.LIGHT_GREEN);

            if (stackTrace != null && stackTrace.length > 0) {
                context.print(Text.zhEn("  堆栈:", "  Stack trace:").text(), Colors.GRAY);
                context.println("");
                for (StackTraceElement element : stackTrace) {
                    context.print("    ", Colors.DEFAULT);
                    context.println(element.toString(), Colors.GRAY);
                }
            }
            context.println("");
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected void dumpSystemInfoColored() {
        context.println(Text.zhEn("=== 系统信息 ===", "=== System information ===").text(), Colors.CYAN);
        context.println("");

        context.print(MemoryTexts.LABEL_OS.text(), Colors.GRAY);
        context.println(System.getProperty("os.name"), Colors.YELLOW);
        context.print(MemoryTexts.LABEL_OS_VERSION.text(), Colors.GRAY);
        context.println(System.getProperty("os.version"), Colors.YELLOW);
        context.print(MemoryTexts.LABEL_ARCH.text(), Colors.GRAY);
        context.println(System.getProperty("os.arch"), Colors.YELLOW);
        context.print(Text.zhEn("处理器数: ", "Processor count: ").text(), Colors.GRAY);
        context.println(String.valueOf(Runtime.getRuntime().availableProcessors()), Colors.YELLOW);
        context.print(Text.zhEn("Java版本: ", "Java version: ").text(), Colors.GRAY);
        context.println(System.getProperty("java.version"), Colors.YELLOW);
        context.print(Text.zhEn("Java供应商: ", "Java vendor: ").text(), Colors.GRAY);
        context.println(System.getProperty("java.vendor"), Colors.YELLOW);
        context.print(Text.zhEn("Java虚拟机: ", "Java VM: ").text(), Colors.GRAY);
        context.println(System.getProperty("java.vm.name"), Colors.YELLOW);
        context.print(Text.zhEn("Java虚拟机版本: ", "Java VM version: ").text(), Colors.GRAY);
        context.println(System.getProperty("java.vm.version"), Colors.YELLOW);
        context.println("");
    }
}
