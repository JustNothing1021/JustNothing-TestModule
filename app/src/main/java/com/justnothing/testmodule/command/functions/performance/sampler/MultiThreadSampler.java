package com.justnothing.testmodule.command.functions.performance.sampler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadSampler {
    private volatile boolean running = false;
    private final Map<String, Map<String, AtomicInteger>> threadMethodCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> totalSamplesPerThread = new ConcurrentHashMap<>();
    private final AtomicInteger totalSamples = new AtomicInteger(0);
    private Thread samplerThread;
    private final int sampleRate;
    private long startTime;
    private long stopTime;

    public MultiThreadSampler(int sampleRate) {
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
        samplerThread.setName("MultiThreadPerformanceSampler");
        samplerThread.setDaemon(true);
        samplerThread.start();
    }

    private void sample() {
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();
            
            if (thread.getName().equals("MultiThreadPerformanceSampler")) {
                continue;
            }
            
            if (stackTrace == null || stackTrace.length == 0) {
                continue;
            }
            
            String threadKey = thread.getName() + " (ID: " + thread.threadId() + ")";
            
            Map<String, AtomicInteger> methodCounts = threadMethodCounts.computeIfAbsent(
                threadKey, k -> new ConcurrentHashMap<>());
            
            for (StackTraceElement element : stackTrace) {
                String methodKey = element.getClassName() + "." + element.getMethodName();
                methodCounts.computeIfAbsent(methodKey, k -> new AtomicInteger(0)).incrementAndGet();
            }
            
            totalSamplesPerThread.computeIfAbsent(threadKey, k -> new AtomicInteger(0)).incrementAndGet();
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

    public Map<String, Map<String, Integer>> getReport() {
        Map<String, Map<String, Integer>> report = new ConcurrentHashMap<>();
        threadMethodCounts.forEach((thread, methods) -> {
            Map<String, Integer> methodReport = new ConcurrentHashMap<>();
            methods.forEach((k, v) -> methodReport.put(k, v.get()));
            report.put(thread, methodReport);
        });
        return report;
    }

    public Map<String, Integer> getThreadSampleCounts() {
        Map<String, Integer> report = new ConcurrentHashMap<>();
        totalSamplesPerThread.forEach((k, v) -> report.put(k, v.get()));
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

    public int getThreadCount() {
        return threadMethodCounts.size();
    }
}
