package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("perf:list")
public class PerfListRequest extends PerformanceRequest {

    public PerfListRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override
    public PerfListRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        return this;
    }
}
