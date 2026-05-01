package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class BeanShellRequest extends CommandRequest {

    public static final String ACTION_EXECUTE = "execute";
    public static final String ACTION_VARS = "vars";
    public static final String ACTION_CLEAR = "clear";

    private String action;
    private String code;
    private String scriptName;
    private String scriptSubCmd;

    public BeanShellRequest() { super(); }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getScriptName() { return scriptName; }
    public void setScriptName(String scriptName) { this.scriptName = scriptName; }

    public String getScriptSubCmd() { return scriptSubCmd; }
    public void setScriptSubCmd(String scriptSubCmd) { this.scriptSubCmd = scriptSubCmd; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("action", action != null ? action : ACTION_EXECUTE);
        if (code != null) obj.put("code", code);
        if (scriptName != null) obj.put("scriptName", scriptName);
        if (scriptSubCmd != null) obj.put("scriptSubCmd", scriptSubCmd);
        return obj;
    }

    @Override
    public BeanShellRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setAction(obj.optString("action", ACTION_EXECUTE));
        setCode(obj.optString("code", null));
        setScriptName(obj.optString("scriptName", null));
        setScriptSubCmd(obj.optString("scriptSubCmd", null));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (String arg : args) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(arg);
            }
            code = sb.toString();
        }
        return this;
    }
}
