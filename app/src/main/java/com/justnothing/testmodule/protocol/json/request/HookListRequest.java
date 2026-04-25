package com.justnothing.testmodule.protocol.json.request;

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
}
