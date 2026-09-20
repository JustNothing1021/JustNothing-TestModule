package com.justnothing.testmodule.command.functions.network;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;

import com.justnothing.testmodule.command.functions.network.request.NetworkListRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkInfoRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkFilterRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkExportRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkInterceptRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkRecordRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkStatusRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkMockRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkHookRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkWatchRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkClearRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkShutdownRequest;
import com.justnothing.testmodule.command.functions.network.impl.NetworkManageCommand;
import com.justnothing.testmodule.command.functions.network.impl.NetworkQueryCommand;
import com.justnothing.testmodule.command.functions.network.response.NetworkResult;

@Cmd(
    name = "network",
    description = NetworkTexts.CMD_NETWORK_DESC
)
@CmdRoutes({
    @CmdRoutes.Route(path = "intercept", request = NetworkInterceptRequest.class, handler = NetworkManageCommand.class, description = NetworkTexts.ROUTE_NETWORK_INTERCEPT_DESC),
    @CmdRoutes.Route(path = "record", request = NetworkRecordRequest.class, handler = NetworkManageCommand.class, description = NetworkTexts.ROUTE_NETWORK_RECORD_DESC),
    @CmdRoutes.Route(path = "status", request = NetworkStatusRequest.class, handler = NetworkQueryCommand.class, description = NetworkTexts.ROUTE_NETWORK_STATUS_DESC),
    @CmdRoutes.Route(path = "list", request = NetworkListRequest.class, handler = NetworkQueryCommand.class, description = NetworkTexts.ROUTE_NETWORK_LIST_DESC),
    @CmdRoutes.Route(path = "info", request = NetworkInfoRequest.class, handler = NetworkQueryCommand.class, description = NetworkTexts.ROUTE_NETWORK_INFO_DESC),
    @CmdRoutes.Route(path = "filter", request = NetworkFilterRequest.class, handler = NetworkManageCommand.class, description = NetworkTexts.ROUTE_NETWORK_FILTER_DESC),
    @CmdRoutes.Route(path = "mock", request = NetworkMockRequest.class, handler = NetworkManageCommand.class, description = NetworkTexts.ROUTE_NETWORK_MOCK_DESC),
    @CmdRoutes.Route(path = "hook", request = NetworkHookRequest.class, handler = NetworkManageCommand.class, description = NetworkTexts.ROUTE_NETWORK_HOOK_DESC),
    @CmdRoutes.Route(path = "watch", request = NetworkWatchRequest.class, handler = NetworkQueryCommand.class, description = NetworkTexts.ROUTE_NETWORK_WATCH_DESC),
    @CmdRoutes.Route(path = "export", request = NetworkExportRequest.class, handler = NetworkQueryCommand.class, description = NetworkTexts.ROUTE_NETWORK_EXPORT_DESC),
    @CmdRoutes.Route(path = "clear", request = NetworkClearRequest.class, handler = NetworkManageCommand.class, description = NetworkTexts.ROUTE_NETWORK_CLEAR_DESC),
    @CmdRoutes.Route(path = "shutdown", request = NetworkShutdownRequest.class, handler = NetworkManageCommand.class, description = NetworkTexts.ROUTE_NETWORK_SHUTDOWN_DESC)
})
public class NetworkMain extends MainCommand<NetworkResult> {

    public NetworkMain() {
        super("network", NetworkResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("network");
    }
}
