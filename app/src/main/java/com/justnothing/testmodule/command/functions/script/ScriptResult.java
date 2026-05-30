package com.justnothing.testmodule.command.functions.script;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

import java.util.ArrayList;
import java.util.List;

public class ScriptResult extends CommandResult {

    @Expose @SerializedName("subCommand")
    private String subCommand;
    @Expose @SerializedName("scriptName")
    private String scriptName;
    @Expose @SerializedName("code")
    private String code;
    @Expose @SerializedName("output")
    private String output;
    @Expose @SerializedName("executionTimeMs")
    private Long executionTimeMs;
    @Expose @SerializedName("variables")
    private List<VariableInfo> variables;
    @Expose @SerializedName("scriptList")
    private List<String> scriptList;
    @Expose @SerializedName("permissionMask")
    private Long permissionMask;
    @Expose @SerializedName("deletedName")
    private String deletedName;
    @Expose @SerializedName("importedName")
    private String importedName;

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

    public Long getPermissionMask() { return permissionMask; }
    public void setPermissionMask(Long permissionMask) { this.permissionMask = permissionMask; }

    public String getDeletedName() { return deletedName; }
    public void setDeletedName(String deletedName) { this.deletedName = deletedName; }

    public String getImportedName() { return importedName; }
    public void setImportedName(String importedName) { this.importedName = importedName; }

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
