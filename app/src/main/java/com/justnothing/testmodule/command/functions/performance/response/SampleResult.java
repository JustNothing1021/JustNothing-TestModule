package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import java.util.List;

@SerializeKeyName("SampleResult")
public class SampleResult extends CommandResult {

    private int taskId;

    private int sampleRate;

    private String status;

    private int totalSamples;

    private long duration;

    private String durationStr;

    private List<MethodEntry> hotMethods;

    private String exportPath;

    public static class MethodEntry {
        private String methodName;

        private int count;

        private double percentage;

        public MethodEntry() {}

        public MethodEntry(String methodName, int count, double percentage) {
            this.methodName = methodName;
            this.count = count;
            this.percentage = percentage;
        }

        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    public SampleResult() {}
    public SampleResult(String requestId) { super(requestId); }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public int getSampleRate() { return sampleRate; }
    public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalSamples() { return totalSamples; }
    public void setTotalSamples(int totalSamples) { this.totalSamples = totalSamples; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public String getDurationStr() { return durationStr; }
    public void setDurationStr(String durationStr) { this.durationStr = durationStr; }
    public List<MethodEntry> getHotMethods() { return hotMethods; }
    public void setHotMethods(List<MethodEntry> hotMethods) { this.hotMethods = hotMethods; }
    public String getExportPath() { return exportPath; }
    public void setExportPath(String exportPath) { this.exportPath = exportPath; }
}
