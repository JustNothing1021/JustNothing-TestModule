package com.justnothing.testmodule.command.functions.performance.sampler;

import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;

public interface SampleData {
    int id();
    int sampleRate();
    long startTime();
    long stopTime();
    int totalSamples();
    default long getDuration() {
        return stopTime() - startTime();
    }
    default String getDurationString() {
        long duration = getDuration();
        if (duration < 1000) {
            return duration + " ms";
        } else if (duration < 60000) {
            return (duration / 1000) + PerformanceTexts.UNIT_SECONDS.text();
        } else {
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;
            return minutes + PerformanceTexts.UNIT_MINUTES.text() + seconds + PerformanceTexts.UNIT_SECONDS.text();
        }
    }
}
