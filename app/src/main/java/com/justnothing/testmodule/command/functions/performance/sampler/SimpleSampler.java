package com.justnothing.testmodule.command.functions.performance.sampler;

import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleSampler extends AbstractSampler<SimpleSampleData> {

    private static final Logger logger = Logger.getLoggerForName(SimpleSampler.class.getSimpleName());
    private final Map<String, AtomicInteger> methodCounts = new ConcurrentHashMap<>();

    public SimpleSampler(int sampleRate) {
        super(sampleRate);
    }

    @Override
    protected void doSample() {
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        int threadCount = 0;
        int frameCount = 0;

        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();

            if (thread.getName().contains("Fast-Pool")) {
                continue;
            }

            if (stackTrace == null) {
                continue;
            }

            threadCount++;
            for (StackTraceElement element : stackTrace) {
                String methodKey = element.getClassName() + "." + element.getMethodName();
                methodCounts.computeIfAbsent(methodKey, k -> new AtomicInteger(0)).incrementAndGet();
                frameCount++;
            }
        }

        int currentTotal = incrementSampleCount();

        if (currentTotal == 1) {
            logger.info("📊 [sample] 首次采样完成: %d 线程, %d 堆栈帧", threadCount, frameCount);
        } else if (currentTotal % sampleRate == 0) {
            logger.info("📊 [sample] 采样中... 总次数=%d, 本次=%d线程/%d帧, 已追踪%d方法",
                    currentTotal, threadCount, frameCount, methodCounts.size());
        }
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
