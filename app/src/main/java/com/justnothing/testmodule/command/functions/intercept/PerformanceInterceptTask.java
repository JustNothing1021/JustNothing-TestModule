package com.justnothing.testmodule.command.functions.intercept;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.XC_MethodHook;

public class PerformanceInterceptTask extends AbstractInterceptTask {

    private final AtomicLong totalDuration = new AtomicLong(0);
    private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxDuration = new AtomicLong(Long.MIN_VALUE);
    private final ConcurrentHashMap<String, AtomicLong> durationByMethod = new ConcurrentHashMap<>();

    private long startTime;
    private long stopTime;

    public PerformanceInterceptTask(int id, String className, String methodName,
                                     String signature, ClassLoader classLoader) {
        super(id, className, methodName, signature, classLoader, TaskType.PERFORMANCE);
    }

    @Override
    protected XC_MethodHook createMethodHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setObjectExtra("perfStartTime", System.nanoTime());
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Long startNs = (Long) param.getObjectExtra("perfStartTime");
                if (startNs == null) return;

                long duration = System.nanoTime() - startNs;
                recordDuration(param.method, duration);
            }
        };
    }

    private void recordDuration(Object method, long durationNs) {
        if (!enabled || !running.get()) return;

        hitCount.incrementAndGet();
        totalDuration.addAndGet(durationNs);

        long currentMin = minDuration.get();
        while (durationNs < currentMin) {
            if (minDuration.compareAndSet(currentMin, durationNs)) break;
            currentMin = minDuration.get();
        }

        long currentMax = maxDuration.get();
        while (durationNs > currentMax) {
            if (maxDuration.compareAndSet(currentMax, durationNs)) break;
            currentMax = maxDuration.get();
        }

        if (method instanceof Method) {
            String methodKey = ((Method) method).getName();
            durationByMethod.computeIfAbsent(methodKey, k -> new AtomicLong(0))
                    .addAndGet(durationNs);
        }
    }

    @Override
    public void onInstall() {
        logger.info("性能监控任务安装: " + getDisplayName());
    }

    @Override
    public void onActivated() {
        startTime = System.currentTimeMillis();
        logger.info("性能监控开始: " + getDisplayName());
    }

    @Override
    public void onDeactivated() {
        stopTime = System.currentTimeMillis();
        logger.info("性能监控暂停: " + getDisplayName());
    }

    @Override
    public void onUninstall() {
        logger.info("性能监控任务卸载: " + getDisplayName());
    }

    public long getTotalDurationNs() {
        return totalDuration.get();
    }

    public long getTotalDurationMs() {
        return totalDuration.get() / 1_000_000;
    }

    public double getAverageDurationNs() {
        int hits = hitCount.get();
        return hits > 0 ? (double) totalDuration.get() / hits : 0;
    }

    public double getAverageDurationMs() {
        return getAverageDurationNs() / 1_000_000;
    }

    public long getMinDurationNs() {
        long min = minDuration.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxDurationNs() {
        long max = maxDuration.get();
        return max == Long.MIN_VALUE ? 0 : max;
    }

    public long getMinDurationMs() {
        return getMinDurationNs() / 1_000_000;
    }

    public long getMaxDurationMs() {
        return getMaxDurationNs() / 1_000_000;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    public long getDurationMs() {
        if (stopTime > 0) {
            return stopTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    public Map<String, AtomicLong> getDurationByMethod() {
        return new ConcurrentHashMap<>(durationByMethod);
    }

    public void resetStats() {
        hitCount.set(0);
        totalDuration.set(0);
        minDuration.set(Long.MAX_VALUE);
        maxDuration.set(Long.MIN_VALUE);
        durationByMethod.clear();
        startTime = System.currentTimeMillis();
        stopTime = 0;
    }

    public PerformanceStats getStats() {
        return new PerformanceStats(
                id,
                className,
                methodName,
                signature,
                hitCount.get(),
                totalDuration.get(),
                getMinDurationNs(),
                getMaxDurationNs(),
                getAverageDurationNs(),
                startTime,
                stopTime > 0 ? stopTime : System.currentTimeMillis(),
                new ConcurrentHashMap<>(durationByMethod)
        );
    }

    public record PerformanceStats(int id, String className, String methodName, String signature,
                                   int callCount, long totalDurationNs, long minDurationNs,
                                   long maxDurationNs, double avgDurationNs, long startTime,
                                   long stopTime, Map<String, AtomicLong> durationByMethod) {

        public long getTotalDurationMs() {
                return totalDurationNs / 1_000_000;
            }

            public double getAvgDurationMs() {
                return avgDurationNs / 1_000_000;
            }

            public long getMinDurationMs() {
                return minDurationNs / 1_000_000;
            }

            public long getMaxDurationMs() {
                return maxDurationNs / 1_000_000;
            }

            public long getDurationMs() {
                return stopTime - startTime;
            }

            public String getDurationString() {
                long duration = getDurationMs();
                if (duration < 1000) {
                    return duration + "ms";
                } else if (duration < 60000) {
                    return String.format(Locale.getDefault(), "%.2fs", duration / 1000.0);
                } else {
                    return String.format(Locale.getDefault(), "%.2fmin", duration / 60000.0);
                }
            }
        }
}
