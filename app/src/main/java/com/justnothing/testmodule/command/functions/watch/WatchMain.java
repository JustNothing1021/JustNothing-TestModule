package com.justnothing.testmodule.command.functions.watch;

import static com.justnothing.testmodule.constants.CommandServer.CMD_WATCH_VER;

import java.util.Arrays;

import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CmdParamProcessor;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.watch.request.WatchAddRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchListRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchStopRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchClearRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchOutputRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchCommandResult;
import com.justnothing.testmodule.command.functions.watch.impl.AddCommand;
import com.justnothing.testmodule.command.functions.watch.impl.ListCommand;
import com.justnothing.testmodule.command.functions.watch.impl.StopCommand;
import com.justnothing.testmodule.command.functions.watch.impl.ClearCommand;
import com.justnothing.testmodule.command.functions.watch.impl.OutputCommand;

@Cmd(
    name = "watch",
    description = "监控字段或方法的变化, 非阻塞执行.",
    version = CMD_WATCH_VER,
    defaultResultType = WatchCommandResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "add",
        request = WatchAddRequest.class,
        handler = AddCommand.class,
        description = "添加字段或方法监控任务"
    ),
    @CmdRoutes.Route(
        path = "list",
        request = WatchListRequest.class,
        handler = ListCommand.class,
        description = "列出所有监控任务"
    ),
    @CmdRoutes.Route(
        path = "stop",
        request = WatchStopRequest.class,
        handler = StopCommand.class,
        description = "停止指定的监控任务"
    ),
    @CmdRoutes.Route(
        path = "clear",
        request = WatchClearRequest.class,
        handler = ClearCommand.class,
        description = "清除所有监控任务"
    ),
    @CmdRoutes.Route(
        path = "output",
        request = WatchOutputRequest.class,
        handler = OutputCommand.class,
        description = "获取监控任务的输出"
    )
})
public class WatchMain extends MainCommand<WatchCommandResult> {

    public WatchMain() {
        super("Watch", WatchCommandResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("watch");
    }

    @Override
    public WatchCommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();
        
        logger.debug("执行watch命令，参数: " + Arrays.toString(args));

        try {
            if (args.length < 1) {
                context.println(getHelpText(), Colors.WHITE);
                return createErrorResult("参数不足，使用 watch <subcmd> [args...]");
            }

            String subCommand = args[0];
            String[] remainingArgs = (args.length > 1) 
                ? Arrays.copyOfRange(args, 1, args.length) 
                : new String[0];

            AbstractWatchCommand<?, ?> command = resolveCommand(subCommand, remainingArgs);
            
            if (command == null) {
                context.print("未知子命令: ", Colors.RED);
                context.println(subCommand, Colors.YELLOW);
                context.println("\n可用的子命令:", Colors.WHITE);
                context.println(getHelpText(), Colors.WHITE);
                return createErrorResult("未知子命令: " + subCommand);
            }

            context.setRequest(parseRequestForCommand(subCommand, remainingArgs));
            return (WatchCommandResult) command.execute(context);

        } catch (IllegalCommandLineArgumentException e) {
            throw e;
        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "watch", e, context, "执行watch命令失败"
            );
            return createErrorResult("执行watch命令失败: " + e.getMessage());
        }
    }

    private AbstractWatchCommand<?, ?> resolveCommand(String subCommand, String[] remainingArgs) {
        return switch (subCommand.toLowerCase()) {
            case "add" -> new AddCommand();
            case "list" -> new ListCommand();
            case "stop" -> new StopCommand();
            case "clear" -> new ClearCommand();
            case "output" -> new OutputCommand();
            default -> null;
        };
    }

    private CommandRequest parseRequestForCommand(String subCommand, String[] args)
            throws Exception {
        CommandRouter.RouteMatch match = CommandRouter.getInstance()
            .matchRoute("watch", new String[]{subCommand});

        if (match != null && match.routeConfig() != null) {
            Class<? extends CommandRequest> requestType = match.routeConfig().requestType();
            CommandRequest request = requestType.getDeclaredConstructor().newInstance();
            return CmdParamProcessor.parseRequest(request, args);
        }

        return new WatchListRequest();
    }
}
