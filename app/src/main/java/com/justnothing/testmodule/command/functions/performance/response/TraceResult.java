package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import java.util.List;

@SerializeKeyName("TraceResult")
public class TraceResult extends CommandResult {

    private int taskId;

    private String status;

    private int traceCount;

    private List<TraceEntry> traces;

    private String exportPath;

    public static class TraceEntry {
        private String name;

        private long startTime;

        private long duration;

        private int threadId;

        private String threadName;

        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public long getStartTime() { return startTime; }
        public void setStartTime(long v) { startTime = v; }
        public long getDuration() { return duration; }
        public void setDuration(long v) { duration = v; }
        public int getThreadId() { return threadId; }
        public void setThreadId(int v) { threadId = v; }
        public String getThreadName() { return threadName; }
        public void setThreadName(String v) { threadName = v; }
    }

    public TraceResult() {}
    public TraceResult(String requestId) { super(requestId); }

    public int getTaskId() { return taskId; }
    public void setTaskId(int v) { taskId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public int getTraceCount() { return traceCount; }
    public void setTraceCount(int v) { traceCount = v; }
    public List<TraceEntry> getTraces() { return traces; }
    public void setTraces(List<TraceEntry> v) { traces = v; }
    public String getExportPath() { return exportPath; }
    public void setExportPath(String v) { exportPath = v; }
}
