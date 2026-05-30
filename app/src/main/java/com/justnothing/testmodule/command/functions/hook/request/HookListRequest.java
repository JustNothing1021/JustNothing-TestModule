package com.justnothing.testmodule.command.functions.hook.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONObject;

@SerializeKeyName("hook:list")
public class HookListRequest extends CommandRequest {

    public HookListRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws org.json.JSONException {
        return super.toJson();
    }

    @Override
    public HookListRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        return this;
    }
}
