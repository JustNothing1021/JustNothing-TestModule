package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.network.NetworkResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class NetworkInterceptRequest extends CommandRequest<NetworkResult> {

    @CmdParam(name = "enable", required = false, description = "开启或关闭拦截")
    private Boolean enable;

    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }
}
