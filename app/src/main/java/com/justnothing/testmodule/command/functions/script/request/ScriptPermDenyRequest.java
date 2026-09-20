package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.functions.script.response.ScriptResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class ScriptPermDenyRequest extends ScriptBaseRequest<ScriptResult> {

    @CmdParam(name = "permissions", position = 1, description = "权限列表(逗号分隔)")
    private String permissions;

    public ScriptPermDenyRequest() {
        super();
    }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
}
