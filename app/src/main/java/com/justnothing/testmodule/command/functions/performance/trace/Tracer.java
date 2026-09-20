package com.justnothing.testmodule.command.functions.performance.trace;

import android.os.Trace;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Tracer {
    private static final String TAG = "PerformanceTracer";
    
    private volatile boolean running = false;
    private final Map<String, TraceSection> traceSections = new ConcurrentHashMap<>();
    private final List<TraceData> traceDataList = new ArrayList<>();
    private final Deque<TraceSection> sectionStack = new ArrayDeque<>();
    private final AtomicInteger sectionCount = new AtomicInteger(0);
    private final AtomicLong totalDuration = new AtomicLong(0);
    private long startTime;

    public void start() {
        if (running) {
            throw new IllegalStateException("Tracer 已在运行");
        }

        running = true;
        startTime = System.currentTimeMillis();
        
        Trace.beginSection("PerformanceTracer");
        Log.i(TAG, "Tracer 已启动");
    }

    public void stop() {
        if (!running) {
            throw new IllegalStateException("Tracer 未在运行");
        }

        running = false;
        
        Trace.endSection();
        Log.i(TAG, "Tracer 已停止");
    }

    public void beginSection(String name) {
        if (!running) {
            Log.w(TAG, "Tracer 未运行，忽略 beginSection: " + name);
            return;
        }

        String sectionId = name + "_" + sectionCount.incrementAndGet();
        TraceSection section = new TraceSection(name, System.nanoTime());
        traceSections.put(sectionId, section);
        sectionStack.push(section);
        
        Trace.beginSection(name);
    }

    public void endSection(String name) {
        if (!running) {
            Log.w(TAG, "Tracer 未运行，忽略 endSection: " + name);
            return;
        }

        long endTime = System.nanoTime();
        
        TraceSection section = sectionStack.poll();
        if (section == null) {
            Log.w(TAG, "没有匹配的 beginSection: " + name);
            return;
        }
        
        if (!section.name.equals(name)) {
            Log.w(TAG, "Section 名称不匹配: 期望 '" + section.name + "', 实际 '" + name + "'");
        }
        
        section.endTime = endTime;
        section.duration = endTime - section.startTime;
        
        TraceData data = new TraceData(
            section.name,
            section.startTime,
            section.duration,
            (int) Thread.currentThread().getId(),
            Thread.currentThread().getName()
        );
        
        traceDataList.add(data);
        totalDuration.addAndGet(section.duration);
        
        Trace.endSection();
    }

    public List<TraceData> getTraceData() {
        return new ArrayList<>(traceDataList);
    }

    public boolean isRunning() {
        return running;
    }

    public int getSectionCount() {
        return traceDataList.size();
    }

    public long getTotalDuration() {
        return totalDuration.get();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getStopTime() {
        if (isRunning()) {
            return System.currentTimeMillis();
        }
        return startTime + (totalDuration.get() / 1_000_000);
    }

    public static class TraceSection {
        public final String name;
        public final long startTime;
        public long endTime;
        public long duration;

        public TraceSection(String name, long startTime) {
            this.name = name;
            this.startTime = startTime;
            this.endTime = 0;
            this.duration = 0;
        }
    }
}
