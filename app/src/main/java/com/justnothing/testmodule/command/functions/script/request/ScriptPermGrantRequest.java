package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.functions.script.ScriptTexts;
import com.justnothing.testmodule.command.functions.script.response.ScriptResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class ScriptPermGrantRequest extends ScriptBaseRequest<ScriptResult> {

    @CmdParam(name = "permissions", position = 1, description = ScriptTexts.PARAM_SCRIPT_PERMISSION_GRANT_PERMISSIONS_DESC)
    private String permissions;

    public ScriptPermGrantRequest() {
        super();
    }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
}
