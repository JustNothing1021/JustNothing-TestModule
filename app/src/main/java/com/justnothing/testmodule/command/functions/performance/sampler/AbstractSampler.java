package com.justnothing.testmodule.command.functions.performance.sampler;

import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.functions.Logger;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractSampler<T extends SampleData> implements Sampler<T> {

    protected final Logger logger = Logger.getLoggerForName(getClass().getSimpleName());

    protected volatile boolean running = false;
    protected final AtomicInteger totalSamples = new AtomicInteger(0);
    protected Future<?> samplerFuture;
    protected final int sampleRate;
    protected long startTime;
    protected long stopTime;

    protected AbstractSampler(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override
    public void start() {
        if (running) {
            throw new IllegalStateException("采样器已在运行");
        }

        running = true;
        startTime = System.currentTimeMillis();
        long intervalMs = 1000 / sampleRate;

        logger.info("启动采样器 (频率: " + sampleRate + " Hz)");

        samplerFuture = ThreadPoolManager.scheduleWithFixedDelayWhile(
            this::doSample,
            0, intervalMs, TimeUnit.MILLISECONDS,
            () -> running
        );
    }

    protected abstract void doSample();

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        stopTime = System.currentTimeMillis();

        if (samplerFuture != null) {
            samplerFuture.cancel(true);
        }

        logger.info("停止采样器 (采样次数: " + totalSamples.get() + ")");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public int getTotalSamples() {
        return totalSamples.get();
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getStopTime() {
        if (isRunning()) {
            return System.currentTimeMillis();
        }
        return stopTime;
    }

    protected void incrementSampleCount() {
        totalSamples.incrementAndGet();
    }
}
