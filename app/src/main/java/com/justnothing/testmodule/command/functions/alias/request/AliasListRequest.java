package com.justnothing.testmodule.command.functions.alias.request;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class AliasListRequest extends CommandRequest {

    public AliasListRequest() {
        super();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override
    public AliasListRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        return this;
    }
}
