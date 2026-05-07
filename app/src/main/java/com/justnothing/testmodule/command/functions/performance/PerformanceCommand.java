package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.base.command.Command;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

public interface PerformanceCommand<Req extends PerformanceRequest, Res extends CommandResult>
        extends Command<Res> {
}
