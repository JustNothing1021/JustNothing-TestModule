package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONException;
import org.json.JSONObject;

public class HookActionRequest extends CommandRequest {

    public static final String ACTION_REMOVE = "remove";
    public static final String ACTION_INFO = "info";
    public static final String ACTION_ENABLE = "enable";
    public static final String ACTION_DISABLE = "disable";
    public static final String ACTION_OUTPUT = "output";
    public static final String ACTION_CLEAR = "clear";

    private String action;
    private String hookId;
    private int outputCount;

    public HookActionRequest() {
        super();
        this.outputCount = 50;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getHookId() { return hookId; }
    public void setHookId(String hookId) { this.hookId = hookId; }

    public int getOutputCount() { return outputCount; }
    public void setOutputCount(int outputCount) { this.outputCount = outputCount; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("action", action);
        if (hookId != null) obj.put("hookId", hookId);
        if (ACTION_OUTPUT.equals(action)) {
            obj.put("outputCount", outputCount);
        }
        return obj;
    }

    @Override
    public HookActionRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setAction(obj.optString("action", ""));
        setHookId(obj.optString("hookId", null));
        setOutputCount(obj.optInt("outputCount", 50));
        return this;
    }
}
