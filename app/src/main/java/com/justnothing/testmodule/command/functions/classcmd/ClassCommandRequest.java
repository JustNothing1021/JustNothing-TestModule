package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;


public abstract class ClassCommandRequest<R extends CommandResult> extends CommandRequest<R> {}
