package com.justnothing.testmodule.command.functions.nativecmd;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONException;
import org.json.JSONObject;

public class NativeResult extends CommandResult {

    private String subCommand;
    private String libraryName;
    private String output;
    private Boolean loaded;
    private String libPath;
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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (libraryName != null) obj.put("libraryName", libraryName);
        if (output != null) obj.put("output", output);
        if (loaded != null) obj.put("loaded", loaded);
        if (libPath != null) obj.put("libPath", libPath);
        if (abi != null) obj.put("abi", abi);
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        subCommand = obj.optString("subCommand", null);
        libraryName = obj.optString("libraryName", null);
        output = obj.optString("output", null);
        loaded = obj.has("loaded") ? obj.getBoolean("loaded") : null;
        libPath = obj.optString("libPath", null);
        abi = obj.optString("abi", null);
    }
}
