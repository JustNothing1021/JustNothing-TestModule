package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("HierarchicalResult")
public class HierarchicalResult extends CommandResult {

    private int taskId;

    private int sampleRate;

    private String status;

    private int totalSamples;

    private int methodCount;

    private long duration;

    private List<MethodCallEntry> hotMethods;

    private String exportPath;

    public static class MethodCallEntry {
        private String methodKey;

        private int sampleCount;

        private double percentage;

        private double avgDepth;

        private List<CallerEntry> callers;

        public static class CallerEntry {
            private String callerName;

            private int count;

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
