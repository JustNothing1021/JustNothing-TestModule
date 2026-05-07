package com.justnothing.testmodule.command.functions.memory.impl;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.memory.MemoryInfoRequest;
import com.justnothing.testmodule.command.functions.memory.MemoryInfoResult;
import com.justnothing.testmodule.command.functions.memory.MemoryUtils;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

@SubCommandInfo(
    description = "显示详细的内存使用情况, 包括Java堆、原生堆、进程内存等",
    usage = "memory info [options]",
    examples = {
        "memory info",
        "memory info -h",
        "memory info --detailed"
    },
    optionsDesc = """
            选项:
              -h, --heap       只显示堆内存信息
              -d, --detailed   显示详细内存信息 (默认)
            """
)
public class InfoCommand extends MemorySubCommand<MemoryInfoRequest, MemoryInfoResult> {

    public InfoCommand() {
        super("memory info", MemoryInfoRequest.class, MemoryInfoResult.class);
    }

    @Override
    protected MemoryInfoResult executeMemoryCommand(MemoryInfoRequest request) throws Exception {
        Context appContext = getApplicationContext();
        if (appContext == null) {
            logger.warn("无法获取应用上下文，部分信息可能不可用");
        }

        MemoryInfoResult result = new MemoryInfoResult();
        result.setRequestId(request.getRequestId());
        result.setTimestamp(System.currentTimeMillis());

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        result.setJavaMaxMemory(maxMemory);
        result.setJavaTotalMemory(totalMemory);
        result.setJavaFreeMemory(freeMemory);
        result.setJavaUsedMemory(usedMemory);

        if (maxMemory > 0) {
            double percent = (double) usedMemory / maxMemory * 100;
            result.setJavaUsagePercent(percent);
        }

        result.setNativeAllocatedSize(Debug.getNativeHeapAllocatedSize());
        result.setNativeHeapSize(Debug.getNativeHeapSize());
        result.setNativeFreeSize(Debug.getNativeHeapFreeSize());

        if (request.isHeapOnly()) {
            context.println("===== 堆内存信息 =====", Colors.CYAN);
            context.println("");

            context.println("===== 原生堆内存 =====", Colors.CYAN);
            context.println("");
            MemoryUtils.printMemoryValue(context, "已分配: ", Debug.getNativeHeapAllocatedSize());
            MemoryUtils.printMemoryValue(context, "已用: ", Debug.getNativeHeapSize());
            MemoryUtils.printMemoryValue(context, "空闲: ", Debug.getNativeHeapFreeSize());
            context.println("");

            context.println("===== Java运行时内存 =====", Colors.CYAN);
            context.println("");
            MemoryUtils.printMemoryValue(context, "最大内存: ", maxMemory);
            MemoryUtils.printMemoryValue(context, "已分配内存: ", totalMemory);
            MemoryUtils.printMemoryValue(context, "空闲内存: ", freeMemory);
            MemoryUtils.printMemoryValue(context, "已用内存: ", usedMemory);
        } else {
            context.println("===== 详细内存信息 =====", Colors.CYAN);
            context.println("");

            printJavaMemoryInfo(runtime, maxMemory, totalMemory, freeMemory, usedMemory);
            printNativeHeapInfo();
            
            if (appContext != null) {
                printProcessMemory(appContext, result);
            }

            printSystemMemoryInfo();
            printProcessMemoryStats();
        }

        logger.info("内存信息查询完成");
        result.setSuccess(true);

        return result;
    }

    private void printJavaMemoryInfo(Runtime runtime, long maxMemory, long totalMemory,
                                       long freeMemory, long usedMemory) {
        context.println("===== Java运行时内存 =====", Colors.CYAN);
        context.println("");

        MemoryUtils.printMemoryValue(context, "最大内存: ", maxMemory);
        MemoryUtils.printMemoryValue(context, "已分配内存: ", totalMemory);
        MemoryUtils.printMemoryValue(context, "空闲内存: ", freeMemory);
        MemoryUtils.printMemoryValue(context, "已用内存: ", usedMemory);

        if (maxMemory > 0) {
            double percent = (double) usedMemory / maxMemory * 100;
            context.print("使用率: ", Colors.GRAY);
            context.print(String.format(Locale.US, "%.2f", percent), MemoryUtils.getPercentColor(percent));
            context.println("%", Colors.GRAY);
        }
        context.println("");
    }

    private void printNativeHeapInfo() {
        context.println("===== 原生堆内存 =====", Colors.CYAN);
        context.println("");

        MemoryUtils.printMemoryValue(context, "已分配: ", Debug.getNativeHeapAllocatedSize());
        MemoryUtils.printMemoryValue(context, "已用: ", Debug.getNativeHeapSize());
        MemoryUtils.printMemoryValue(context, "空闲: ", Debug.getNativeHeapFreeSize());
        context.println("");
    }

    private void printProcessMemory(Context appContext, MemoryInfoResult result) {
        context.println("===== 进程内存 =====", Colors.CYAN);
        context.println("");

        ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);

            MemoryUtils.printMemoryValue(context, "可用内存: ", memoryInfo.availMem);
            MemoryUtils.printMemoryValue(context, "总内存: ", memoryInfo.totalMem);
            MemoryUtils.printMemoryValue(context, "内存阈值: ", memoryInfo.threshold);
            context.print("低内存状态: ", Colors.GRAY);
            context.println(memoryInfo.lowMemory ? "是" : "否", 
                memoryInfo.lowMemory ? Colors.RED : Colors.LIGHT_GREEN);
            context.println("");

            result.setSystemAvailMem(memoryInfo.availMem);
            result.setTotalMem(memoryInfo.totalMem);
            result.setSystemThreshold(memoryInfo.threshold);
            result.setSystemLowMemory(memoryInfo.lowMemory);

            int pid = android.os.Process.myPid();
            Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{pid});
            if (memoryInfos.length > 0) {
                Debug.MemoryInfo processMemory = memoryInfos[0];
                context.println("当前进程内存:", Colors.CYAN);
                MemoryUtils.printMemoryValue(context, "  PSS: ", processMemory.getTotalPss() * 1024L);
                MemoryUtils.printMemoryValue(context, "  USS: ", processMemory.getTotalPrivateDirty() * 1024L);
                MemoryUtils.printMemoryValue(context, "  RSS: ", processMemory.getTotalSharedDirty() * 1024L);
                context.println("");

                result.setProcessPss(processMemory.getTotalPss() * 1024L);
                result.setProcessUss(processMemory.getTotalPrivateDirty() * 1024L);
                result.setProcessRss(processMemory.getTotalSharedDirty() * 1024L);
            }
        }
    }

    private void printSystemMemoryInfo() {
        context.println("===== 系统内存 =====", Colors.CYAN);
        context.println("");
        printMeminfoColored();
    }

    private void printProcessMemoryStats() {
        context.println("===== 进程内存统计 =====", Colors.CYAN);
        context.println("");
        printProcessMemoryStatsColored();
    }
}
