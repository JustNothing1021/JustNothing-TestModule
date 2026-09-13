package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;

public class NetworkInfoRequest extends CommandRequest<CommandResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = true,
        description = "请求 ID",
        serializedName = "targetRequestId"
    )
    private int targetRequestId;

    public NetworkInfoRequest() {
        super();
    }

    public int getTargetRequestId() { return targetRequestId; }
    public void setTargetRequestId(int id) { this.targetRequestId = id; }
}
