package com.justnothing.testmodule.command.functions.performance.sampler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadSampler extends AbstractSampler<MultiThreadSampleData> {

    private final Map<String, Map<String, AtomicInteger>> threadMethodCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> totalSamplesPerThread = new ConcurrentHashMap<>();

    public MultiThreadSampler(int sampleRate) {
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

            String threadKey = thread.getName() + " (ID: " + thread.getId() + ")";

            Map<String, AtomicInteger> methodCounts = threadMethodCounts.computeIfAbsent(
                    threadKey, k -> new ConcurrentHashMap<>());

            for (StackTraceElement element : stackTrace) {
                String methodKey = element.getClassName() + "." + element.getMethodName();
                methodCounts.computeIfAbsent(methodKey, k -> new AtomicInteger(0)).incrementAndGet();
            }

            totalSamplesPerThread.computeIfAbsent(threadKey, k -> new AtomicInteger(0)).incrementAndGet();
        }

        incrementSampleCount();
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

    public int getThreadCount() {
        return threadMethodCounts.size();
    }

    @Override
    public MultiThreadSampleData getData() {
        return new MultiThreadSampleData(
                0,
                sampleRate,
                startTime,
                getStopTime(),
                totalSamples.get(),
                getReport(),
                getThreadSampleCounts(),
                getThreadCount()
        );
    }
}
