package com.justnothing.testmodule.command.framework.base.command;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.protocol.CommandResult;

public interface Command<Res extends CommandResult> {
    Res execute(CommandExecutor.CmdExecContext<? extends CommandRequest> context);
    String getHelpText();
}
