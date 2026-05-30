package com.justnothing.testmodule.command.functions.trace;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

public class TraceResult extends CommandResult {

    @Expose @SerializedName("targetClass")
    private String targetClass;
    @Expose @SerializedName("targetMethod")
    private String targetMethod;
    @Expose @SerializedName("output")
    private String output;
    @Expose @SerializedName("active")
    private Boolean active;
    @Expose @SerializedName("entryCount")
    private Long entryCount;

    public TraceResult() { super(); }
    public TraceResult(String requestId) { super(requestId); }

    public String getTargetClass() { return targetClass; }
    public void setTargetClass(String targetClass) { this.targetClass = targetClass; }

    public String getTargetMethod() { return targetMethod; }
    public void setTargetMethod(String targetMethod) { this.targetMethod = targetMethod; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Long getEntryCount() { return entryCount; }
    public void setEntryCount(Long entryCount) { this.entryCount = entryCount; }
}
