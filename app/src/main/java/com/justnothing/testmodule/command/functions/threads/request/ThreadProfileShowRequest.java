package com.justnothing.testmodule.command.functions.threads.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;

@SerializeKeyName("threads:profile:show")
public class ThreadProfileShowRequest extends CommandRequest {

    public ThreadProfileShowRequest() {
        super();
    }
}
