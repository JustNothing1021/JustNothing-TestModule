package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;

import java.util.List;

@SerializeKeyName("SampleResult")
@AutoSerializable
public class SampleResult extends CommandResult {

    @ResultField(name = "taskId", description = "任务ID", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int taskId;

    @ResultField(name = "sampleRate", description = "采样频率(Hz)", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int sampleRate;

    @ResultField(name = "status", description = "状态: running/stopped", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String status;

    @ResultField(name = "totalSamples", description = "总采样次数", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int totalSamples;

    @ResultField(name = "duration", description = "持续时间(ms)", defaultValue = ValueSupplier.ZeroSupplier.class)
    private long duration;

    @ResultField(name = "durationStr", description = "持续时间(可读)", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String durationStr;

    @ResultField(name = "hotMethods", description = "热点方法列表")
    private List<MethodEntry> hotMethods;

    @ResultField(name = "exportPath", description = "导出文件路径", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String exportPath;

    @AutoSerializable
    public static class MethodEntry {
        @ResultField(name = "methodName", defaultValue = ValueSupplier.EmptyStringSupplier.class)
        private String methodName;

        @ResultField(name = "count", defaultValue = ValueSupplier.ZeroSupplier.class)
        private int count;

        @ResultField(name = "percentage")
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
