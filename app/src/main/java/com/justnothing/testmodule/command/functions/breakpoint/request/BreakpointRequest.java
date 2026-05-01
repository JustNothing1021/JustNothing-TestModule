package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class BreakpointRequest extends CommandRequest {

    public static final String SUB_ADD = "add";
    public static final String SUB_LIST = "list";
    public static final String SUB_ENABLE = "enable";
    public static final String SUB_DISABLE = "disable";
    public static final String SUB_REMOVE = "remove";
    public static final String SUB_CLEAR = "clear";
    public static final String SUB_HITS = "hits";

    private String subCommand;
    private String className;
    private String methodName;
    private String signature;
    private String id;

    public BreakpointRequest() { super(); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (className != null) obj.put("className", className);
        if (methodName != null) obj.put("methodName", methodName);
        if (signature != null) obj.put("signature", signature);
        if (id != null) obj.put("id", id);
        return obj;
    }

    @Override
    public BreakpointRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setSubCommand(obj.optString("subCommand", SUB_LIST));
        setClassName(obj.optString("className", null));
        setMethodName(obj.optString("methodName", null));
        setSignature(obj.optString("signature", null));
        setId(obj.optString("id", null));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) subCommand = args[0];
        int pos = 1;
        if (args.length > 1 && !isOption(args[1])) className = args[pos++];
        if (args.length > pos && !isOption(args[pos])) methodName = args[pos++];
        for (int i = pos; i < args.length; i++) {
            String arg = args[i];
            if ("sig".equals(arg) || "signature".equals(arg)) {
                if (i + 1 < args.length) signature = args[++i];
            } else if (!isOption(arg) && signature == null) {
                signature = arg;
            }
        }
        if (SUB_ENABLE.equals(subCommand) || SUB_DISABLE.equals(subCommand) || SUB_REMOVE.equals(subCommand)) {
            if (args.length > 1) id = args[1];
        }
        return this;
    }

    private boolean isOption(String s) {
        return s.startsWith("-");
    }
}
