package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.base.Command;

public interface ClassCommand<Req extends ClassCommandRequest, Res extends ClassCommandResult>
        extends Command<Req, Res> {
}
