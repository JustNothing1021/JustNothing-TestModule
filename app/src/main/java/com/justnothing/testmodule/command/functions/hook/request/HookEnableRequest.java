package com.justnothing.testmodule.command.functions.hook.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("hook:enable")
public class HookEnableRequest extends CommandRequest {

    @CmdParam(
        name = "hookId",
        position = 1,
        required = true,
        description = "Hook ID",
        serializedName = "hookId"
    )
    private String hookId;

    public HookEnableRequest() {
        super();
    }

    public String getHookId() { return hookId; }
    public void setHookId(String hookId) { this.hookId = hookId; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (hookId != null) obj.put("hookId", hookId);
        return obj;
    }

    @Override
    public HookEnableRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setHookId(obj.optString("hookId", null));
        return this;
    }
}
