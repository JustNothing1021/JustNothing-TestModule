package com.justnothing.testmodule.command.functions.memory.impl;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.memory.MemoryUtils;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

public abstract class MemorySubCommand<Req extends CommandRequest, Res extends CommandResult>
        extends AbstractCommand<Req, Res> {

    protected static final Logger logger = Logger.getLoggerForName("MemorySubCommand");

    protected CommandExecutor.CmdExecContext<Req> context;

    protected MemorySubCommand(String commandName, Class<Req> requestType, Class<Res> responseType) {
        super(commandName, requestType, responseType);
    }

    @Override
    protected Res executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception {
        this.context = context;
        Req request = context.getRequest();
        if (request == null) {
            @SuppressWarnings("unchecked")
            Res errorResult = (Res) new com.justnothing.testmodule.command.base.protocol.CommandResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("请求对象不能为空");
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
            context.print("无法读取 /proc/meminfo: ", Colors.RED);
            context.println(
                Objects.requireNonNullElse(e.getMessage(), "未知错误"), Colors.YELLOW
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

                        MemoryUtils.printMemoryValue(context, "总大小: ", size);
                        MemoryUtils.printMemoryValue(context, "驻留内存: ", resident);
                        MemoryUtils.printMemoryValue(context, "共享内存: ", shared);
                        MemoryUtils.printMemoryValue(context, "代码段: ", text);
                        MemoryUtils.printMemoryValue(context, "数据段: ", data);
                    }
                }
            }
        } catch (IOException e) {
            context.print("无法读取进程内存统计: ", Colors.RED);
            context.println(
                Objects.requireNonNullElse(e.getMessage(), "未知错误"), Colors.YELLOW
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
            result.append("无法读取 /proc/meminfo: ").append(e.getMessage()).append("\n");
        }
        return result.toString();
    }

    protected void dumpHeapInfoColored() {
        context.println("=== 堆内存信息 ===", Colors.CYAN);
        context.println("");

        Runtime runtime = Runtime.getRuntime();
        context.println("Java运行时内存:", Colors.CYAN);
        MemoryUtils.printMemoryValue(context, "  最大: ", runtime.maxMemory());
        MemoryUtils.printMemoryValue(context, "  已分配: ", runtime.totalMemory());
        MemoryUtils.printMemoryValue(context, "  空闲: ", runtime.freeMemory());
        MemoryUtils.printMemoryValue(context, "  已用: ", runtime.totalMemory() - runtime.freeMemory());
        context.println("");

        context.println("原生堆内存:", Colors.CYAN);
        MemoryUtils.printMemoryValue(context, "  已分配: ", android.os.Debug.getNativeHeapAllocatedSize());
        MemoryUtils.printMemoryValue(context, "  已用: ", android.os.Debug.getNativeHeapSize());
        MemoryUtils.printMemoryValue(context, "  空闲: ", android.os.Debug.getNativeHeapFreeSize());
        context.println("");

        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager =
                (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                context.println("系统内存:", Colors.CYAN);
                MemoryUtils.printMemoryValue(context, "  可用: ", memoryInfo.availMem);
                MemoryUtils.printMemoryValue(context, "  总计: ", memoryInfo.totalMem);
                MemoryUtils.printMemoryValue(context, "  阈值: ", memoryInfo.threshold);
                context.print("  低内存: ", Colors.GRAY);
                context.println(memoryInfo.lowMemory ? "是" : "否",
                    memoryInfo.lowMemory ? Colors.RED : Colors.LIGHT_GREEN);
                context.println("");
            }
        }

        context.println("=== 内存详细信息 ===", Colors.CYAN);
        context.println("");
        printMeminfoColored();
    }

    protected void dumpThreadInfoColored() {
        context.println("=== 线程信息 ===", Colors.CYAN);
        context.println("");

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        context.print("线程总数: ", Colors.GRAY);
        context.println(String.valueOf(allStackTraces.size()), Colors.YELLOW);
        context.println("");

        context.println("=== 线程详情 ===", Colors.CYAN);
        context.println("");

        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();

            context.print("线程: ", Colors.CYAN);
            context.println(thread.getName(), Colors.LIGHT_GREEN);
            context.print("  ID: ", Colors.GRAY);
            context.println(String.valueOf(thread.getId()), Colors.YELLOW);
            context.print("  状态: ", Colors.GRAY);
            context.println(thread.getState().toString(), Colors.LIGHT_GREEN);
            context.print("  优先级: ", Colors.GRAY);
            context.println(String.valueOf(thread.getPriority()), Colors.YELLOW);
            context.print("  守护: ", Colors.GRAY);
            context.println(thread.isDaemon() ? "是" : "否",
                thread.isDaemon() ? Colors.MAGENTA : Colors.LIGHT_GREEN);
            context.print("  中断: ", Colors.GRAY);
            context.println(thread.isInterrupted() ? "是" : "否",
                thread.isInterrupted() ? Colors.RED : Colors.LIGHT_GREEN);

            if (stackTrace != null && stackTrace.length > 0) {
                context.print("  堆栈:", Colors.GRAY);
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
        context.println("=== 系统信息 ===", Colors.CYAN);
        context.println("");

        context.print("操作系统: ", Colors.GRAY);
        context.println(System.getProperty("os.name"), Colors.YELLOW);
        context.print("系统版本: ", Colors.GRAY);
        context.println(System.getProperty("os.version"), Colors.YELLOW);
        context.print("架构: ", Colors.GRAY);
        context.println(System.getProperty("os.arch"), Colors.YELLOW);
        context.print("处理器数: ", Colors.GRAY);
        context.println(String.valueOf(Runtime.getRuntime().availableProcessors()), Colors.YELLOW);
        context.print("Java版本: ", Colors.GRAY);
        context.println(System.getProperty("java.version"), Colors.YELLOW);
        context.print("Java供应商: ", Colors.GRAY);
        context.println(System.getProperty("java.vendor"), Colors.YELLOW);
        context.print("Java虚拟机: ", Colors.GRAY);
        context.println(System.getProperty("java.vm.name"), Colors.YELLOW);
        context.print("Java虚拟机版本: ", Colors.GRAY);
        context.println(System.getProperty("java.vm.version"), Colors.YELLOW);
        context.println("");
    }
}
