package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("PerformanceResult")
public class PerformanceResult extends CommandResult {

    private String subCommand;

    private String output;

    private String subResult;

    private Long durationMs;

    private Long sampleDuration;

    private Integer threadCount;

    private Long memoryUsed;

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
