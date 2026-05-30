package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("script:permission:preset")
public class ScriptPermPresetRequest extends ScriptBaseRequest {

    @CmdParam(name = "presetName", position = 1, description = "预设名称(sandbox/expression/minimal/full)")
    private String presetName;

    public ScriptPermPresetRequest() {
        super();
    }

    public String getPresetName() { return presetName; }
    public void setPresetName(String presetName) { this.presetName = presetName; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("presetName", presetName);
        return obj;
    }

    @Override
    public ScriptPermPresetRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setPresetName(obj.optString("presetName"));
        return this;
    }
}
