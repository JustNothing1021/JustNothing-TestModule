package com.justnothing.testmodule.command.functions.watch;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONException;
import org.json.JSONObject;

public class WatchResult extends CommandResult {

    private String targetExpression;
    private String output;
    private Boolean active;
    private String valueType;
    private String currentValue;
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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (targetExpression != null) obj.put("targetExpression", targetExpression);
        if (output != null) obj.put("output", output);
        if (active != null) obj.put("active", active);
        if (valueType != null) obj.put("valueType", valueType);
        if (currentValue != null) obj.put("currentValue", currentValue);
        if (changeCount != null) obj.put("changeCount", changeCount);
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        targetExpression = obj.optString("targetExpression", null);
        output = obj.optString("output", null);
        active = obj.has("active") ? obj.getBoolean("active") : null;
        valueType = obj.optString("valueType", null);
        currentValue = obj.optString("currentValue", null);
        changeCount = obj.has("changeCount") ? obj.getLong("changeCount") : null;
    }
}
