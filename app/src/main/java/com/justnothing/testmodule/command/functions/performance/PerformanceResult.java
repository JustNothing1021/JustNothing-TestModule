package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("PerformanceResult")
@AutoSerializable
public class PerformanceResult extends CommandResult {

    @ResultField(name = "subCommand", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String subCommand;

    @ResultField(name = "output", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String output;

    @ResultField(name = "subResult", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String subResult;

    @ResultField(name = "durationMs")
    private Long durationMs;

    @ResultField(name = "sampleDuration")
    private Long sampleDuration;

    @ResultField(name = "threadCount")
    private Integer threadCount;

    @ResultField(name = "memoryUsed")
    private Long memoryUsed;

    @ResultField(name = "cpuTimeMs")
    private Long cpuTimeMs;

    public PerformanceResult() { super(); }
    public PerformanceResult(String requestId) { super(requestId); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public String getSubResult() { return subResult; }
    public void setSubResult(String subResult) { this.subResult = subResult; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Long getSampleDuration() { return sampleDuration; }
    public void setSampleDuration(Long sampleDuration) { this.sampleDuration = sampleDuration; }

    public Integer getThreadCount() { return threadCount; }
    public void setThreadCount(Integer threadCount) { this.threadCount = threadCount; }

    public Long getMemoryUsed() { return memoryUsed; }
    public void setMemoryUsed(Long memoryUsed) { this.memoryUsed = memoryUsed; }

    public Long getCpuTimeMs() { return cpuTimeMs; }
    public void setCpuTimeMs(Long cpuTimeMs) { this.cpuTimeMs = cpuTimeMs; }
}
