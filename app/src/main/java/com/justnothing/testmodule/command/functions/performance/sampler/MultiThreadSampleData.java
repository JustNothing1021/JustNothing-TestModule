package com.justnothing.testmodule.command.functions.performance.sampler;

import java.util.Map;

public record MultiThreadSampleData(int id, int sampleRate, long startTime, long stopTime,
                                    int totalSamples,
                                    Map<String, Map<String, Integer>> threadMethodCounts,
                                    Map<String, Integer> threadSampleCounts, int threadCount) implements SampleData {
}
