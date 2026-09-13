package com.justnothing.testmodule.command.framework.model;

import com.justnothing.testmodule.command.framework.CommandExecutor;

public interface Command<Res extends CommandResult> {
    Res execute(CommandExecutor.CmdExecContext<? extends CommandRequest<?>> context);
    String getHelpText();
}
