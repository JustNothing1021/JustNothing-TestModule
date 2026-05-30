package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("network:info")
public class NetworkInfoRequest extends CommandRequest {

    @CmdParam(
        name = "id",
        position = 1,
        required = true,
        description = "请求 ID",
        serializedName = "targetRequestId"
    )
    private int targetRequestId;

    public NetworkInfoRequest() {
        super();
    }

    public int getTargetRequestId() { return targetRequestId; }
    public void setTargetRequestId(int id) { this.targetRequestId = id; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("targetRequestId", targetRequestId);
        return obj;
    }

    @Override
    public NetworkInfoRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setTargetRequestId(obj.optInt("targetRequestId", 0));
        return this;
    }
}
