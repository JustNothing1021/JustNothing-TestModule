package com.justnothing.testmodule.command.functions.alias.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.parser.PositionalParam;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("AliasRemove")
@SubCommand("remove")
public class AliasRemoveRequest extends CommandRequest {

    @PositionalParam(order = 1, name = "别名", required = true, description = "要删除的别名名称")
    private String name;

    public AliasRemoveRequest() {
        super();
    }

    public AliasRemoveRequest(String name) {
        super();
        this.name = name;
    }

    @Override
    public String getCommandType() {
        return "AliasRemove";
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
