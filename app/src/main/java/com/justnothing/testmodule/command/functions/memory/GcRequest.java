package com.justnothing.testmodule.command.functions.memory;

import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("Gc")
@AutoSerializable
public class GcRequest extends CommandRequest {

    @FlagParam(names = {"--full"}, description = "执行完整的GC")
    private boolean fullGc = false;

    @FlagParam(names = {"--stats"}, description = "显示GC统计信息")
    private boolean showStats = false;

    public GcRequest() {
        super();
    }

    public GcRequest(boolean fullGc) {
        super();
        this.fullGc = fullGc;
    }

    public boolean isFullGc() {
        return fullGc;
    }

    public void setFullGc(boolean fullGc) {
        this.fullGc = fullGc;
    }

    public boolean isShowStats() {
        return showStats;
    }

    public void setShowStats(boolean showStats) {
        this.showStats = showStats;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("fullGc", fullGc);
        obj.put("showStats", showStats);
        return obj;
    }

    @Override
    public GcRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setFullGc(obj.optBoolean("fullGc", false));
        setShowStats(obj.optBoolean("showStats", false));
        return this;
    }
}
