package com.justnothing.testmodule.command.functions.threads.response;

import com.justnothing.testmodule.command.base.protocol.CommandResult;

public class ThreadCommandResult extends CommandResult {

    public ThreadCommandResult() {
        super();
    }

    public ThreadCommandResult(String requestId) {
        super(requestId);
    }
}
