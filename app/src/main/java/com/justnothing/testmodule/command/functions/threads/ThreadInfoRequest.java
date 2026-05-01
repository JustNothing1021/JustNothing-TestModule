package com.justnothing.testmodule.command.functions.threads;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONObject;

public class ThreadInfoRequest extends CommandRequest {

    public static final String LEVEL_BASIC = "basic";
    public static final String LEVEL_FULL = "full";

    private String detailLevel = LEVEL_FULL;
    private String filterState;

    public ThreadInfoRequest() {
        super();
    }

    public ThreadInfoRequest(String detailLevel) {
        super();
        this.detailLevel = detailLevel;
    }

    public String getDetailLevel() {
        return detailLevel;
    }

    public void setDetailLevel(String detailLevel) {
        this.detailLevel = detailLevel;
    }

    public String getFilterState() {
        return filterState;
    }

    public void setFilterState(String filterState) {
        this.filterState = filterState;
    }

    @Override
    public JSONObject toJson() throws org.json.JSONException {
        JSONObject obj = super.toJson();
        obj.put("detailLevel", detailLevel);
        if (filterState != null && !filterState.isEmpty()) {
            obj.put("filterState", filterState);
        }
        return obj;
    }

    @Override
    public ThreadInfoRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setDetailLevel(obj.optString("detailLevel", LEVEL_FULL));
        setFilterState(obj.optString("filterState", null));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        for (String arg : args) {
            if ("--basic".equals(arg)) {
                detailLevel = LEVEL_BASIC;
            } else if ("--full".equals(arg)) {
                detailLevel = LEVEL_FULL;
            } else if (arg.startsWith("--filter=")) {
                filterState = arg.substring(9);
            }
        }
        return this;
    }
}
