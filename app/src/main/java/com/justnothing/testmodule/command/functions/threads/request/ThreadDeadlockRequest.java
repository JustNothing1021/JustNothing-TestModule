package com.justnothing.testmodule.command.functions.threads.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("threads:deadlock")
public class ThreadDeadlockRequest extends CommandRequest {

    public ThreadDeadlockRequest() {
        super();
    }
}
