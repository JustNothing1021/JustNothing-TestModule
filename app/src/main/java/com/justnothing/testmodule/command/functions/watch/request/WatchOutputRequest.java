package com.justnothing.testmodule.command.functions.watch.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

@SerializeKeyName("WatchOutput")
public class WatchOutputRequest extends CommandRequest {

    @CmdParam(
        name = "target",
        description = "目标 (ID或all)",
        required = false,
        position = 1
    )
    private String target;

    @CmdParam(
        name = "limit",
        description = "输出行数",
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
