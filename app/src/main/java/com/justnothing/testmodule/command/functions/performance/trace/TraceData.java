package com.justnothing.testmodule.command.functions.performance.trace;

public class TraceData {
    public final String name;
    public final long startTime;
    public final long duration;
    public final int threadId;
    public final String threadName;

    public TraceData(String name, long startTime, long duration, int threadId, String threadName) {
        this.name = name;
        this.startTime = startTime;
        this.duration = duration;
        this.threadId = threadId;
        this.threadName = threadName;
    }

    public String toHumanReadable() {
        return String.format("%s: %.3f ms (Thread: %s, ID: %d)", 
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
