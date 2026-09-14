package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.network.response.NetworkResult;

public class NetworkFilterRequest extends CommandRequest<NetworkResult> {

    @CmdParam(
        name = "host",
        position = 1,
        required = true,
        description = "主机名",
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
