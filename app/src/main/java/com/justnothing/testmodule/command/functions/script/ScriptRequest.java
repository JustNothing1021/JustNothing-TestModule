package com.justnothing.testmodule.command.functions.script;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class ScriptRequest extends CommandRequest {

    public static final String SUB_RUN = "run";
    public static final String SUB_CLEAR = "clear";
    public static final String SUB_VARS = "vars";

    private String subCommand;
    private String scriptPath;
    private String scriptCode;

    public ScriptRequest() { super(); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getScriptPath() { return scriptPath; }
    public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }

    public String getScriptCode() { return scriptCode; }
    public void setScriptCode(String scriptCode) { this.scriptCode = scriptCode; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (scriptPath != null) obj.put("scriptPath", scriptPath);
        if (scriptCode != null) obj.put("scriptCode", scriptCode);
        return obj;
    }

    @Override
    public ScriptRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setSubCommand(obj.optString("subCommand", SUB_RUN));
        setScriptPath(obj.optString("scriptPath", null));
        setScriptCode(obj.optString("scriptCode", null));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) subCommand = "run";
        if (args.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (String arg : args) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(arg);
            }
            scriptCode = sb.toString();
        }
        return this;
    }
}
