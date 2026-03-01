package com.justnothing.testmodule.command.functions.performance.sampler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleSampler {
    private volatile boolean running = false;
    private final Map<String, AtomicInteger> methodCounts = new ConcurrentHashMap<>();
    private final AtomicInteger totalSamples = new AtomicInteger(0);
    private Thread samplerThread;
    private final int sampleRate;
    private long startTime;
    private long stopTime;

    public SimpleSampler(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void start() {
        if (running) {
            throw new IllegalStateException("采样器已在运行");
        }

        running = true;
        startTime = System.currentTimeMillis();
        long intervalMs = 1000 / sampleRate;

        samplerThread = new Thread(() -> {
            while (running) {
                sample();
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        samplerThread.setName("PerformanceSampler");
        samplerThread.setDaemon(true);
        samplerThread.start();
    }

    private void sample() {
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();
            
            if (thread.getName().equals("PerformanceSampler")) {
                continue;
            }
            
            if (stackTrace == null || stackTrace.length == 0) {
                continue;
            }
            
            for (StackTraceElement element : stackTrace) {
                String methodKey = element.getClassName() + "." + element.getMethodName();
                methodCounts.computeIfAbsent(methodKey, k -> new AtomicInteger(0)).incrementAndGet();
            }
        }
        
        totalSamples.incrementAndGet();
    }

    public void stop() {
        running = false;
        stopTime = System.currentTimeMillis();
        if (samplerThread != null) {
            samplerThread.interrupt();
        }
    }

    public Map<String, Integer> getReport() {
        Map<String, Integer> report = new ConcurrentHashMap<>();
        methodCounts.forEach((k, v) -> report.put(k, v.get()));
        return report;
    }

    public int getTotalSamples() {
        return totalSamples.get();
    }

    public boolean isRunning() {
        return running;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getStopTime() {
        if (isRunning()) {
            return System.currentTimeMillis();
        }
        return stopTime;
    }
}
