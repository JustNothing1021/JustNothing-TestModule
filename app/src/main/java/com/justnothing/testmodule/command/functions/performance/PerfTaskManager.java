package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampler;
import com.justnothing.testmodule.command.functions.performance.sampler.MultiThreadSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.MultiThreadSampler;
import com.justnothing.testmodule.command.functions.performance.sampler.SimpleSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.SimpleSampler;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceData;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceRunner;
import com.justnothing.testmodule.command.functions.performance.trace.TraceData;
import com.justnothing.testmodule.command.functions.performance.trace.Tracer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PerfTaskManager {
    private static final PerfTaskManager INSTANCE = new PerfTaskManager();

    private final AtomicInteger nextId = new AtomicInteger(1);

    private final Map<Integer, SimpleSampler> samplers = new ConcurrentHashMap<>();
    private final Map<Integer, SimpleSampleData> sampleDataMap = new ConcurrentHashMap<>();
    private final Map<Integer, MultiThreadSampler> multiThreadSamplers = new ConcurrentHashMap<>();
    private final Map<Integer, MultiThreadSampleData> multiThreadSampleDataMap = new ConcurrentHashMap<>();
    private final Map<Integer, HierarchicalSampler> hierarchicalSamplers = new ConcurrentHashMap<>();
    private final Map<Integer, HierarchicalSampleData> hierarchicalSampleDataMap = new ConcurrentHashMap<>();
    private final Map<Integer, Tracer> tracers = new ConcurrentHashMap<>();
    private final Map<Integer, List<TraceData>> traceDataMap = new ConcurrentHashMap<>();
    private final Map<Integer, SystraceRunner> systraceRunners = new ConcurrentHashMap<>();
    private final Map<Integer, SystraceData> systraceDataMap = new ConcurrentHashMap<>();

    private PerfTaskManager() {}

    public static PerfTaskManager getInstance() {
        return INSTANCE;
    }

    public int generateId() {
        return nextId.getAndIncrement();
    }

    public void resetId() {
        nextId.set(1);
    }

    // === Simple Sampler ===
    public int addSimpleSampler(SimpleSampler sampler) {
        int id = generateId();
        samplers.put(id, sampler);
        return id;
    }

    public SimpleSampler getSimpleSampler(int id) { return samplers.get(id); }
    public Map<Integer, SimpleSampler> getSimpleSamplers() { return samplers; }

    public void addSimpleSampleData(int id, SimpleSampleData data) {
        sampleDataMap.put(id, data);
    }
    public SimpleSampleData getSimpleSampleData(int id) { return sampleDataMap.get(id); }
    public Map<Integer, SimpleSampleData> getSimpleSampleDataMap() { return sampleDataMap; }
    public void removeSimpleSampler(int id) { samplers.remove(id); }

    // === MultiThread Sampler ===
    public int addMultiThreadSampler(MultiThreadSampler sampler) {
        int id = generateId();
        multiThreadSamplers.put(id, sampler);
        return id;
    }
    public MultiThreadSampler getMultiThreadSampler(int id) { return multiThreadSamplers.get(id); }
    public Map<Integer, MultiThreadSampler> getMultiThreadSamplers() { return multiThreadSamplers; }

    public void addMultiThreadSampleData(int id, MultiThreadSampleData data) {
        multiThreadSampleDataMap.put(id, data);
    }
    public MultiThreadSampleData getMultiThreadSampleData(int id) { return multiThreadSampleDataMap.get(id); }
    public Map<Integer, MultiThreadSampleData> getMultiThreadSampleDataMap() { return multiThreadSampleDataMap; }
    public void removeMultiThreadSampler(int id) { multiThreadSamplers.remove(id); }

    // === Hierarchical Sampler ===
    public int addHierarchicalSampler(HierarchicalSampler sampler) {
        int id = generateId();
        hierarchicalSamplers.put(id, sampler);
        return id;
    }
    public HierarchicalSampler getHierarchicalSampler(int id) { return hierarchicalSamplers.get(id); }
    public Map<Integer, HierarchicalSampler> getHierarchicalSamplers() { return hierarchicalSamplers; }

    public void addHierarchicalSampleData(int id, HierarchicalSampleData data) {
        hierarchicalSampleDataMap.put(id, data);
    }
    public HierarchicalSampleData getHierarchicalSampleData(int id) { return hierarchicalSampleDataMap.get(id); }
    public Map<Integer, HierarchicalSampleData> getHierarchicalSampleDataMap() { return hierarchicalSampleDataMap; }
    public void removeHierarchicalSampler(int id) { hierarchicalSamplers.remove(id); }

    // === Tracer ===
    public int addTracer(Tracer tracer) {
        int id = generateId();
        tracers.put(id, tracer);
        return id;
    }
    public Tracer getTracer(int id) { return tracers.get(id); }
    public Map<Integer, Tracer> getTracers() { return tracers; }

    public void addTraceData(int id, List<TraceData> data) {
        traceDataMap.put(id, data);
    }
    public List<TraceData> getTraceData(int id) { return traceDataMap.get(id); }
    public Map<Integer, List<TraceData>> getTraceDataMap() { return traceDataMap; }
    public void removeTracer(int id) { tracers.remove(id); }

    // === Systrace ===
    public int addSystraceRunner(SystraceRunner runner) {
        int id = generateId();
        systraceRunners.put(id, runner);
        return id;
    }
    public SystraceRunner getSystraceRunner(int id) { return systraceRunners.get(id); }
    public Map<Integer, SystraceRunner> getSystraceRunners() { return systraceRunners; }

    public void addSystraceData(int id, SystraceData data) {
        systraceDataMap.put(id, data);
    }
    public SystraceData getSystraceData(int id) { return systraceDataMap.get(id); }
    public Map<Integer, SystraceData> getSystraceDataMap() { return systraceDataMap; }
    public void removeSystraceRunner(int id) { systraceRunners.remove(id); }

    // === Clear All ===
    public void clearAll() {
        for (SimpleSampler s : samplers.values()) s.stop();
        for (MultiThreadSampler s : multiThreadSamplers.values()) s.stop();
        for (HierarchicalSampler s : hierarchicalSamplers.values()) s.stop();
        for (Tracer t : tracers.values()) { if (t.isRunning()) t.stop(); }
        for (SystraceRunner r : systraceRunners.values()) { if (r.isRunning()) r.stop(); }

        samplers.clear();
        sampleDataMap.clear();
        multiThreadSamplers.clear();
        multiThreadSampleDataMap.clear();
        hierarchicalSamplers.clear();
        hierarchicalSampleDataMap.clear();
        tracers.clear();
        traceDataMap.clear();
        systraceRunners.clear();
        systraceDataMap.clear();

        PerformanceManager.getInstance().clearAll();
        resetId();
    }

    public int getTotalRunningCount() {
        return samplers.size() + multiThreadSamplers.size() + hierarchicalSamplers.size()
                + tracers.size() + systraceRunners.size()
                + PerformanceManager.getInstance().getPerformanceHookCount();
    }

    public int getTotalCompletedCount() {
        return sampleDataMap.size() + multiThreadSampleDataMap.size()
                + hierarchicalSampleDataMap.size() + traceDataMap.size() + systraceDataMap.size();
    }
}
