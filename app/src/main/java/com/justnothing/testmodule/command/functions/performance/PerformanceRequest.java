package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;

public abstract class PerformanceRequest<R extends CommandResult> extends CommandRequest<R> {
}
