package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONException;
import org.json.JSONObject;

public class AliasClearRequest extends CommandRequest {

    public AliasClearRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override
    public AliasClearRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        return this;
    }
}
