package com.justnothing.testmodule.command.functions.memory;

import static com.justnothing.testmodule.constants.CommandServer.CMD_MEMORY_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.memory.impl.InfoCommand;
import com.justnothing.testmodule.command.functions.memory.impl.GcCommand;
import com.justnothing.testmodule.command.functions.memory.impl.DumpCommand;
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
        handler = InfoCommand.class,
        description = "显示详细的内存使用情况"
    ),
    @CmdRoutes.Route(
        path = "gc",
        request = GcRequest.class,
        handler = GcCommand.class,
        description = "手动触发垃圾回收"
    ),
    @CmdRoutes.Route(
        path = "dump",
        request = DumpRequest.class,
        handler = DumpCommand.class,
        description = "导出堆信息和系统状态"
    )
})
public class MemoryMain extends MainCommand<CommandResult> {

    public MemoryMain() {
        super("Memory", CommandResult.class);
    }

    @Override
    public CommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        // 别名解析
        String subCommand;
        String[] subArgs;

        switch (context.cmdName) {
            case "minfo" -> { subCommand = "info"; subArgs = args; }
            case "mgc"  -> { subCommand = "gc"; subArgs = args; }
            case "mdump" -> { subCommand = "dump"; subArgs = args; }
            default -> {
                if (args.length < 1) {
                    context.println(getHelpText());
                    return createErrorResult("参数不足");
                }
                subCommand = args[0];
                subArgs = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[0];
            }
        }

        // 构造带子命令的参数数组，委托给 CommandRouter 分发到 Command
        String[] routeArgs = new String[subArgs.length + 1];
        routeArgs[0] = subCommand;
        System.arraycopy(subArgs, 0, routeArgs, 1, subArgs.length);

        // 创建新的上下文，复用原上下文的输出/类加载器等，替换 cmdName 和 args
        CommandExecutor.CmdExecContext<CommandRequest> routeContext = new CommandExecutor.CmdExecContext<>(
            "memory", routeArgs, context.origCommand,
            context.classLoader, context.output, context.argGroup, context.requirements
        );
        routeContext.setRequest(context.getRequest());
        routeContext.setExecutionType(context.getExecutionType());

        try {
            return CommandRouter.getInstance().dispatch(routeContext);
        } catch (IllegalArgumentException e) {
            context.print("未知子命令: ", Colors.RED);
            context.println(subCommand, Colors.YELLOW);
            return createErrorResult("未知子命令: " + subCommand);
        } catch (Throwable t) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "memory", t instanceof Exception ? t : new RuntimeException(t), context, "执行memory命令失败"
            );
            return createErrorResult("执行memory命令失败: " + t.getMessage());
        }
    }
}
