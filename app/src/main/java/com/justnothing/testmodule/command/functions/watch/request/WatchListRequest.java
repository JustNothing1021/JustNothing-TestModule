package com.justnothing.testmodule.command.functions.watch.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("WatchList")
public class WatchListRequest extends CommandRequest {

    public WatchListRequest() {
        super();
    }
}
