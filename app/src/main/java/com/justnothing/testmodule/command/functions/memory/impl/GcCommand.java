package com.justnothing.testmodule.command.functions.memory.impl;

import android.app.ActivityManager;
import android.content.Context;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.memory.GcRequest;
import com.justnothing.testmodule.command.functions.memory.GcResult;
import com.justnothing.testmodule.command.functions.memory.MemoryUtils;
import com.justnothing.testmodule.command.output.Colors;

import java.util.Locale;

@SubCommandInfo(
    description = "手动触发垃圾回收, 可选完整GC或显示统计信息",
    usage = "memory gc [options]",
    examples = {
        "memory gc",
        "memory gc --full",
        "memory gc --stats"
    },
    optionsDesc = """
            选项:
              --full    - 执行完整的GC (建议开启)
              --stats   - 显示GC统计信息
            """
)
public class GcCommand extends MemorySubCommand<GcRequest, GcResult> {

    public GcCommand() {
        super("memory gc", GcRequest.class, GcResult.class);
    }

    @Override
    protected GcResult executeMemoryCommand(GcRequest request) throws Exception {
        Runtime runtime = Runtime.getRuntime();

        context.println("===== 垃圾回收 =====", Colors.CYAN);
        context.println("");

        long beforeUsed = runtime.totalMemory() - runtime.freeMemory();
        long beforeTotal = runtime.totalMemory();
        long beforeMax = runtime.maxMemory();

        printMemoryStatus(context, "GC前堆内存: ", beforeUsed, beforeTotal, beforeMax);
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

        printMemoryStatus(context, "GC后堆内存: ", afterUsed, afterTotal, afterMax);
        context.println("");

        long freed = beforeUsed - afterUsed;
        if (freed > 0) {
            context.print("释放内存: ", Colors.LIGHT_GREEN);
            MemoryUtils.printBytes(context, freed);
            context.println("", Colors.DEFAULT);
        } else if (freed < 0) {
            context.print("内存增加: ", Colors.RED);
            MemoryUtils.printBytes(context, -freed);
            context.println(" (?)", Colors.GRAY);
            context.println("可以试试 memory gc --full", Colors.YELLOW);
        } else {
            context.println("内存未变化", Colors.GRAY);
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
        context.println("===== GC统计信息 =====", Colors.CYAN);
        context.println("");
        context.println("Tip: Android不提供详细的GC统计信息", Colors.GRAY);
        context.println("以下是内存使用统计:", Colors.GRAY);
        context.println("");

        context.println("Java堆内存:", Colors.CYAN);
        MemoryUtils.printMemoryValue(context, "  最大: ", afterMax);
        MemoryUtils.printMemoryValue(context, "  已分配: ", afterTotal);
        MemoryUtils.printMemoryValue(context, "  已用: ", afterUsed);
        MemoryUtils.printMemoryValue(context, "  空闲: ", runtime.freeMemory());
        context.println("");

        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager =
                (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo =
                    new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                context.println("系统内存:", Colors.CYAN);
                MemoryUtils.printMemoryValue(context, "  可用: ", memoryInfo.availMem);
                MemoryUtils.printMemoryValue(context, "  总计: ", memoryInfo.totalMem);
                context.print("  低内存: ", Colors.GRAY);
                context.println(memoryInfo.lowMemory ? "是" : "否",
                    memoryInfo.lowMemory ? Colors.RED : Colors.LIGHT_GREEN);
                context.println("");
            }
        }

        context.println("进程内存统计:", Colors.CYAN);
        printProcessMemoryStatsColored();
    }
}
