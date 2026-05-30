package com.justnothing.testmodule.command.functions.nativecmd;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

public class NativeResult extends CommandResult {

    @Expose @SerializedName("subCommand")
    private String subCommand;
    @Expose @SerializedName("libraryName")
    private String libraryName;
    @Expose @SerializedName("output")
    private String output;
    @Expose @SerializedName("loaded")
    private Boolean loaded;
    @Expose @SerializedName("libPath")
    private String libPath;
    @Expose @SerializedName("abi")
    private String abi;

    public NativeResult() { super(); }
    public NativeResult(String requestId) { super(requestId); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getLibraryName() { return libraryName; }
    public void setLibraryName(String libraryName) { this.libraryName = libraryName; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public Boolean getLoaded() { return loaded; }
    public void setLoaded(Boolean loaded) { this.loaded = loaded; }

    public String getLibPath() { return libPath; }
    public void setLibPath(String libPath) { this.libPath = libPath; }

    public String getAbi() { return abi; }
    public void setAbi(String abi) { this.abi = abi; }
}
