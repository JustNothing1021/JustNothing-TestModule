package com.justnothing.testmodule.command.functions.hook.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONObject;

@SerializeKeyName("hook:clear")
public class HookClearRequest extends CommandRequest {

    public HookClearRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws org.json.JSONException {
        return super.toJson();
    }

    @Override
    public HookClearRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        return this;
    }
}
