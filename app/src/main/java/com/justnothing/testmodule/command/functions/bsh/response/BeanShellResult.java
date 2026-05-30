package com.justnothing.testmodule.command.functions.bsh.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

import java.util.ArrayList;
import java.util.List;

public class BeanShellResult extends CommandResult {

    @Expose @SerializedName("action")
    private String action;
    @Expose @SerializedName("code")
    private String code;
    @Expose @SerializedName("output")
    private String output;
    @Expose @SerializedName("variables")
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

    public static class VariableInfo {
        @Expose @SerializedName("name")
        private String name;
        @Expose @SerializedName("type")
        private String type;
        @Expose @SerializedName("value")
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
    }
}
