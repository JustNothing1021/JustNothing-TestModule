package com.justnothing.testmodule.command.functions.watch.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("WatchList")
public class WatchListRequest extends CommandRequest {

    public WatchListRequest() {
        super();
    }
}
