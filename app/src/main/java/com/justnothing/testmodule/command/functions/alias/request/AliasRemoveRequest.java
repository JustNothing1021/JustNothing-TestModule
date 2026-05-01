package com.justnothing.testmodule.command.functions.alias.request;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class AliasRemoveRequest extends CommandRequest {

    private String name;

    public AliasRemoveRequest() {
        super();
    }

    public AliasRemoveRequest(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("name", name);
        return obj;
    }

    @Override
    public AliasRemoveRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        this.name = obj.optString("name");
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) name = args[0];
        return this;
    }
}
