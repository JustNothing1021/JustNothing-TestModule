package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;

@SerializeKeyName("PerfHookResult")
@AutoSerializable
public class PerfHookResult extends CommandResult {

    @ResultField(name = "taskId", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int taskId;

    @ResultField(name = "status", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String status;

    @ResultField(name = "className", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String className;

    @ResultField(name = "methodName", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String methodName;

    @ResultField(name = "signature", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String signature;

    @ResultField(name = "callCount", description = "调用次数", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int callCount;

    @ResultField(name = "totalDurationNs", defaultValue = ValueSupplier.ZeroSupplier.class)
    private long totalDurationNs;

    @ResultField(name = "avgDurationNs", defaultValue = ValueSupplier.ZeroSupplier.class)
    private double avgDurationNs;

    @ResultField(name = "minDurationNs", defaultValue = ValueSupplier.ZeroSupplier.class)
    private long minDurationNs;

    @ResultField(name = "maxDurationNs", defaultValue = ValueSupplier.ZeroSupplier.class)
    private long maxDurationNs;

    @ResultField(name = "totalDurationMs", defaultValue = ValueSupplier.ZeroSupplier.class)
    private long totalDurationMs;

    @ResultField(name = "avgDurationMs", defaultValue = ValueSupplier.ZeroSupplier.class)
    private double avgDurationMs;

    @ResultField(name = "monitorDuration", description = "监控时长(ms)", defaultValue = ValueSupplier.ZeroSupplier.class)
    private long monitorDuration;

    @ResultField(name = "exportPath", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String exportPath;

    public PerfHookResult() {}
    public PerfHookResult(String requestId) { super(requestId); }

    public int getTaskId() { return taskId; }
    public void setTaskId(int v) { taskId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public String getClassName() { return className; }
    public void setClassName(String v) { className = v; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String v) { methodName = v; }
    public String getSignature() { return signature; }
    public void setSignature(String v) { signature = v; }
    public int getCallCount() { return callCount; }
    public void setCallCount(int v) { callCount = v; }
    public long getTotalDurationNs() { return totalDurationNs; }
    public void setTotalDurationNs(long v) { totalDurationNs = v; }
    public double getAvgDurationNs() { return avgDurationNs; }
    public void setAvgDurationNs(double v) { avgDurationNs = v; }
    public long getMinDurationNs() { return minDurationNs; }
    public void setMinDurationNs(long v) { minDurationNs = v; }
    public long getMaxDurationNs() { return maxDurationNs; }
    public void setMaxDurationNs(long v) { maxDurationNs = v; }
    public long getTotalDurationMs() { return totalDurationMs; }
    public void setTotalDurationMs(long v) { totalDurationMs = v; }
    public double getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(double v) { avgDurationMs = v; }
    public long getMonitorDuration() { return monitorDuration; }
    public void setMonitorDuration(long v) { monitorDuration = v; }
    public String getExportPath() { return exportPath; }
    public void setExportPath(String v) { exportPath = v; }
}
