package com.justnothing.testmodule.command.functions.memory;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class GcRequest extends CommandRequest {

    private boolean fullGc = false;

    public GcRequest() {
        super();
    }

    public GcRequest(boolean fullGc) {
        super();
        this.fullGc = fullGc;
    }

    public boolean isFullGc() {
        return fullGc;
    }

    public void setFullGc(boolean fullGc) {
        this.fullGc = fullGc;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("fullGc", fullGc);
        return obj;
    }

    @Override
    public GcRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setFullGc(obj.optBoolean("fullGc", false));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        for (String arg : args) {
            if ("--full".equals(arg)) {
                fullGc = true;
            }
        }
        return this;
    }
}
