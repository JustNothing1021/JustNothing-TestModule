package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.framework.model.Command;
import com.justnothing.testmodule.command.framework.model.CommandResult;

public interface PerformanceCommand<Res extends CommandResult>
        extends Command<Res> {
}
