package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("script:vars")
public class ScriptVarsRequest extends ScriptBaseRequest {

    public ScriptVarsRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override
    public ScriptVarsRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        return this;
    }
}
