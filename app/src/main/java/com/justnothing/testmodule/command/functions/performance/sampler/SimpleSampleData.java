package com.justnothing.testmodule.command.functions.performance.sampler;

import java.util.Map;

public record SimpleSampleData(int id, int sampleRate, long startTime, long stopTime, 
                               int totalSamples, Map<String, Integer> methodCounts) implements SampleData {
}
