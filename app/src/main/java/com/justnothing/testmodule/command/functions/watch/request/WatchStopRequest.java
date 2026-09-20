package com.justnothing.testmodule.command.functions.watch.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.watch.response.WatchStopResult;
import com.justnothing.testmodule.command.functions.watch.WatchTexts;

public class WatchStopRequest extends CommandRequest<WatchStopResult> {

    @CmdParam(
        name = "watchId",
        description = WatchTexts.PARAM_WATCH_STOP_WATCHID_DESC,
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
