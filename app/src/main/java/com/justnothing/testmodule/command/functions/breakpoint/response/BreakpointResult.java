package com.justnothing.testmodule.command.functions.breakpoint.response;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class BreakpointResult extends CommandResult {

    private String subCommand;
    private String output;
    private List<BreakpointInfo> breakpoints;
    private Integer totalBreakpoints;
    private Integer activeBreakpoints;

    public BreakpointResult() { super(); this.breakpoints = new ArrayList<>(); }
    public BreakpointResult(String requestId) { super(requestId); this.breakpoints = new ArrayList<>(); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public List<BreakpointInfo> getBreakpoints() { return breakpoints; }
    public void setBreakpoints(List<BreakpointInfo> breakpoints) { this.breakpoints = breakpoints; }
    public void addBreakpoint(BreakpointInfo bp) { this.breakpoints.add(bp); }

    public Integer getTotalBreakpoints() { return totalBreakpoints; }
    public void setTotalBreakpoints(Integer totalBreakpoints) { this.totalBreakpoints = totalBreakpoints; }

    public Integer getActiveBreakpoints() { return activeBreakpoints; }
    public void setActiveBreakpoints(Integer activeBreakpoints) { this.activeBreakpoints = activeBreakpoints; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (output != null) obj.put("output", output);
        if (totalBreakpoints != null) obj.put("totalBreakpoints", totalBreakpoints);
        if (activeBreakpoints != null) obj.put("activeBreakpoints", activeBreakpoints);
        if (breakpoints != null && !breakpoints.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (BreakpointInfo bp : breakpoints) arr.put(bp.toJson());
            obj.put("breakpoints", arr);
        }
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        subCommand = obj.optString("subCommand", null);
        output = obj.optString("output", null);
        totalBreakpoints = obj.has("totalBreakpoints") ? obj.getInt("totalBreakpoints") : null;
        activeBreakpoints = obj.has("activeBreakpoints") ? obj.getInt("activeBreakpoints") : null;
        if (obj.has("breakpoints")) {
            JSONArray arr = obj.getJSONArray("breakpoints");
            breakpoints = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) breakpoints.add(BreakpointInfo.fromJson(arr.getJSONObject(i)));
        }
    }

    public static class BreakpointInfo {
        private String id;
        private String className;
        private String methodName;
        private Integer lineNumber;
        private Boolean enabled;
        private Integer hitCount;

        public BreakpointInfo() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        public Integer getLineNumber() { return lineNumber; }
        public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public Integer getHitCount() { return hitCount; }
        public void setHitCount(Integer hitCount) { this.hitCount = hitCount; }

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            if (id != null) o.put("id", id);
            if (className != null) o.put("className", className);
            if (methodName != null) o.put("methodName", methodName);
            if (lineNumber != null) o.put("lineNumber", lineNumber);
            if (enabled != null) o.put("enabled", enabled);
            if (hitCount != null) o.put("hitCount", hitCount);
            return o;
        }

        public static BreakpointInfo fromJson(JSONObject obj) throws JSONException {
            BreakpointInfo info = new BreakpointInfo();
            info.id = obj.optString("id", null);
            info.className = obj.optString("className", null);
            info.methodName = obj.optString("methodName", null);
            info.lineNumber = obj.has("lineNumber") ? obj.getInt("lineNumber") : null;
            info.enabled = obj.has("enabled") ? obj.getBoolean("enabled") : null;
            info.hitCount = obj.has("hitCount") ? obj.getInt("hitCount") : null;
            return info;
        }
    }
}
