package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("script:permission:reset")
public class ScriptPermResetRequest extends ScriptBaseRequest {

    public ScriptPermResetRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override
    public ScriptPermResetRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        return this;
    }
}
