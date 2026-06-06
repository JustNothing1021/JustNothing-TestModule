package com.justnothing.testmodule.command.functions.network;

import static com.justnothing.testmodule.constants.CommandServer.CMD_NETWORK_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.output.Colors;

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

@Cmd(
    name = "network",
    description = "网络请求监控和调试工具",
    defaultResultType = NetworkResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(path = "intercept", request = NetworkInterceptRequest.class, handler = NetworkManageCommand.class, description = "开启/关闭网络拦截"),
    @CmdRoutes.Route(path = "record", request = NetworkRecordRequest.class, handler = NetworkManageCommand.class, description = "开启/关闭请求记录"),
    @CmdRoutes.Route(path = "status", request = NetworkStatusRequest.class, handler = NetworkQueryCommand.class, description = "显示当前状态"),
    @CmdRoutes.Route(path = "list", request = NetworkListRequest.class, handler = NetworkQueryCommand.class, description = "列出请求记录"),
    @CmdRoutes.Route(path = "info", request = NetworkInfoRequest.class, handler = NetworkQueryCommand.class, description = "查看请求详情"),
    @CmdRoutes.Route(path = "filter", request = NetworkFilterRequest.class, handler = NetworkManageCommand.class, description = "过滤特定主机的请求"),
    @CmdRoutes.Route(path = "mock", request = NetworkMockRequest.class, handler = NetworkManageCommand.class, description = "Mock 规则管理"),
    @CmdRoutes.Route(path = "hook", request = NetworkHookRequest.class, handler = NetworkManageCommand.class, description = "Hook 管理"),
    @CmdRoutes.Route(path = "watch", request = NetworkWatchRequest.class, handler = NetworkQueryCommand.class, description = "实时监控"),
    @CmdRoutes.Route(path = "export", request = NetworkExportRequest.class, handler = NetworkQueryCommand.class, description = "导出请求记录"),
    @CmdRoutes.Route(path = "clear", request = NetworkClearRequest.class, handler = NetworkManageCommand.class, description = "清除请求记录"),
    @CmdRoutes.Route(path = "shutdown", request = NetworkShutdownRequest.class, handler = NetworkManageCommand.class, description = "关闭网络监控")
})
public class NetworkMain extends MainCommand<NetworkResult> {

    private static final com.justnothing.testmodule.utils.logging.Logger logger =
        com.justnothing.testmodule.utils.logging.Logger.getLoggerForName("NetworkMain");

    public NetworkMain() {
        super("network", NetworkResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("network");
    }

    @Override
    public NetworkResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        logger.debug("执行 network 命令（fallback路径），参数: %s", java.util.Arrays.toString(args));

        if (args.length == 0) {
            context.println(getHelpText(), Colors.WHITE);
            return null;
        }

        context.println("提示: 使用 'network help' 查看路由帮助", Colors.CYAN);
        return createErrorResult("旧路径已迁移到 @CmdRoutes，请使用新路径");
    }
}
