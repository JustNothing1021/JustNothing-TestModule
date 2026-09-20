package com.justnothing.testmodule.command.functions.performance.sampler;

import java.util.Map;

public record HierarchicalSampleData(int id, int sampleRate, long startTime, long stopTime,
                                     int totalSamples,
                                     Map<String, HierarchicalSampler.MethodCallInfo> methodCallInfos,
                                     Map<String, Integer> callerCounts, int methodCount) implements SampleData {
}
