package com.justnothing.testmodule.command.functions.performance.hook;


public record HookData(int id, String className, String methodName, String signature,
                       long startTime, long stopTime, int callCount, long totalDuration,
                       long minDuration, long maxDuration, double averageDuration) {

    public static HookData fromMethodStats(int id, MethodStats stats, long startTime, long stopTime) {
        return new HookData(
                id,
                stats.className,
                stats.methodName,
                stats.getSignature(),
                startTime,
                stopTime,
                stats.callCount.get(),
                stats.totalDuration.get(),
                stats.minDuration.get(),
                stats.maxDuration.get(),
                stats.getAverageDuration()
        );
    }

    public long getDuration() {
        return stopTime - startTime;
    }

    public String getDurationString() {
        long duration = getDuration();
        if (duration < 1000) {
            return duration + " ms";
        } else if (duration < 60000) {
            return (duration / 1000) + " 秒";
        } else {
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;
            return minutes + " 分 " + seconds + " 秒";
        }
    }

    public String getDurationStringNs(long durationNs) {
        long durationUs = durationNs / 1000;
        long durationMs = durationUs / 1000;
        long durationS = durationMs / 1000;

        if (durationS > 0) {
            return durationS + " 秒";
        } else if (durationMs > 0) {
            return durationMs + " ms";
        } else if (durationUs > 0) {
            return durationUs + " μs";
        } else {
            return durationNs + " ns";
        }
    }
}
