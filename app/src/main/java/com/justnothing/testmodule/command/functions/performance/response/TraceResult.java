package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;

import java.util.List;

@SerializeKeyName("TraceResult")
@AutoSerializable
public class TraceResult extends CommandResult {

    @ResultField(name = "taskId", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int taskId;

    @ResultField(name = "status", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String status;

    @ResultField(name = "traceCount", description = "Trace段数量", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int traceCount;

    @ResultField(name = "traces", description = "Trace数据列表")
    private List<TraceEntry> traces;

    @ResultField(name = "exportPath", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String exportPath;

    @AutoSerializable
    public static class TraceEntry {
        @ResultField(name = "name", defaultValue = ValueSupplier.EmptyStringSupplier.class)
        private String name;

        @ResultField(name = "startTime", defaultValue = ValueSupplier.ZeroSupplier.class)
        private long startTime;

        @ResultField(name = "duration", defaultValue = ValueSupplier.ZeroSupplier.class)
        private long duration;

        @ResultField(name = "threadId", defaultValue = ValueSupplier.ZeroSupplier.class)
        private int threadId;

        @ResultField(name = "threadName", defaultValue = ValueSupplier.EmptyStringSupplier.class)
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
