package com.justnothing.testmodule.command.functions.performance.sampler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleSampler extends AbstractSampler<SimpleSampleData> {

    private final Map<String, AtomicInteger> methodCounts = new ConcurrentHashMap<>();

    public SimpleSampler(int sampleRate) {
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

            for (StackTraceElement element : stackTrace) {
                String methodKey = element.getClassName() + "." + element.getMethodName();
                methodCounts.computeIfAbsent(methodKey, k -> new AtomicInteger(0)).incrementAndGet();
            }
        }

        incrementSampleCount();
    }

    public Map<String, Integer> getReport() {
        Map<String, Integer> report = new ConcurrentHashMap<>();
        methodCounts.forEach((k, v) -> report.put(k, v.get()));
        return report;
    }

    @Override
    public SimpleSampleData getData() {
        return new SimpleSampleData(
                0,
                sampleRate,
                startTime,
                getStopTime(),
                totalSamples.get(),
                getReport()
        );
    }
}
