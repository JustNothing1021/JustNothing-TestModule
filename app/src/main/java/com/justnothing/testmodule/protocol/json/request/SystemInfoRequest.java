package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONException;
import org.json.JSONObject;

public class SystemInfoRequest extends CommandRequest {

    public SystemInfoRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override
    public SystemInfoRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        return this;
    }
}
