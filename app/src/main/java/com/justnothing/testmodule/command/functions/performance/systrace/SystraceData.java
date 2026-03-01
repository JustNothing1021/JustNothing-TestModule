package com.justnothing.testmodule.command.functions.performance.systrace;

import java.util.List;
import java.util.Map;

public class SystraceData {
    public final String file;
    public final long duration;
    public final CpuData cpuData;
    public final GpuData gpuData;
    public final MemoryData memoryData;
    public final List<ThreadData> threadData;
    public final List<IoData> ioData;

    public SystraceData(String file, long duration, CpuData cpuData, GpuData gpuData, 
                        MemoryData memoryData, List<ThreadData> threadData, List<IoData> ioData) {
        this.file = file;
        this.duration = duration;
        this.cpuData = cpuData;
        this.gpuData = gpuData;
        this.memoryData = memoryData;
        this.threadData = threadData;
        this.ioData = ioData;
    }

    public static class CpuData {
        public final Map<Integer, Double> cpuUsage;
        public final double averageUsage;

        public CpuData(Map<Integer, Double> cpuUsage, double averageUsage) {
            this.cpuUsage = cpuUsage;
            this.averageUsage = averageUsage;
        }
    }

    public static class GpuData {
        public final double usage;
        public final int fps;
        public final int droppedFrames;

        public GpuData(double usage, int fps, int droppedFrames) {
            this.usage = usage;
            this.fps = fps;
            this.droppedFrames = droppedFrames;
        }
    }

    public static class MemoryData {
        public final long totalMemory;
        public final long heapMemory;
        public final int gcCount;
        public final long gcDuration;

        public MemoryData(long totalMemory, long heapMemory, int gcCount, long gcDuration) {
            this.totalMemory = totalMemory;
            this.heapMemory = heapMemory;
            this.gcCount = gcCount;
            this.gcDuration = gcDuration;
        }
    }

    public static class ThreadData {
        public final int threadId;
        public final String threadName;
        public final String state;
        public final double cpuUsage;

        public ThreadData(int threadId, String threadName, String state, double cpuUsage) {
            this.threadId = threadId;
            this.threadName = threadName;
            this.state = state;
            this.cpuUsage = cpuUsage;
        }
    }

    public static class IoData {
        public final String operation;
        public final long bytes;
        public final long duration;

        public IoData(String operation, long bytes, long duration) {
            this.operation = operation;
            this.bytes = bytes;
            this.duration = duration;
        }
    }
}
