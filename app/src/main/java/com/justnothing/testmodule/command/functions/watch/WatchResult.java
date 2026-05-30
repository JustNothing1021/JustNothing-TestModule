package com.justnothing.testmodule.command.functions.watch;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

public class WatchResult extends CommandResult {

    @Expose @SerializedName("targetExpression")
    private String targetExpression;
    @Expose @SerializedName("output")
    private String output;
    @Expose @SerializedName("active")
    private Boolean active;
    @Expose @SerializedName("valueType")
    private String valueType;
    @Expose @SerializedName("currentValue")
    private String currentValue;
    @Expose @SerializedName("changeCount")
    private Long changeCount;

    public WatchResult() { super(); }
    public WatchResult(String requestId) { super(requestId); }

    public String getTargetExpression() { return targetExpression; }
    public void setTargetExpression(String targetExpression) { this.targetExpression = targetExpression; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }

    public String getCurrentValue() { return currentValue; }
    public void setCurrentValue(String currentValue) { this.currentValue = currentValue; }

    public Long getChangeCount() { return changeCount; }
    public void setChangeCount(Long changeCount) { this.changeCount = changeCount; }
}
