package com.justnothing.testmodule.command.functions.memory.impl;

import android.app.ActivityManager;
import android.content.Context;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.memory.request.GcRequest;
import com.justnothing.testmodule.command.functions.memory.response.GcResult;
import com.justnothing.testmodule.command.functions.memory.util.MemoryUtils;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.memory.MemoryTexts;

import java.util.Locale;

@SubCommandInfo(
    description = MemoryTexts.SUB_MEMORY_GC_DESC,
    usage = "memory gc [options]",
    examples = {
        "memory gc",
        "memory gc --full",
        "memory gc --stats"
    },
    optionsDesc = MemoryTexts.SUB_MEMORY_GC_OPTIONS
)
public class GcCommand extends AbstractMemoryCommand<GcRequest, GcResult> {

    public GcCommand() {
        super("memory gc", GcRequest.class, GcResult.class);
    }

    @Override
    protected GcResult executeMemoryCommand(GcRequest request) throws Exception {
        Runtime runtime = Runtime.getRuntime();

        context.println(Text.zhEn("===== 垃圾回收 =====", "===== Garbage collection =====").text(), Colors.CYAN);
        context.println("");

        long beforeUsed = runtime.totalMemory() - runtime.freeMemory();
        long beforeTotal = runtime.totalMemory();
        long beforeMax = runtime.maxMemory();

        printMemoryStatus(context, Text.zhEn("GC前堆内存: ", "Heap before GC: ").text(), beforeUsed, beforeTotal, beforeMax);
        context.println("");

        if (request.isFullGc()) {
            logger.info("执行完整GC");
            System.gc();
            System.runFinalization();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.gc();
        } else {
            logger.info("执行GC");
            System.gc();
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long afterUsed = runtime.totalMemory() - runtime.freeMemory();
        long afterTotal = runtime.totalMemory();
        long afterMax = runtime.maxMemory();

        printMemoryStatus(context, Text.zhEn("GC后堆内存: ", "Heap after GC: ").text(), afterUsed, afterTotal, afterMax);
        context.println("");

        long freed = beforeUsed - afterUsed;
        if (freed > 0) {
            context.print(Text.zhEn("释放内存: ", "Freed memory: ").text(), Colors.LIGHT_GREEN);
            MemoryUtils.printBytes(context, freed);
            context.println("", Colors.DEFAULT);
        } else if (freed < 0) {
            context.print(Text.zhEn("内存增加: ", "Memory increased: ").text(), Colors.RED);
            MemoryUtils.printBytes(context, -freed);
            context.println(" (?)", Colors.GRAY);
            context.println(Text.zhEn("可以试试 memory gc --full", "Try running memory gc --full").text(), Colors.YELLOW);
        } else {
            context.println(Text.zhEn("内存未变化", "Memory unchanged").text(), Colors.GRAY);
        }

        GcResult result = new GcResult(request.getRequestId());
        result.setBeforeUsedMemory(beforeUsed);
        result.setAfterUsedMemory(afterUsed);
        result.setFreedBytes(freed);
        result.setBeforeTotalMemory(beforeTotal);
        result.setAfterTotalMemory(afterTotal);

        if (beforeMax > 0) {
            double beforePercent = (double) beforeUsed / beforeMax * 100;
            double afterPercent = (double) afterUsed / afterMax * 100;
            result.setBeforeUsagePercent(beforePercent);
            result.setAfterUsagePercent(afterPercent);
        }

        if (request.isShowStats()) {
            printGcStats(runtime, afterUsed, afterTotal, afterMax);
        }

        logger.info("垃圾回收完成");
        result.setSuccess(true);

        return result;
    }

    private void printMemoryStatus(CommandExecutor.CmdExecContext ctx, String label,
                                    long used, long total, long max) {
        ctx.print(label, Colors.GRAY);
        MemoryUtils.printBytes(ctx, used);
        ctx.print(" / ", Colors.GRAY);
        MemoryUtils.printBytes(ctx, total);
        if (max > 0) {
            double percent = (double) used / max * 100;
            ctx.print(" (", Colors.GRAY);
            ctx.print(String.format(Locale.US, "%.1f", percent), MemoryUtils.getPercentColor(percent));
            ctx.print("%)", Colors.GRAY);
        }
        ctx.println("", Colors.DEFAULT);
    }

    private void printGcStats(Runtime runtime, long afterUsed, long afterTotal, long afterMax) {
        context.println("");
        context.println(Text.zhEn("===== GC统计信息 =====", "===== GC statistics =====").text(), Colors.CYAN);
        context.println("");
        context.println(Text.zhEn("Tip: Android不提供详细的GC统计信息", "Tip: Android does not provide detailed GC statistics").text(), Colors.GRAY);
        context.println(Text.zhEn("以下是内存使用统计:", "Memory usage statistics:").text(), Colors.GRAY);
        context.println("");

        context.println(Text.zhEn("Java堆内存:", "Java heap:").text(), Colors.CYAN);
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_MAX.text(), afterMax);
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_ALLOCATED.text(), afterTotal);
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_USED.text(), afterUsed);
        MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_FREE.text(), runtime.freeMemory());
        context.println("");

        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager =
                (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo =
                    new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                context.println(MemoryTexts.LABEL_SYSTEM_MEMORY.text(), Colors.CYAN);
                MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_AVAILABLE.text(), memoryInfo.availMem);
                MemoryUtils.printMemoryValue(context, MemoryTexts.LABEL_INDENTED_TOTAL.text(), memoryInfo.totalMem);
                context.print(MemoryTexts.LABEL_INDENTED_LOW_MEMORY.text(), Colors.GRAY);
                context.println(memoryInfo.lowMemory ? MemoryTexts.VALUE_YES.text() : MemoryTexts.VALUE_NO.text(),
                    memoryInfo.lowMemory ? Colors.RED : Colors.LIGHT_GREEN);
                context.println("");
            }
        }

        context.println(Text.zhEn("进程内存统计:", "Process memory statistics:").text(), Colors.CYAN);
        printProcessMemoryStatsColored();
    }
}
