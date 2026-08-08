package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("script:manage")
public class ScriptManageRequest extends ScriptBaseRequest {

    public ScriptManageRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override
    public ScriptManageRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        return this;
    }
}
