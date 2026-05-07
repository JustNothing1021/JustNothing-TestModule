package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.parser.KeywordParam;
import com.justnothing.testmodule.command.base.parser.PositionalParam;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("TestAliasAdd")
public class TestAliasAddRequest extends CommandRequest {

    @PositionalParam(order = 1, name = "别名名称", required = true)
    private String name;

    @PositionalParam(order = 2, name = "完整命令", required = true, varArgs = true)
    private String command;

    @FlagParam(names = {"-v", "--verbose"}, description = "显示详细信息")
    private boolean verbose;

    @FlagParam(names = {"-f", "--force"}, description = "强制覆盖")
    private boolean force;

    @KeywordParam(name = "description", description = "别名描述")
    private String description;

    public String getName() { return name; }
    public String getCommand() { return command; }
    public boolean isVerbose() { return verbose; }
    public boolean isForce() { return force; }
    public String getDescription() { return description; }

    @Override
    public String getCommandType() {
        return "test-alias-add";
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        if (name != null) json.put("name", name);
        if (command != null) json.put("command", command);
        json.put("verbose", verbose);
        json.put("force", force);
        if (description != null) json.put("description", description);
        return json;
    }

    @Override
    public CommandRequest fromJson(JSONObject obj) throws JSONException {
        this.setRequestId(obj.optString("requestId"));
        this.name = obj.optString("name", null);
        this.command = obj.optString("command", null);
        this.verbose = obj.optBoolean("verbose", false);
        this.force = obj.optBoolean("force", false);
        this.description = obj.optString("description", null);
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(TestAliasAddRequest.class, args);
    }
}
