package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.network.NetworkTexts;
import com.justnothing.testmodule.command.functions.network.response.NetworkResult;

public class NetworkInterceptRequest extends CommandRequest<NetworkResult> {

    @CmdParam(name = "enable", required = false, description = NetworkTexts.PARAM_NETWORK_INTERCEPT_ENABLE_DESC)
    private Boolean enable;

    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }
}
