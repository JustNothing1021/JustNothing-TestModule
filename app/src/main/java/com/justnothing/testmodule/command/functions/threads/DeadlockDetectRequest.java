package com.justnothing.testmodule.command.functions.threads;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONObject;

public class DeadlockDetectRequest extends CommandRequest {

    public DeadlockDetectRequest() {
        super();
    }

    @Override
    public DeadlockDetectRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        return this;
    }
}
