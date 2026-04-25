package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONException;
import org.json.JSONObject;

public class ExportContextRequest extends CommandRequest {

    public ExportContextRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override
    public ExportContextRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        return this;
    }
}
