package com.justnothing.testmodule.command.functions.script;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class ScriptResult extends CommandResult {

    private String subCommand;
    private String scriptName;
    private String code;
    private String output;
    private Long executionTimeMs;
    private List<VariableInfo> variables;
    private List<String> scriptList;

    public ScriptResult() {
        super();
    }

    public ScriptResult(String requestId) {
        super(requestId);
    }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getScriptName() { return scriptName; }
    public void setScriptName(String scriptName) { this.scriptName = scriptName; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public List<VariableInfo> getVariables() { return variables; }
    public void setVariables(List<VariableInfo> variables) { this.variables = variables; }

    public List<String> getScriptList() { return scriptList; }
    public void setScriptList(List<String> scriptList) { this.scriptList = scriptList; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (scriptName != null) obj.put("scriptName", scriptName);
        if (code != null) obj.put("code", code);
        if (output != null) obj.put("output", output);
        if (executionTimeMs != null) obj.put("executionTimeMs", executionTimeMs);
        if (variables != null && !variables.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (VariableInfo v : variables) {
                arr.put(v.toJson());
            }
            obj.put("variables", arr);
        }
        if (scriptList != null && !scriptList.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (String s : scriptList) {
                arr.put(s);
            }
            obj.put("scriptList", arr);
        }
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        subCommand = obj.optString("subCommand", null);
        scriptName = obj.optString("scriptName", null);
        code = obj.optString("code", null);
        output = obj.optString("output", null);
        executionTimeMs = obj.has("executionTimeMs") ? obj.getLong("executionTimeMs") : null;
        if (obj.has("variables")) {
            JSONArray arr = obj.getJSONArray("variables");
            variables = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                variables.add(VariableInfo.fromJson(arr.getJSONObject(i)));
            }
        }
        if (obj.has("scriptList")) {
            JSONArray arr = obj.getJSONArray("scriptList");
            scriptList = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                scriptList.add(arr.getString(i));
            }
        }
    }

    public static class VariableInfo {
        private String name;
        private String type;
        private String value;

        public VariableInfo() {}

        public VariableInfo(String name, Object value) {
            this.name = name;
            this.value = String.valueOf(value);
            this.type = value != null ? value.getClass().getSimpleName() : "null";
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("type", type != null ? type : "unknown");
            obj.put("value", value);
            return obj;
        }

        public static VariableInfo fromJson(JSONObject obj) throws JSONException {
            VariableInfo info = new VariableInfo();
            info.name = obj.optString("name", "");
            info.type = obj.optString("type", "unknown");
            info.value = obj.optString("value", null);
            return info;
        }
    }
}
