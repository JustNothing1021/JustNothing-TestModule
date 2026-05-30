package com.justnothing.testmodule.command.functions.memory;

import static com.justnothing.testmodule.constants.CommandServer.CMD_MEMORY_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdParamProcessor;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.handlers.memory.DumpRequestHandler;
import com.justnothing.testmodule.command.handlers.memory.GcRequestHandler;
import com.justnothing.testmodule.command.handlers.memory.MemoryInfoRequestHandler;
import com.justnothing.testmodule.command.output.Colors;

@Cmd(
    name = "memory",
    group = "system",
    description = "内存调试和管理工具, 包括内存信息查询、GC、堆转储等功能",
    version = CMD_MEMORY_VER,
    defaultResultType = CommandResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "info",
        request = MemoryInfoRequest.class,
        handler = MemoryInfoRequestHandler.class,
        description = "显示详细的内存使用情况"
    ),
    @CmdRoutes.Route(
        path = "gc",
        request = GcRequest.class,
        handler = GcRequestHandler.class,
        description = "手动触发垃圾回收"
    ),
    @CmdRoutes.Route(
        path = "dump",
        request = DumpRequest.class,
        handler = DumpRequestHandler.class,
        description = "导出堆信息和系统状态"
    )
})
public class MemoryMain extends MainCommand<CommandResult> {

    public MemoryMain() {
        super("Memory", CommandResult.class);
    }

    @Override
    public CommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String cmdName = context.cmdName();
        String[] args = context.args();

        String subCommand;
        String[] subArgs;

        switch (cmdName) {
            case "minfo" -> {
                subCommand = "info";
                subArgs = args;
            }
            case "mgc" -> {
                subCommand = "gc";
                subArgs = args;
            }
            case "mdump" -> {
                subCommand = "dump";
                subArgs = args;
            }
            default -> {
                if (args.length < 1) {
                    context.println(getHelpText());
                    return createErrorResult("参数不足");
                }
                subCommand = args[0];
                subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
            }
        }

        try {
            CommandRouter router = CommandRouter.getInstance();
            CommandRouter.RouteMatch match = router.matchRoute("memory", 
                (subArgs.length > 0) ? new String[]{subCommand} : new String[0]);

            if (match != null) {
                Class<? extends CommandRequest> requestType = match.routeConfig().requestType();
                CommandRequest request = requestType.getDeclaredConstructor().newInstance();

                if (match.remainingArgs().length > 0 || hasRequiredParams(requestType)) {
                    CmdParamProcessor.parseCommandLineArgs(request, subArgs.length > 1 
                        ? java.util.Arrays.copyOfRange(subArgs, 1, subArgs.length) 
                        : new String[0]);
                }

                context.setRequest(request);
                return dispatchToHandler(subCommand, request);
            }

            context.print("未知子命令: ", Colors.RED);
            context.println(subCommand, Colors.YELLOW);
            return createErrorResult("未知子命令: " + subCommand);

        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "memory", e, context, "执行memory命令失败"
            );

            return createErrorResult("执行memory命令失败: " + e.getMessage());
        }
    }

    private boolean hasRequiredParams(Class<? extends CommandRequest> requestType) {
        var fields = CmdParamProcessor.getCmdParamFields(requestType);
        return fields.stream().anyMatch(fi -> fi.param().required());
    }

    private CommandResult dispatchToHandler(String subCommand, CommandRequest request) throws Exception {
        return switch (subCommand) {
            case "info" -> new MemoryInfoRequestHandler().handle((MemoryInfoRequest) request);
            case "gc" -> new GcRequestHandler().handle((GcRequest) request);
            case "dump" -> new DumpRequestHandler().handle((DumpRequest) request);
            default -> throw new IllegalArgumentException("未知子命令: " + subCommand);
        };
    }
}
