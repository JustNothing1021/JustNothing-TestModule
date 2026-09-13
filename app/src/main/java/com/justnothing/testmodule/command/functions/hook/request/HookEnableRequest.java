package com.justnothing.testmodule.command.functions.hook.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.hook.result.HookListResult;

public class HookEnableRequest extends CommandRequest<HookListResult> {

    @CmdParam(
        name = "hookId",
        position = 1,
        required = true,
        description = "Hook ID",
        serializedName = "hookId"
    )
    private String hookId;

    public HookEnableRequest() {
        super();
    }

    public String getHookId() { return hookId; }
    public void setHookId(String hookId) { this.hookId = hookId; }
}
