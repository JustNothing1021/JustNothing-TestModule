package com.justnothing.testmodule.command.functions.watch.response;

import com.justnothing.testmodule.command.base.protocol.CommandResult;

public class WatchCommandResult extends CommandResult {

    public WatchCommandResult() {
        super();
    }

    public WatchCommandResult(String requestId) {
        super(requestId);
    }
}
