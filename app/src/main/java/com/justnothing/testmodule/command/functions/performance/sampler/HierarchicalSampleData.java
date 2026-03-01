package com.justnothing.testmodule.command.functions.performance.sampler;

import java.util.Map;

public class HierarchicalSampleData {
    public final int id;
    public final int sampleRate;
    public final long startTime;
    public final long stopTime;
    public final int totalSamples;
    public final Map<String, HierarchicalSampler.MethodCallInfo> methodCallInfos;
    public final Map<String, Integer> callerCounts;
    public final int methodCount;

    public HierarchicalSampleData(int id, int sampleRate, long startTime, long stopTime,
                                   int totalSamples, Map<String, HierarchicalSampler.MethodCallInfo> methodCallInfos,
                                   Map<String, Integer> callerCounts, int methodCount) {
        this.id = id;
        this.sampleRate = sampleRate;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.totalSamples = totalSamples;
        this.methodCallInfos = methodCallInfos;
        this.callerCounts = callerCounts;
        this.methodCount = methodCount;
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
}
