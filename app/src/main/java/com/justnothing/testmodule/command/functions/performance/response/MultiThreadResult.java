package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import java.util.List;

@SerializeKeyName("MultiThreadResult")
public class MultiThreadResult extends CommandResult {

    private int taskId;

    private int sampleRate;

    private String status;

    private int totalSamples;

    private int threadCount;

    private long duration;

    private List<ThreadEntry> threadData;

    private String exportPath;

    public static class ThreadEntry {
        private String threadName;

        private int sampleCount;

        private List<MethodEntry> methods;

        public static class MethodEntry {
            private String methodName;

            private int count;

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
