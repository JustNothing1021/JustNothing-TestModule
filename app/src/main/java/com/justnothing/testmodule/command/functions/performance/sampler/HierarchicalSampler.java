package com.justnothing.testmodule.command.functions.performance.sampler;

import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class HierarchicalSampler {
    private volatile boolean running = false;
    private final Map<String, MethodCallInfo> methodCallInfos = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> callerCounts = new ConcurrentHashMap<>();
    private final AtomicInteger totalSamples = new AtomicInteger(0);
    private Future<?> samplerFuture;
    private final int sampleRate;
    private long startTime;
    private long stopTime;

    public HierarchicalSampler(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void start() {
        if (running) {
            throw new IllegalStateException("采样器已在运行");
        }

        running = true;
        startTime = System.currentTimeMillis();
        long intervalMs = 1000 / sampleRate;

        samplerFuture = ThreadPoolManager.submitFastRunnable(() -> {
            while (running) {
                sample();
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    private void sample() {
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();
            
            if (thread.getName().contains("Fast-Pool")) {
                continue;
            }
            
            if (stackTrace == null || stackTrace.length == 0) {
                continue;
            }
            
            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement element = stackTrace[i];
                String methodKey = element.getClassName() + "." + element.getMethodName();
                
                MethodCallInfo info = methodCallInfos.computeIfAbsent(methodKey, 
                    k -> new MethodCallInfo(methodKey));
                
                info.sampleCount.incrementAndGet();
                info.totalDepth.addAndGet(stackTrace.length - i);
                
                if (i > 0) {
                    StackTraceElement callerElement = stackTrace[i - 1];
                    String callerKey = callerElement.getClassName() + "." + callerElement.getMethodName();
                    
                    info.callers.computeIfAbsent(callerKey, k -> new AtomicInteger(0)).incrementAndGet();
                    callerCounts.computeIfAbsent(callerKey, k -> new AtomicInteger(0)).incrementAndGet();
                }
            }
        }
        
        totalSamples.incrementAndGet();
    }

    public void stop() {
        running = false;
        stopTime = System.currentTimeMillis();
        if (samplerFuture != null) {
            samplerFuture.cancel(true);
        }
    }

    public Map<String, MethodCallInfo> getReport() {
        return new ConcurrentHashMap<>(methodCallInfos);
    }

    public Map<String, Integer> getCallerCounts() {
        Map<String, Integer> report = new ConcurrentHashMap<>();
        callerCounts.forEach((k, v) -> report.put(k, v.get()));
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

    public int getMethodCount() {
        return methodCallInfos.size();
    }

    public static class MethodCallInfo {
        public final String methodKey;
        public final AtomicInteger sampleCount;
        public final AtomicLong totalDepth;
        public final Map<String, AtomicInteger> callers;

        public MethodCallInfo(String methodKey) {
            this.methodKey = methodKey;
            this.sampleCount = new AtomicInteger(0);
            this.totalDepth = new AtomicLong(0);
            this.callers = new ConcurrentHashMap<>();
        }

        public int getSampleCount() {
            return sampleCount.get();
        }

        public double getAverageDepth() {
            int count = sampleCount.get();
            if (count == 0) {
                return 0.0;
            }
            return (double) totalDepth.get() / count;
        }

        public Map<String, Integer> getCallers() {
            Map<String, Integer> result = new ConcurrentHashMap<>();
            callers.forEach((k, v) -> result.put(k, v.get()));
            return result;
        }
    }
}
