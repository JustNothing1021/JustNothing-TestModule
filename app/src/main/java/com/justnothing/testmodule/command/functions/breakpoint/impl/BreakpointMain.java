package com.justnothing.testmodule.command.functions.breakpoint.impl;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BREAKPOINT_VER;

import java.util.Arrays;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CmdParamProcessor;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointAddRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointListRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointEnableRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointDisableRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointRemoveRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointClearRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointHitsRequest;
import com.justnothing.testmodule.command.functions.breakpoint.response.BreakpointResult;
import com.justnothing.testmodule.command.output.Colors;

@Cmd(
    name = "breakpoint",
    description = "设置和管理断点",
    version = CMD_BREAKPOINT_VER,
    defaultResultType = BreakpointResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(path = "add", request = BreakpointAddRequest.class, handler = BreakpointManageCommand.class, description = "添加断点"),
    @CmdRoutes.Route(path = "list", request = BreakpointListRequest.class, handler = BreakpointQueryCommand.class, description = "列出所有断点"),
    @CmdRoutes.Route(path = "enable", request = BreakpointEnableRequest.class, handler = BreakpointManageCommand.class, description = "启用断点"),
    @CmdRoutes.Route(path = "disable", request = BreakpointDisableRequest.class, handler = BreakpointManageCommand.class, description = "禁用断点"),
    @CmdRoutes.Route(path = "remove", request = BreakpointRemoveRequest.class, handler = BreakpointManageCommand.class, description = "移除断点"),
    @CmdRoutes.Route(path = "clear", request = BreakpointClearRequest.class, handler = BreakpointManageCommand.class, description = "清除所有断点"),
    @CmdRoutes.Route(path = "hits", request = BreakpointHitsRequest.class, handler = BreakpointQueryCommand.class, description = "显示断点命中统计")
})
public class BreakpointMain extends MainCommand<BreakpointResult> {

    public BreakpointMain() {
        super("Breakpoint", BreakpointResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("breakpoint");
    }

    @Override
    public BreakpointResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        logger.debug("执行breakpoint命令，参数: " + Arrays.toString(args));

        if (args.length < 1) {
            context.println(getHelpText(), Colors.WHITE);
            return createErrorResult("参数不足，使用 breakpoint <subcmd> [args...]");
        }

        String subCommand = args[0].toLowerCase();

        try {
            CommandRouter.RouteMatch match = CommandRouter.getInstance()
                .matchRoute("breakpoint", new String[]{subCommand});

            if (match != null && match.routeConfig() != null) {
                Class<? extends CommandRequest> requestType = match.routeConfig().requestType();
                CommandRequest request = requestType.getDeclaredConstructor().newInstance();
                
                String[] remainingArgs = args.length > 1
                    ? Arrays.copyOfRange(args, 1, args.length)
                    : new String[0];
                CmdParamProcessor.parseCommandLineArgs(request, remainingArgs);

                context.setRequest(request);

                Class<?> handlerClass = match.routeConfig().handlerType();
                AbstractCommand<?, ?> handler = (AbstractCommand<?, ?>) handlerClass.getDeclaredConstructor().newInstance();
                return (BreakpointResult) handler.execute(context);
            }

            context.println("未知子命令: " + subCommand, Colors.RED);
            context.println(getHelpText(), Colors.WHITE);
            return createErrorResult("未知子命令: " + subCommand);

        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "breakpoint " + subCommand, e, context, "执行breakpoint的某个子命令时出错");
            return createErrorResult("执行breakpoint命令失败: " + e.getMessage());
        }
    }
}
