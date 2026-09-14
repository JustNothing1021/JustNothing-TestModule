package com.justnothing.testmodule.command.functions.bytecode.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.model.CommandResult;

import java.util.List;

public class BytecodeResult extends CommandResult {

    @Expose @SerializedName("subCommand")
    private String subCommand;
    @Expose @SerializedName("className")
    private String className;
    @Expose @SerializedName("output")
    private String output;
    @Expose @SerializedName("source")
    private String source;
    @Expose @SerializedName("dexTrust")
    private String dexTrust;
    @Expose @SerializedName("files")
    private List<String> files;

    public BytecodeResult() {
        super();
    }

    public BytecodeResult(String requestId) {
        super(requestId);
    }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDexTrust() { return dexTrust; }
    public void setDexTrust(String dexTrust) { this.dexTrust = dexTrust; }

    public List<String> getFiles() { return files; }
    public void setFiles(List<String> files) { this.files = files; }
}
