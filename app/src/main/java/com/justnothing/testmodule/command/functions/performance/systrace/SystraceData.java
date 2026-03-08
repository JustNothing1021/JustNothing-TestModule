package com.justnothing.testmodule.command.functions.performance.systrace;

import java.util.List;
import java.util.Map;

public record SystraceData(String file, long duration, CPUData cpuData, GPUData gpuData,
                           MemoryData memoryData, List<ThreadData> threadData,
                           List<IOData> ioData) {

    public record CPUData(Map<Integer, Double> cpuUsage, double averageUsage) {
    }

    public record GPUData(double usage, int fps, int droppedFrames) {
    }

    public record MemoryData(long totalMemory, long heapMemory, int gcCount, long gcDuration) {
    }

    public record ThreadData(int threadId, String threadName, String state, double cpuUsage) {
    }

    public record IOData(String operation, long bytes, long duration) {
    }
}
