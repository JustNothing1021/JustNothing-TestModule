package com.justnothing.testmodule.command.functions.packages;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.SubCommand;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("Packages")
@SubCommand("list")
public class PackagesRequest extends CommandRequest {

    public PackagesRequest() {
        super();
    }

    @Override
    public String getCommandType() {
        return "packages";
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override
    public PackagesRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        return this;
    }
}
