package com.justnothing.testmodule.command.functions.hook.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.hook.HookTexts;
import com.justnothing.testmodule.command.functions.hook.response.HookListResult;

public class HookDisableRequest extends CommandRequest<HookListResult> {

    @CmdParam(
        name = "hookId",
        position = 1,
        required = true,
        description = HookTexts.PARAM_HOOK_DISABLE_HOOKID_DESC,
        serializedName = "hookId"
    )
    private String hookId;

    public HookDisableRequest() {
        super();
    }

    public String getHookId() { return hookId; }
    public void setHookId(String hookId) { this.hookId = hookId; }
}
