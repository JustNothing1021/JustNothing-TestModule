package com.justnothing.testmodule.command.functions.hook;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONObject;

public class HookListRequest extends CommandRequest {

    public HookListRequest() {
        super();
    }


    @Override
    public HookListRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        return this;
    }
}
