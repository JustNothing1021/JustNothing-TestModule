package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("script:run")
public class ScriptRunRequest extends ScriptBaseRequest {

    @CmdParam(name = "name", position = 1, description = "脚本名称")
    private String name;

    public ScriptRunRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("name", name);
        return obj;
    }

    @Override
    public ScriptRunRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setName(obj.optString("name"));
        return this;
    }
}
