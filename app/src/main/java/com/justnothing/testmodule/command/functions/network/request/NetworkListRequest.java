package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.network.NetworkTexts;
import com.justnothing.testmodule.command.framework.model.CommandResult;

public class NetworkListRequest extends CommandRequest<CommandResult> {

    @CmdParam(
        name = "--method",
        required = false,
        description = NetworkTexts.PARAM_NETWORK_LIST_METHOD_DESC
    )
    private String method;

    @CmdParam(
        name = "--host",
        required = false,
        description = NetworkTexts.PARAM_NETWORK_LIST_HOST_DESC
    )
    private String host;

    @CmdParam(
        name = "--status",
        required = false,
        description = NetworkTexts.PARAM_NETWORK_LIST_STATUS_DESC
    )
    private String statusFilter;

    @CmdParam(
        name = "--limit",
        required = false,
        defaultValue = "20",
        description = NetworkTexts.PARAM_NETWORK_LIST_LIMIT_DESC
    )
    private int limit = 20;

    public NetworkListRequest() {
        super();
    }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
