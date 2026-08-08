package com.justnothing.testmodule.command.functions.watch.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("WatchClear")
public class WatchClearRequest extends CommandRequest {

    public WatchClearRequest() {
        super();
    }
}
