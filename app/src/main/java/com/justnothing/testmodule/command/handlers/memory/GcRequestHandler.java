package com.justnothing.testmodule.command.handlers.memory;

import com.justnothing.testmodule.command.functions.memory.GcRequest;
import com.justnothing.testmodule.command.functions.memory.GcResult;

public class GcRequestHandler {

    public GcResult handle(GcRequest request) {
        Runtime runtime = Runtime.getRuntime();

        long beforeUsed = runtime.totalMemory() - runtime.freeMemory();
        long beforeTotal = runtime.totalMemory();
        long beforeMax = runtime.maxMemory();

        if (request.isFullGc()) {
            System.gc();
            System.runFinalization();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.gc();
        } else {
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

        GcResult result = new GcResult(request.getRequestId());
        result.setBeforeUsedMemory(beforeUsed);
        result.setAfterUsedMemory(afterUsed);
        result.setFreedBytes(beforeUsed - afterUsed);
        result.setBeforeTotalMemory(beforeTotal);
        result.setAfterTotalMemory(afterTotal);

        if (beforeMax > 0) {
            double beforePercent = (double) beforeUsed / beforeMax * 100;
            double afterPercent = (double) afterUsed / afterMax * 100;
            result.setBeforeUsagePercent(beforePercent);
            result.setAfterUsagePercent(afterPercent);
        }

        result.setSuccess(true);

        return result;
    }
}
