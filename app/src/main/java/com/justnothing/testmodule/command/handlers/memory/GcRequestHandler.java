package com.justnothing.testmodule.command.handlers.memory;

import com.justnothing.testmodule.command.proxy.RequestHandler;
import com.justnothing.testmodule.command.functions.memory.GcRequest;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.functions.memory.GcResult;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.util.Locale;

public class GcRequestHandler implements RequestHandler<GcRequest, GcResult> {

    private static final Logger logger = Logger.getLoggerForName("GcRequestHandler");

    @Override
    public String getCommandType() {
        return "Gc";
    }

    @Override
    public GcRequest parseRequest(JSONObject obj) {
        return new GcRequest().fromJson(obj);
    }

    @Override
    public GcResult createResult(String requestId) {
        return new GcResult(requestId);
    }

    @Override
    public GcResult handle(GcRequest request) {
        boolean fullGc = request.isFullGc();
        logger.debug("处理GC请求, fullGc=" + fullGc);

        GcResult result = new GcResult(request.getRequestId());

        try {
            Runtime runtime = Runtime.getRuntime();

            long beforeUsed = runtime.totalMemory() - runtime.freeMemory();
            long beforeTotal = runtime.totalMemory();
            long beforeMax = runtime.maxMemory();

            double beforePercent = beforeMax > 0 ? (double) beforeUsed / beforeMax * 100 : 0;

            result.setBeforeUsedMemory(beforeUsed);
            result.setBeforeTotalMemory(beforeTotal);
            result.setBeforeUsagePercent(beforePercent);

            if (fullGc) {
                logger.info("执行完整GC");
                System.gc();
                System.runFinalization();
                Thread.sleep(100);
                System.gc();
            } else {
                logger.info("执行标准GC");
                System.gc();
            }

            Thread.sleep(100);

            long afterUsed = runtime.totalMemory() - runtime.freeMemory();
            long afterTotal = runtime.totalMemory();
            double afterPercent = beforeMax > 0 ? (double) afterUsed / beforeMax * 100 : 0;

            result.setAfterUsedMemory(afterUsed);
            result.setAfterTotalMemory(afterTotal);
            result.setAfterUsagePercent(afterPercent);
            result.setFreedBytes(beforeUsed - afterUsed);

            logger.info("GC完成, 释放: " + formatBytes(result.getFreedBytes()) +
                    ", 使用率: " + String.format(Locale.US, "%.1f%% -> %.1f%%",
                            beforePercent, afterPercent));

        } catch (Exception e) {
            logger.error("GC执行失败", e);
            result.setError(new CommandResult.ErrorInfo("GC_ERROR", "GC执行失败: " + e.getMessage()));
        }

        return result;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        else return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024));
    }
}
