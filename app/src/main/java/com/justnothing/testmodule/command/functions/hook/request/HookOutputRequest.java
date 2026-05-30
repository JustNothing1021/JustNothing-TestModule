package com.justnothing.testmodule.command.functions.hook.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("hook:output")
public class HookOutputRequest extends CommandRequest {

    @CmdParam(
        name = "hookId",
        position = 1,
        required = true,
        description = "Hook ID",
        serializedName = "hookId"
    )
    private String hookId;

    @CmdParam(
        name = "--count",
        required = false,
        defaultValue = "50",
        description = "输出条数",
        serializedName = "outputCount"
    )
    private int outputCount = 50;

    public HookOutputRequest() {
        super();
    }

    public String getHookId() { return hookId; }
    public void setHookId(String hookId) { this.hookId = hookId; }

    public int getOutputCount() { return outputCount; }
    public void setOutputCount(int outputCount) { this.outputCount = outputCount; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (hookId != null) obj.put("hookId", hookId);
        obj.put("outputCount", outputCount);
        return obj;
    }

    @Override
    public HookOutputRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setHookId(obj.optString("hookId", null));
        setOutputCount(obj.optInt("outputCount", 50));
        return this;
    }
}
