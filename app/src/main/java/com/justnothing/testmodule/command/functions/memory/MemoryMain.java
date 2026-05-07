package com.justnothing.testmodule.command.functions.memory;

import static com.justnothing.testmodule.constants.CommandServer.CMD_MEMORY_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.command.CommandInfo;
import com.justnothing.testmodule.command.base.command.RegisterCommand;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.command.SubCommands;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.memory.impl.DumpCommand;
import com.justnothing.testmodule.command.functions.memory.impl.GcCommand;
import com.justnothing.testmodule.command.functions.memory.impl.InfoCommand;
import com.justnothing.testmodule.command.handlers.memory.DumpRequestHandler;
import com.justnothing.testmodule.command.handlers.memory.GcRequestHandler;
import com.justnothing.testmodule.command.handlers.memory.MemoryInfoRequestHandler;

import java.util.Arrays;

@RegisterCommand("memory")
@CommandInfo(
    name = "memory",
    group = "system",
    description = "内存调试和管理工具, 包括内存信息查询、GC、堆转储等功能",
    resultType = CommandResult.class,
    helpText = """
            语法: memory <subcmd> [args...]
            
            内存调试和管理工具.
            
            子命令:
                info [options]                     - 显示详细的内存使用情况
                gc [options]                       - 手动触发垃圾回收 (建议开full)
                dump [options] [file]              - 导出堆信息和系统状态
            
            info 选项:
                -h, --heap       只显示堆内存信息
                -d, --detailed   显示详细内存信息 (默认)
            
            gc 选项:
                --full    - 执行完整的GC
                --stats   - 显示GC统计信息
            
            dump 选项:
                --heap            - 只导出堆信息
                --threads         - 只导出线程信息
                --full            - 导出完整信息 (默认)
            
            快捷命令:
                minfo    - 等同于 memory info
                mgc       - 等同于 memory gc
                mdump     - 等同于 memory dump
            
            示例:
                memory info
                memory info -h
                memory gc
                memory gc --full
                memory gc --stats
                memory dump
                memory dump /sdcard/heap_dump.txt
                memory dump --heap /sdcard/heap_only.txt
                memory dump --full /sdcard/full_dump.txt
            
            (Submodule memory %s)
            """,
    version = "1.0.0"
)
@SubCommands({
    @SubCommand(
        value = "info",
        request = MemoryInfoRequest.class,
        result = MemoryInfoResult.class,
        command = InfoCommand.class,
        description = "显示详细的内存使用情况"
    ),
    @SubCommand(
        value = "gc",
        request = GcRequest.class,
        result = GcResult.class,
        command = GcCommand.class,
        description = "手动触发垃圾回收"
    ),
    @SubCommand(
        value = "dump",
        request = DumpRequest.class,
        result = DumpResult.class,
        command = DumpCommand.class,
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
                    return null;
                }
                subCommand = args[0];
                subArgs = Arrays.copyOfRange(args, 1, args.length);
            }
        }

        try {
            CommandRequest request = context.parseRequest();

            if (request != null && shouldReturnStructuredData(context)) {
                return dispatchToHandler(subCommand, request);
            }

            AbstractCommand<?, ?> command = resolveSubCommandFromRequest(request);
            if (command != null) {
                return command.execute(context);
            }

            context.print("未知子命令: ", Colors.RED);
            context.println(subCommand, Colors.YELLOW);
            return null;
        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "memory", e, context, "执行memory命令失败"
            );

            if (shouldReturnStructuredData(context)) {
                return createErrorResult("执行memory命令失败: " + e.getMessage());
            }
            return null;
        }
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
