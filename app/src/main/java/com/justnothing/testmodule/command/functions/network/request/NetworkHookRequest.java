package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.network.response.NetworkResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class NetworkHookRequest extends CommandRequest<NetworkResult> {

    @CmdParam(name = "subCommand", required = false, description = "子命令 (add/remove/list/clear)")
    private String subCommand;

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }
}
