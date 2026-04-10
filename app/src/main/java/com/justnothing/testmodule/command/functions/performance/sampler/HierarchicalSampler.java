package com.justnothing.testmodule.command.functions.performance.sampler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class HierarchicalSampler extends AbstractSampler<HierarchicalSampleData> {

    private final Map<String, MethodCallInfo> methodCallInfos = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> callerCounts = new ConcurrentHashMap<>();

    public HierarchicalSampler(int sampleRate) {
        super(sampleRate);
    }

    @Override
    protected void doSample() {
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

        incrementSampleCount();
    }

    public Map<String, MethodCallInfo> getReport() {
        return new ConcurrentHashMap<>(methodCallInfos);
    }

    public Map<String, Integer> getCallerCounts() {
        Map<String, Integer> report = new ConcurrentHashMap<>();
        callerCounts.forEach((k, v) -> report.put(k, v.get()));
        return report;
    }

    public int getMethodCount() {
        return methodCallInfos.size();
    }

    @Override
    public HierarchicalSampleData getData() {
        return new HierarchicalSampleData(
                0,
                sampleRate,
                startTime,
                getStopTime(),
                totalSamples.get(),
                getReport(),
                getCallerCounts(),
                getMethodCount()
        );
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
