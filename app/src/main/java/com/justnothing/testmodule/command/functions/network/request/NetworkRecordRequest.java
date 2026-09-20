package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.network.response.NetworkResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class NetworkRecordRequest extends CommandRequest<NetworkResult> {

    @CmdParam(name = "enable", required = false, description = "开启或关闭记录")
    private Boolean enable;

    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }
}
