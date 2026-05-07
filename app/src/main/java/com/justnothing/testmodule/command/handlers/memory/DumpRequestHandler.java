package com.justnothing.testmodule.command.handlers.memory;

import com.justnothing.testmodule.command.functions.memory.DumpRequest;
import com.justnothing.testmodule.command.functions.memory.DumpResult;
import com.justnothing.testmodule.command.functions.memory.MemoryUtils;

import java.util.Date;
import java.util.Map;

public class DumpRequestHandler {

    public DumpResult handle(DumpRequest request) {
        DumpResult result = new DumpResult(request.getRequestId());
        result.setTimestamp(System.currentTimeMillis());

        StringBuilder output = new StringBuilder();

        output.append("=== 系统堆转储 ===\n");
        output.append("时间: ").append(new Date()).append("\n\n");

        if (request.isHeapOnly() || request.isFullDump()) {
            appendHeapInfo(output);
        }

        if (request.isThreadsOnly() || request.isFullDump()) {
            appendThreadInfo(output);
        }

        if (request.isFullDump()) {
            appendSystemInfo(output);
        }

        result.setDumpContent(output.toString());
        result.setSuccess(true);

        return result;
    }

    private void appendHeapInfo(StringBuilder output) {
        output.append("=== 堆内存信息 ===\n\n");

        Runtime runtime = Runtime.getRuntime();
        output.append("Java运行时内存:\n");
        output.append("  最大: ").append(MemoryUtils.formatBytes(runtime.maxMemory())).append("\n");
        output.append("  已分配: ").append(MemoryUtils.formatBytes(runtime.totalMemory())).append("\n");
        output.append("  空闲: ").append(MemoryUtils.formatBytes(runtime.freeMemory())).append("\n");
        output.append("  已用: ").append(MemoryUtils.formatBytes(runtime.totalMemory() - runtime.freeMemory())).append("\n\n");

        output.append("原生堆内存:\n");
        output.append("  已分配: ").append(MemoryUtils.formatBytes(android.os.Debug.getNativeHeapAllocatedSize())).append("\n");
        output.append("  已用: ").append(MemoryUtils.formatBytes(android.os.Debug.getNativeHeapSize())).append("\n");
        output.append("  空闲: ").append(MemoryUtils.formatBytes(android.os.Debug.getNativeHeapFreeSize())).append("\n\n");

        output.append("=== 内存详细信息 ===\n\n");
        output.append(readMeminfo());
    }

    private void appendThreadInfo(StringBuilder output) {
        output.append("=== 线程信息 ===\n\n");

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        output.append("线程总数: ").append(allStackTraces.size()).append("\n\n");

        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();

            output.append("线程: ").append(thread.getName()).append("\n");
            output.append("  ID: ").append(thread.getId()).append("\n");
            output.append("  状态: ").append(thread.getState()).append("\n\n");
        }
    }

    private void appendSystemInfo(StringBuilder output) {
        output.append("=== 系统信息 ===\n\n");
        output.append("操作系统: ").append(System.getProperty("os.name")).append("\n");
        output.append("系统版本: ").append(System.getProperty("os.version")).append("\n");
        output.append("架构: ").append(System.getProperty("os.arch")).append("\n\n");
    }

    private static String readMeminfo() {
        StringBuilder result = new StringBuilder();
        try (java.io.BufferedReader reader = 
                 new java.io.BufferedReader(new java.io.FileReader("/proc/meminfo"))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 20) {
                result.append(line).append("\n");
                count++;
            }
        } catch (java.io.IOException e) {
            result.append("无法读取 /proc/meminfo: ").append(e.getMessage()).append("\n");
        }
        return result.toString();
    }
}
