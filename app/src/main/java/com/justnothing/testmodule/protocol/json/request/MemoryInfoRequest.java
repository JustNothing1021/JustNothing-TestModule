package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONException;
import org.json.JSONObject;

public class MemoryInfoRequest extends CommandRequest {

    public static final String LEVEL_BASIC = "basic";
    public static final String LEVEL_FULL = "full";

    private String detailLevel = LEVEL_FULL;

    public MemoryInfoRequest() {
        super();
    }

    public MemoryInfoRequest(String detailLevel) {
        super();
        this.detailLevel = detailLevel;
    }

    public String getDetailLevel() {
        return detailLevel;
    }

    public void setDetailLevel(String detailLevel) {
        this.detailLevel = detailLevel;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("detailLevel", detailLevel);
        return obj;
    }

    @Override
    public MemoryInfoRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setDetailLevel(obj.optString("detailLevel", LEVEL_FULL));
        return this;
    }
}
