package com.justnothing.testmodule.command.functions.memory;

import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("MemoryInfo")
@AutoSerializable
public class MemoryInfoRequest extends CommandRequest {

    public static final String LEVEL_BASIC = "basic";
    public static final String LEVEL_FULL = "full";

    private String detailLevel;

    @FlagParam(names = {"-h", "--heap"}, description = "只显示堆内存信息")
    private boolean heapOnly = false;

    @FlagParam(names = {"-d", "--detailed"}, description = "显示详细内存信息 (默认)")
    private boolean detailed = true;

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

    public boolean isHeapOnly() {
        return heapOnly;
    }

    public void setHeapOnly(boolean heapOnly) {
        this.heapOnly = heapOnly;
    }

    public boolean isDetailed() {
        return detailed;
    }

    public void setDetailed(boolean detailed) {
        this.detailed = detailed;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("detailLevel", detailLevel);
        obj.put("heapOnly", heapOnly);
        obj.put("detailed", detailed);
        return obj;
    }

    @Override
    public MemoryInfoRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setDetailLevel(obj.optString("detailLevel", LEVEL_FULL));
        setHeapOnly(obj.optBoolean("heapOnly", false));
        setDetailed(obj.optBoolean("detailed", true));
        return this;
    }
}
