package com.justnothing.testmodule.command.functions.bsh.response;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class BeanShellResult extends CommandResult {

    private String action;
    private String code;
    private String output;
    private List<VariableInfo> variables;

    public BeanShellResult() {
        super();
    }

    public BeanShellResult(String requestId) {
        super(requestId);
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public List<VariableInfo> getVariables() { return variables; }
    public void setVariables(List<VariableInfo> variables) { this.variables = variables; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (action != null) obj.put("action", action);
        if (code != null) obj.put("code", code);
        if (output != null) obj.put("output", output);
        if (variables != null && !variables.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (VariableInfo v : variables) {
                arr.put(v.toJson());
            }
            obj.put("variables", arr);
        }
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        action = obj.optString("action", null);
        code = obj.optString("code", null);
        output = obj.optString("output", null);
        if (obj.has("variables")) {
            JSONArray arr = obj.getJSONArray("variables");
            variables = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                variables.add(VariableInfo.fromJson(arr.getJSONObject(i)));
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
