package com.justnothing.testmodule.command.functions.threads.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("threads:profile:stop")
public class ThreadProfileStopRequest extends CommandRequest {

    public ThreadProfileStopRequest() {
        super();
    }
}
