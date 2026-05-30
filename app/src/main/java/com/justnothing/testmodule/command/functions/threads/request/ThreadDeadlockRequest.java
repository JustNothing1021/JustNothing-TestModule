package com.justnothing.testmodule.command.functions.threads.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("threads:deadlock")
public class ThreadDeadlockRequest extends CommandRequest {

    public ThreadDeadlockRequest() {
        super();
    }
}
