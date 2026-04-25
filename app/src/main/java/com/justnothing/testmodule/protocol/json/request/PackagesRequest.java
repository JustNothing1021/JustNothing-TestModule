package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONException;
import org.json.JSONObject;

public class PackagesRequest extends CommandRequest {

    public PackagesRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override
    public PackagesRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        return this;
    }
}
