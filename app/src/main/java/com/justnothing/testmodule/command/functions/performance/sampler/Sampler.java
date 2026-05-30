package com.justnothing.testmodule.command.functions.performance.sampler;

public interface Sampler<T extends SampleData> {
    void start();
    void stop();
    boolean isRunning();
    int getSampleRate();
    int getTotalSamples();
    long getStartTime();
    long getStopTime();
    T getData();

    default long getDuration() {
        return getStopTime() - getStartTime();
    }

}
