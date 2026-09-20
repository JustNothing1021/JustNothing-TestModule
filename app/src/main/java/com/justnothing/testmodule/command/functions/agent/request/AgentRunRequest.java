package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.agent.AgentTexts;

public class AgentRunRequest extends CommandRequest<CommandResult> {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, required = true, description = AgentTexts.PARAM_AGENT_RUN_PKG_DESC)
    private String packageName;

    @Expose @SerializedName("command")
    @CmdParam(name = "command", position = 2, varArgs = true, required = true,
              description = AgentTexts.PARAM_AGENT_RUN_COMMAND_DESC)
    private String command;

    public String getPackageName() { return packageName; }
    public void setPackageName(String pkg) { this.packageName = pkg; }

    public String getCommand() { return command; }
    public void setCommand(String cmd) { this.command = cmd; }
}
