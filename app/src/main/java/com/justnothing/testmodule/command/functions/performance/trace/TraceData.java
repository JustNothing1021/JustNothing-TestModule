package com.justnothing.testmodule.command.functions.performance.trace;

import java.util.Locale;

public record TraceData(String name, long startTime, long duration, int threadId,
                        String threadName) {

    public String toHumanReadable() {
        return String.format(
                Locale.getDefault(),
                "%s: %.3f ms (Thread: %s, ID: %d)",
                name, duration / 1_000_000.0, threadName, threadId);
    }

    public String getDurationString() {
        long durationUs = duration / 1000;
        long durationMs = durationUs / 1000;
        long durationS = durationMs / 1000;

        if (durationS > 0) {
            return durationS + " 秒";
        } else if (durationMs > 0) {
            return durationMs + " ms";
        } else if (durationUs > 0) {
            return durationUs + " μs";
        } else {
            return duration + " ns";
        }
    }

    public long getStartTimeMs() {
        return startTime / 1_000_000;
    }

    public long getDurationMs() {
        return duration / 1_000_000;
    }
}
