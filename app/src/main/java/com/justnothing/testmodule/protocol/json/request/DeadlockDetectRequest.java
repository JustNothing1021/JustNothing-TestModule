package com.justnothing.testmodule.protocol.json.request;

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
}
