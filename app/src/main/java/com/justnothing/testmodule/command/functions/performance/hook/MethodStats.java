package com.justnothing.testmodule.command.functions.performance.hook;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MethodStats {
    private static final int MAX_DURATIONS = 1000;
    
    public final String className;
    public final String methodName;
    public final Class<?>[] paramTypes;
    public final AtomicInteger callCount;
    public final AtomicLong totalDuration;
    public final AtomicLong minDuration;
    public final AtomicLong maxDuration;
    public final List<Long> durations;

    public MethodStats(String className, String methodName, Class<?>[] paramTypes) {
        this.className = className;
        this.methodName = methodName;
        this.paramTypes = paramTypes;
        this.callCount = new AtomicInteger(0);
        this.totalDuration = new AtomicLong(0);
        this.minDuration = new AtomicLong(Long.MAX_VALUE);
        this.maxDuration = new AtomicLong(0);
        this.durations = new ArrayList<>();
    }

    public void recordCall(long duration) {
        callCount.incrementAndGet();
        totalDuration.addAndGet(duration);
        
        long currentMin = minDuration.get();
        if (duration < currentMin) {
            minDuration.compareAndSet(currentMin, duration);
        }
        
        long currentMax = maxDuration.get();
        if (duration > currentMax) {
            maxDuration.compareAndSet(currentMax, duration);
        }
        
        synchronized (durations) {
            durations.add(duration);
            if (durations.size() > MAX_DURATIONS) {
                durations.remove(0);
            }
        }
    }

    public double getAverageDuration() {
        int count = callCount.get();
        if (count == 0) {
            return 0.0;
        }
        return (double) totalDuration.get() / count;
    }

    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(className).append(".").append(methodName).append("(");
        
        if (paramTypes != null && paramTypes.length > 0) {
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(paramTypes[i].getSimpleName());
            }
        }
        
        sb.append(")");
        return sb.toString();
    }

    public String getDurationString(long durationNs) {
        long durationUs = durationNs / 1000;
        long durationMs = durationUs / 1000;
        long durationS = durationMs / 1000;
        
        if (durationS > 0) {
            return durationS + " 秒";
        } else if (durationMs > 0) {
            return durationMs + " ms";
        } else if (durationUs > 0) {
            return durationUs + " μs";
        } else {
            return durationNs + " ns";
        }
    }
}
