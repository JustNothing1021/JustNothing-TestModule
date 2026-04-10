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

    default String getDurationString() {
        long duration = getDuration();
        if (duration < 1000) {
            return duration + " ms";
        } else if (duration < 60000) {
            return String.format("%.2f 秒", duration / 1000.0);
        } else {
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;
            return minutes + " 分 " + seconds + " 秒";
        }
    }
}
