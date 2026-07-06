package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.command.CmdParam;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;

@SerializeKeyName("network:hook")
public class NetworkHookRequest extends CommandRequest {

    @CmdParam(name = "subCommand", required = false, description = "子命令 (add/remove/list/clear)")
    private String subCommand;

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }
}
