package com.justnothing.testmodule.command.functions.performance.sampler;

import java.util.Map;

public record HierarchicalSampleData(int id, int sampleRate, long startTime, long stopTime,
                                     int totalSamples,
                                     Map<String, HierarchicalSampler.MethodCallInfo> methodCallInfos,
                                     Map<String, Integer> callerCounts, int methodCount) {

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
}
