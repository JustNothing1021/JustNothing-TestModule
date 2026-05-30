package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("script:permission:grant")
public class ScriptPermGrantRequest extends ScriptBaseRequest {

    @CmdParam(name = "permissions", position = 1, description = "权限列表(逗号分隔)")
    private String permissions;

    public ScriptPermGrantRequest() {
        super();
    }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("permissions", permissions);
        return obj;
    }

    @Override
    public ScriptPermGrantRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setPermissions(obj.optString("permissions"));
        return this;
    }
}
