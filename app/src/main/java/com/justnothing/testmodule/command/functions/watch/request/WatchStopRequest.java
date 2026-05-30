package com.justnothing.testmodule.command.functions.watch.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("WatchStop")
public class WatchStopRequest extends CommandRequest {

    @CmdParam(
        name = "watchId",
        description = "要停止的Watch ID",
        required = false,
        position = 1
    )
    private Integer watchId;

    public WatchStopRequest() {
        super();
    }

    public Integer getWatchId() { return watchId; }
    public void setWatchId(Integer watchId) { this.watchId = watchId; }
}
