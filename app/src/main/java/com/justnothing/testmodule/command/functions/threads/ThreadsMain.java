package com.justnothing.testmodule.command.functions.threads;

import static com.justnothing.testmodule.constants.CommandServer.CMD_THREADS_VER;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdParamProcessor;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.threads.impl.DeadlockCommand;
import com.justnothing.testmodule.command.functions.threads.impl.ListCommand;
import com.justnothing.testmodule.command.functions.threads.impl.ProfileExportCommand;
import com.justnothing.testmodule.command.functions.threads.impl.ProfileShowCommand;
import com.justnothing.testmodule.command.functions.threads.impl.ProfileStartCommand;
import com.justnothing.testmodule.command.functions.threads.impl.ProfileStopCommand;
import com.justnothing.testmodule.command.functions.threads.request.ThreadDeadlockRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadListRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileExportRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileShowRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileStartRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileStopRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadCommandResult;
import com.justnothing.testmodule.command.output.Colors;

@Cmd(
    name = "threads",
    group = "system",
    description = "线程管理和分析工具",
    version = CMD_THREADS_VER,
    defaultResultType = ThreadCommandResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "list",
        request = ThreadListRequest.class,
        handler = ListCommand.class,
        description = "列出所有线程及其状态"
    ),
    @CmdRoutes.Route(
        path = "deadlock",
        request = ThreadDeadlockRequest.class,
        handler = DeadlockCommand.class,
        description = "检测Java应用程序中的死锁"
    ),
    @CmdRoutes.Route(
        path = "profile/start",
        request = ThreadProfileStartRequest.class,
        handler = ProfileStartCommand.class,
        description = "开始性能分析"
    ),
    @CmdRoutes.Route(
        path = "profile/stop",
        request = ThreadProfileStopRequest.class,
        handler = ProfileStopCommand.class,
        description = "停止当前分析"
    ),
    @CmdRoutes.Route(
        path = "profile/show",
        request = ThreadProfileShowRequest.class,
        handler = ProfileShowCommand.class,
        description = "显示分析结果"
    ),
    @CmdRoutes.Route(
        path = "profile/export",
        request = ThreadProfileExportRequest.class,
        handler = ProfileExportCommand.class,
        description = "导出分析结果到文件"
    )
})
public class ThreadsMain extends MainCommand<ThreadCommandResult> {

    private final Map<String, AbstractThreadsCommand<?, ?>> subCommandMap = new ConcurrentHashMap<>();

    public ThreadsMain() {
        super("Threads", ThreadCommandResult.class);
        
        registerSubCommand("list", new ListCommand());
        registerSubCommand("deadlock", new DeadlockCommand());
        registerSubCommand("profile/start", new ProfileStartCommand());
        registerSubCommand("profile/stop", new ProfileStopCommand());
        registerSubCommand("profile/show", new ProfileShowCommand());
        registerSubCommand("profile/export", new ProfileExportCommand());
        
        CommandRouter.getInstance().registerCommand(ThreadsMain.class);
    }

    private void registerSubCommand(String name, AbstractThreadsCommand<?, ?> command) {
        subCommandMap.put(name.toLowerCase(), command);
        logger.debug("注册子命令: " + name + " -> " + command.getClass().getSimpleName());
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("threads");
    }

    @Override
    public ThreadCommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();
        
        logger.debug("执行threads命令，参数: " + Arrays.toString(args));

        try {
            if (args.length < 1) {
                context.println(getHelpText(), Colors.WHITE);
                return createErrorResult("参数不足，使用 threads <subcmd> [args...]");
            }

            String subCommand = args[0];
            String[] remainingArgs = (args.length > 1) 
                ? Arrays.copyOfRange(args, 1, args.length) 
                : new String[0];

            AbstractThreadsCommand<?, ?> command = resolveCommand(subCommand, remainingArgs);
            
            if (command == null) {
                context.print("未知子命令: ", Colors.RED);
                context.println(subCommand, Colors.YELLOW);
                context.println("\n可用的子命令:", Colors.WHITE);
                context.println(getHelpText(), Colors.WHITE);
                return createErrorResult("未知子命令: " + subCommand);
            }

            context.setRequest(parseRequestForCommand(subCommand, remainingArgs));
            return (ThreadCommandResult) command.execute(context);

        } catch (com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException e) {
            throw e;
        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "threads", e, context, "执行threads命令失败"
            );
            return createErrorResult("执行threads命令失败: " + e.getMessage());
        }
    }

    private AbstractThreadsCommand<?, ?> resolveCommand(String subCommand, String[] remainingArgs) {
        if ("profile".equals(subCommand) && remainingArgs.length >= 1) {
            String nestedPath = "profile/" + remainingArgs[0];
            AbstractThreadsCommand<?, ?> nestedCmd = subCommandMap.get(nestedPath.toLowerCase());
            if (nestedCmd != null) return nestedCmd;
        }
        
        return subCommandMap.get(subCommand.toLowerCase());
    }

    @SuppressWarnings("unchecked")
    private CommandRequest parseRequestForCommand(String subCommand, String[] args)
            throws Exception {
        String routePath = ("profile".equals(subCommand) && args.length > 0)
            ? "profile/" + args[0]
            : subCommand;

        CommandRouter.RouteMatch match = CommandRouter.getInstance()
            .matchRoute("threads", new String[]{routePath});

        if (match != null && match.routeConfig() != null) {
            Class<? extends CommandRequest> requestType = match.routeConfig().requestType();
            CommandRequest request = requestType.getDeclaredConstructor().newInstance();

            String[] parseArgs = ("profile".equals(subCommand) && args.length > 1)
                ? Arrays.copyOfRange(args, 1, args.length)
                : args;

            return CmdParamProcessor.parseRequest(request, parseArgs);
        }

        return new ThreadListRequest();
    }
}
