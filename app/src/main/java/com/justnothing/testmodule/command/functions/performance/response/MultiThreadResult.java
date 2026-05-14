package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;

import java.util.List;

@SerializeKeyName("MultiThreadResult")
@AutoSerializable
public class MultiThreadResult extends CommandResult {

    @ResultField(name = "taskId", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int taskId;

    @ResultField(name = "sampleRate", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int sampleRate;

    @ResultField(name = "status", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String status;

    @ResultField(name = "totalSamples", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int totalSamples;

    @ResultField(name = "threadCount", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int threadCount;

    @ResultField(name = "duration", defaultValue = ValueSupplier.ZeroSupplier.class)
    private long duration;

    @ResultField(name = "threadData", description = "各线程数据")
    private List<ThreadEntry> threadData;

    @ResultField(name = "exportPath", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String exportPath;

    @AutoSerializable
    public static class ThreadEntry {
        @ResultField(name = "threadName", defaultValue = ValueSupplier.EmptyStringSupplier.class)
        private String threadName;

        @ResultField(name = "sampleCount", defaultValue = ValueSupplier.ZeroSupplier.class)
        private int sampleCount;

        @ResultField(name = "methods")
        private List<MethodEntry> methods;

        @AutoSerializable
        public static class MethodEntry {
            @ResultField(name = "methodName", defaultValue = ValueSupplier.EmptyStringSupplier.class)
            private String methodName;

            @ResultField(name = "count", defaultValue = ValueSupplier.ZeroSupplier.class)
            private int count;

            @ResultField(name = "percentage")
            private double percentage;

            public String getMethodName() { return methodName; }
            public void setMethodName(String v) { methodName = v; }
            public int getCount() { return count; }
            public void setCount(int v) { count = v; }
            public double getPercentage() { return percentage; }
            public void setPercentage(double v) { percentage = v; }
        }

        public String getThreadName() { return threadName; }
        public void setThreadName(String v) { threadName = v; }
        public int getSampleCount() { return sampleCount; }
        public void setSampleCount(int v) { sampleCount = v; }
        public List<MethodEntry> getMethods() { return methods; }
        public void setMethods(List<MethodEntry> v) { methods = v; }
    }

    public MultiThreadResult() {}
    public MultiThreadResult(String requestId) { super(requestId); }

    public int getTaskId() { return taskId; }
    public void setTaskId(int v) { taskId = v; }
    public int getSampleRate() { return sampleRate; }
    public void setSampleRate(int v) { sampleRate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public int getTotalSamples() { return totalSamples; }
    public void setTotalSamples(int v) { totalSamples = v; }
    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int v) { threadCount = v; }
    public long getDuration() { return duration; }
    public void setDuration(long v) { duration = v; }
    public List<ThreadEntry> getThreadData() { return threadData; }
    public void setThreadData(List<ThreadEntry> v) { threadData = v; }
    public String getExportPath() { return exportPath; }
    public void setExportPath(String v) { exportPath = v; }
}
