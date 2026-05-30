package com.justnothing.testmodule.command.functions.trace.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("trace:clear")
public class TraceClearRequest extends CommandRequest {

    public TraceClearRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override
    public TraceClearRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        return this;
    }
}
