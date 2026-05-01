package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONException;
import org.json.JSONObject;

public class PerformanceResult extends CommandResult {

    private String subCommand;
    private String output;
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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (output != null) obj.put("output", output);
        if (durationMs != null) obj.put("durationMs", durationMs);
        if (sampleDuration != null) obj.put("sampleDuration", sampleDuration);
        if (threadCount != null) obj.put("threadCount", threadCount);
        if (memoryUsed != null) obj.put("memoryUsed", memoryUsed);
        if (cpuTimeMs != null) obj.put("cpuTimeMs", cpuTimeMs);
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        subCommand = obj.optString("subCommand", null);
        output = obj.optString("output", null);
        durationMs = obj.has("durationMs") ? obj.getLong("durationMs") : null;
        sampleDuration = obj.has("sampleDuration") ? obj.getLong("sampleDuration") : null;
        threadCount = obj.has("threadCount") ? obj.getInt("threadCount") : null;
        memoryUsed = obj.has("memoryUsed") ? obj.getLong("memoryUsed") : null;
        cpuTimeMs = obj.has("cpuTimeMs") ? obj.getLong("cpuTimeMs") : null;
    }
}
