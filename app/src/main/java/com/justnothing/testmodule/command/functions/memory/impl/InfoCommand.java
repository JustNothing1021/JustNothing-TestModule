package com.justnothing.testmodule.command.functions.memory.impl;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.memory.MemoryTexts;
import com.justnothing.testmodule.command.functions.memory.request.MemoryInfoRequest;
import com.justnothing.testmodule.command.functions.memory.response.MemoryInfoResult;
import com.justnothing.testmodule.command.functions.memory.util.MemoryUtils;

import java.util.Locale;

@SubCommandInfo(
    description = MemoryTexts.SUB_MEMORY_INFO_DESC,
    usage = "memory info [options]",
    examples = {
        "memory info",
        "memory info -h",
        "memory info --detailed"
    },
    optionsDesc = MemoryTexts.SUB_MEMORY_INFO_OPTIONS
)
public class InfoCommand extends AbstractMemoryCommand<MemoryInfoRequest, MemoryInfoResult> {

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
            context.println(Text.zhEn("===== 堆内存信息 =====", "===== Heap memory information =====").text(), Colors.CYAN);
            context.println("");

            context.println(MemoryTexts.SECTION_NATIVE_HEAP.text(), Colors.CYAN);
            context.println("");
            MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_NATIVE_ALLOCATED.text(), Debug.getNativeHeapAllocatedSize());
            MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_NATIVE_USED.text(), Debug.getNativeHeapSize());
            MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_NATIVE_FREE.text(), Debug.getNativeHeapFreeSize());
            context.println("");

            context.println(MemoryTexts.SECTION_JAVA_RUNTIME.text(), Colors.CYAN);
            context.println("");
            MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_JAVA_MAX.text(), maxMemory);
            MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_JAVA_ALLOCATED.text(), totalMemory);
            MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_JAVA_FREE.text(), freeMemory);
            MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_JAVA_USED.text(), usedMemory);
        } else {
            context.println(Text.zhEn("===== 详细内存信息 =====", "===== Detailed memory information =====").text(), Colors.CYAN);
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
        context.println(MemoryTexts.SECTION_JAVA_RUNTIME.text(), Colors.CYAN);
        context.println("");

        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_JAVA_MAX.text(), maxMemory);
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_JAVA_ALLOCATED.text(), totalMemory);
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_JAVA_FREE.text(), freeMemory);
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_JAVA_USED.text(), usedMemory);

        if (maxMemory > 0) {
            double percent = (double) usedMemory / maxMemory * 100;
            context.print(Text.zhEn("使用率: ", "Usage: ").text(), Colors.GRAY);
            context.print(String.format(Locale.US, "%.2f", percent), MemoryUtils.getPercentColor(percent));
            context.println("%", Colors.GRAY);
        }
        context.println("");
    }

    private void printNativeHeapInfo() {
        context.println(MemoryTexts.SECTION_NATIVE_HEAP.text(), Colors.CYAN);
        context.println("");

        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_NATIVE_ALLOCATED.text(), Debug.getNativeHeapAllocatedSize());
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_NATIVE_USED.text(), Debug.getNativeHeapSize());
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_NATIVE_FREE.text(), Debug.getNativeHeapFreeSize());
        context.println("");
    }

    private void printProcessMemory(Context appContext, MemoryInfoResult result) {
        context.println(Text.zhEn("===== 进程内存 =====", "===== Process memory =====").text(), Colors.CYAN);
        context.println("");

        ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);

            MemoryUtils.printMemoryValue(context, Text.zhEn("可用内存: ", "Available memory: ").text(), memoryInfo.availMem);
            MemoryUtils.printMemoryValue(context, Text.zhEn("总内存: ", "Total memory: ").text(), memoryInfo.totalMem);
            MemoryUtils.printMemoryValue(context, Text.zhEn("内存阈值: ", "Memory threshold: ").text(), memoryInfo.threshold);
            context.print(Text.zhEn("低内存状态: ", "Low memory state: ").text(), Colors.GRAY);
            context.println(memoryInfo.lowMemory ? MemoryTexts.VALUE_YES.text() : MemoryTexts.VALUE_NO.text(), 
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
                context.println(Text.zhEn("当前进程内存:", "Current process memory:").text(), Colors.CYAN);
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
        context.println(Text.zhEn("===== 系统内存 =====", "===== System memory =====").text(), Colors.CYAN);
        context.println("");
        printMeminfoColored();
    }

    private void printProcessMemoryStats() {
        context.println(Text.zhEn("===== 进程内存统计 =====", "===== Process memory statistics =====").text(), Colors.CYAN);
        context.println("");
        printProcessMemoryStatsColored();
    }
}
