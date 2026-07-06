package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.framework.base.command.Command;
import com.justnothing.testmodule.command.framework.base.protocol.CommandResult;

public interface PerformanceCommand<Res extends CommandResult>
        extends Command<Res> {
}
