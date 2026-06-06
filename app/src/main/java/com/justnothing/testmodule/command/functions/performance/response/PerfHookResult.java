package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("PerfHookResult")
public class PerfHookResult extends CommandResult {

    private int taskId;

    private String status;

    private String className;

    private String methodName;

    private String signature;

    private int callCount;

    private long totalDurationNs;

    private double avgDurationNs;

    private long minDurationNs;

    private long maxDurationNs;

    private long totalDurationMs;

    private double avgDurationMs;

    private long monitorDuration;

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
