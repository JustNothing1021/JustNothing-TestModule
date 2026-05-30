package com.justnothing.testmodule.command.functions.trace.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("trace:stop")
public class TraceStopRequest extends CommandRequest {

    @CmdParam(
        name = "id",
        position = 1,
        required = true,
        description = "跟踪任务 ID",
        serializedName = "traceId"
    )
    private int traceId;

    public TraceStopRequest() {
        super();
    }

    public int getTraceId() { return traceId; }
    public void setTraceId(int id) { this.traceId = id; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("traceId", traceId);
        return obj;
    }

    @Override
    public TraceStopRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setTraceId(obj.optInt("traceId", 0));
        return this;
    }
}
