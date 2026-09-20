package com.justnothing.testmodule.command.functions.hook.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;

public class HookOutputRequest extends CommandRequest<CommandResult> {

    @CmdParam(
        name = "hookId",
        position = 1,
        required = true,
        description = "Hook ID",
        serializedName = "hookId"
    )
    private String hookId;

    @CmdParam(
        name = "--count",
        required = false,
        defaultValue = "50",
        description = "输出条数",
        serializedName = "outputCount"
    )
    private int outputCount = 50;

    public HookOutputRequest() {
        super();
    }

    public String getHookId() { return hookId; }
    public void setHookId(String hookId) { this.hookId = hookId; }

    public int getOutputCount() { return outputCount; }
    public void setOutputCount(int outputCount) { this.outputCount = outputCount; }
}
