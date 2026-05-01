package com.justnothing.testmodule.command.functions.trace;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONException;
import org.json.JSONObject;

public class TraceResult extends CommandResult {

    private String targetClass;
    private String targetMethod;
    private String output;
    private Boolean active;
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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (targetClass != null) obj.put("targetClass", targetClass);
        if (targetMethod != null) obj.put("targetMethod", targetMethod);
        if (output != null) obj.put("output", output);
        if (active != null) obj.put("active", active);
        if (entryCount != null) obj.put("entryCount", entryCount);
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        targetClass = obj.optString("targetClass", null);
        targetMethod = obj.optString("targetMethod", null);
        output = obj.optString("output", null);
        active = obj.has("active") ? obj.getBoolean("active") : null;
        entryCount = obj.has("entryCount") ? obj.getLong("entryCount") : null;
    }
}
