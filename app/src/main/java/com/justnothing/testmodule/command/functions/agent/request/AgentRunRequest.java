package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("AgentRun")
public class AgentRunRequest extends CommandRequest {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, required = true, description = "目标应用包名")
    private String packageName;

    @Expose @SerializedName("command")
    @CmdParam(name = "command", position = 2, varArgs = true, required = true,
              description = "要在目标应用上执行的命令（剩余所有参数）")
    private String command;

    public String getPackageName() { return packageName; }
    public void setPackageName(String pkg) { this.packageName = pkg; }

    public String getCommand() { return command; }
    public void setCommand(String cmd) { this.command = cmd; }
}
