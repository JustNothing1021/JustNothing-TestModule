package com.justnothing.testmodule.command.functions.watch.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;

@SerializeKeyName("WatchClear")
public class WatchClearRequest extends CommandRequest {

    public WatchClearRequest() {
        super();
    }
}
