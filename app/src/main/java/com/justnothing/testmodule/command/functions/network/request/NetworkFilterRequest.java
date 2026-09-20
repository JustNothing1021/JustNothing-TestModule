package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.network.NetworkTexts;
import com.justnothing.testmodule.command.functions.network.response.NetworkResult;

public class NetworkFilterRequest extends CommandRequest<NetworkResult> {

    @CmdParam(
        name = "host",
        position = 1,
        required = true,
        description = NetworkTexts.PARAM_NETWORK_FILTER_HOST_DESC,
        serializedName = "host"
    )
    private String host;

    public NetworkFilterRequest() {
        super();
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getHostPattern() { return host; }
}
