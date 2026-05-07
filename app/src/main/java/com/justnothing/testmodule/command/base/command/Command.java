package com.justnothing.testmodule.command.base.command;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

public interface Command<Res extends CommandResult> {
    Res execute(CommandExecutor.CmdExecContext<? extends CommandRequest> context);
    String getHelpText();
}
