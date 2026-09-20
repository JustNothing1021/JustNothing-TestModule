package com.justnothing.testmodule.command.functions.hook.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.functions.hook.HookTexts;

public class HookInfoRequest extends CommandRequest<CommandResult> {

    @CmdParam(
        name = "hookId",
        position = 1,
        required = true,
        description = HookTexts.PARAM_HOOK_INFO_HOOKID_DESC,
        serializedName = "hookId"
    )
    private String hookId;

    public HookInfoRequest() {
        super();
    }

    public String getHookId() { return hookId; }
    public void setHookId(String hookId) { this.hookId = hookId; }
}
