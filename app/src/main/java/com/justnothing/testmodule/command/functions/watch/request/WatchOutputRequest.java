package com.justnothing.testmodule.command.functions.watch.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.watch.response.WatchOutputResult;
import com.justnothing.testmodule.command.functions.watch.WatchTexts;

public class WatchOutputRequest extends CommandRequest<WatchOutputResult> {

    @CmdParam(
        name = "target",
        description = WatchTexts.PARAM_WATCH_OUTPUT_TARGET_DESC,
        required = false,
        position = 1
    )
    private String target;

    @CmdParam(
        name = "limit",
        description = WatchTexts.PARAM_WATCH_OUTPUT_LIMIT_DESC,
        required = false,
        defaultValue = "20",
        position = 2
    )
    private Integer limit;

    public WatchOutputRequest() {
        super();
        this.limit = 20;
    }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
