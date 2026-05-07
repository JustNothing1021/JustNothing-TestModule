package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("HierarchicalResult")
@AutoSerializable
public class HierarchicalResult extends CommandResult {

    @ResultField(name = "taskId", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int taskId;

    @ResultField(name = "sampleRate", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int sampleRate;

    @ResultField(name = "status", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String status;

    @ResultField(name = "totalSamples", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int totalSamples;

    @ResultField(name = "methodCount", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int methodCount;

    @ResultField(name = "duration", defaultValue = ValueSupplier.ZeroSupplier.class)
    private long duration;

    @ResultField(name = "hotMethods", description = "热点方法(含调用者)")
    private List<MethodCallEntry> hotMethods;

    @ResultField(name = "exportPath", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String exportPath;

    @AutoSerializable
    public static class MethodCallEntry {
        @ResultField(name = "methodKey", defaultValue = ValueSupplier.EmptyStringSupplier.class)
        private String methodKey;

        @ResultField(name = "sampleCount", defaultValue = ValueSupplier.ZeroSupplier.class)
        private int sampleCount;

        @ResultField(name = "percentage")
        private double percentage;

        @ResultField(name = "avgDepth")
        private double avgDepth;

        @ResultField(name = "callers")
        private List<CallerEntry> callers;

        @AutoSerializable
        public static class CallerEntry {
            @ResultField(name = "callerName", defaultValue = ValueSupplier.EmptyStringSupplier.class)
            private String callerName;

            @ResultField(name = "count", defaultValue = ValueSupplier.ZeroSupplier.class)
            private int count;

            @ResultField(name = "percentage")
            private double percentage;

            public String getCallerName() { return callerName; }
            public void setCallerName(String v) { callerName = v; }
            public int getCount() { return count; }
            public void setCount(int v) { count = v; }
            public double getPercentage() { return percentage; }
            public void setPercentage(double v) { percentage = v; }
        }

        public String getMethodKey() { return methodKey; }
        public void setMethodKey(String v) { methodKey = v; }
        public int getSampleCount() { return sampleCount; }
        public void setSampleCount(int v) { sampleCount = v; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double v) { percentage = v; }
        public double getAvgDepth() { return avgDepth; }
        public void setAvgDepth(double v) { avgDepth = v; }
        public List<CallerEntry> getCallers() { return callers; }
        public void setCallers(List<CallerEntry> v) { callers = v; }
    }

    public HierarchicalResult() {}
    public HierarchicalResult(String requestId) { super(requestId); }

    public int getTaskId() { return taskId; }
    public void setTaskId(int v) { taskId = v; }
    public int getSampleRate() { return sampleRate; }
    public void setSampleRate(int v) { sampleRate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public int getTotalSamples() { return totalSamples; }
    public void setTotalSamples(int v) { totalSamples = v; }
    public int getMethodCount() { return methodCount; }
    public void setMethodCount(int v) { methodCount = v; }
    public long getDuration() { return duration; }
    public void setDuration(long v) { duration = v; }
    public List<MethodCallEntry> getHotMethods() { return hotMethods; }
    public void setHotMethods(List<MethodCallEntry> v) { hotMethods = v; }
    public String getExportPath() { return exportPath; }
    public void setExportPath(String v) { exportPath = v; }
}
