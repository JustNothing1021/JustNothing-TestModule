package com.justnothing.testmodule.command.base;

import com.justnothing.testmodule.command.CommandExecutor;

public interface Command<Req extends CommandRequest, Res extends CommandResult> {

    Res execute(CommandExecutor.CmdExecContext<? extends CommandRequest> context);
    String getHelpText();
}
