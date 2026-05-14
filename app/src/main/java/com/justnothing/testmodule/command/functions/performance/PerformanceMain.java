package com.justnothing.testmodule.command.functions.performance;

import static com.justnothing.testmodule.constants.CommandServer.CMD_PERFORMANCE_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.command.*;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.performance.impl.*;
import com.justnothing.testmodule.command.functions.performance.parser.PerformanceCommandParser;
import com.justnothing.testmodule.command.functions.performance.request.*;
import com.justnothing.testmodule.command.functions.performance.response.*;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampler;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceData;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceRunner;
import com.justnothing.testmodule.command.functions.performance.trace.TraceData;
import com.justnothing.testmodule.command.functions.performance.trace.Tracer;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@CommandInfo(
    name = "performance",
    group = "performance",
    description = "性能分析命令，支持多种分析方式（采样、多线程、分层、Trace、Systrace、Hook）",
    helpText = """
            语法: performance <subcmd> [args...]
            
            性能分析命令，支持多种分析方式。
            (输入 performance <subcmd> 查看子命令帮助)
            
            子命令:
                sample start [rate] [-e <exclude>]       - 开始采样
                sample stop <id>                        - 停止采样
                sample report [id]                       - 查看采样报告
                sample export <id> <file>               - 导出采样数据

                multithread start [rate] [-e <exclude>]  - 开始多线程采样
                multithread stop <id>                   - 停止多线程采样
                multithread report [id]                 - 查看多线程采样报告
                multithread export <id> <file>          - 导出多线程采样数据

                hierarchical start [rate] [-e <exclude>] - 开始分层采样
                hierarchical stop <id>                  - 停止分层采样
                hierarchical report [id]                - 查看分层采样报告
                hierarchical export <id> <file>         - 导出分层采样数据

                trace start                             - 开始 Trace
                trace stop <id>                         - 停止 Trace
                trace report [id]                       - 查看 Trace 报告
                trace export <id> <file>                - 导出 Trace 数据

                systrace start [duration] [categories]  - 开始 Systrace
                systrace stop <id>                     - 停止 Systrace
                systrace report [id]                    - 查看 Systrace 报告
                systrace export <id> <file>             - 导出 Systrace 数据

                hook <class> [method] [sig]             - Hook 方法
                hook stop <id>                          - 停止 Hook
                hook report [id]                        - 查看 Hook 报告
                hook export <id> <file>                 - 导出 Hook 数据

                list                                    - 列出所有任务
                clear                                   - 清除所有任务
            
            选项:
                rate   - 采样频率（Hz），默认 100
            
            示例:
                performance sample start 100
                performance sample stop 1
                performance hook com.example.MyClass myMethod
            
            (Submodule performance %s)
            """,
    version = CMD_PERFORMANCE_VER,
    resultType = PerformanceResult.class
)
@SubCommands({
    @SubCommand(value = "sample", request = SampleRequest.class, result = SampleResult.class, command = SampleCommand.class, description = "单线程采样分析"),
    @SubCommand(value = "multithread", request = MultiThreadRequest.class, result = MultiThreadResult.class, command = MultiThreadCommand.class, description = "多线程采样分析"),
    @SubCommand(value = "hierarchical", request = HierarchicalRequest.class, result = HierarchicalResult.class, command = HierarchicalCommand.class, description = "分层调用采样"),
    @SubCommand(value = "trace", request = TraceRequest.class, result = TraceResult.class, command = TraceCommand.class, description = "方法执行追踪"),
    @SubCommand(value = "systrace", request = SystraceRequest.class, result = SystraceResult.class, command = SystraceCommand.class, description = "系统级 Systrace"),
    @SubCommand(value = "hook", request = PerfHookRequest.class, result = PerfHookResult.class, command = HookCommand.class, description = "方法级别 Hook")
})
@RegisterCommand("performance")
@SupportsRequests({
    SampleRequest.class,
    MultiThreadRequest.class,
    HierarchicalRequest.class,
    TraceRequest.class,
    SystraceRequest.class,
    PerfHookRequest.class
})
@RegisterParser(PerformanceCommandParser.class)
public class PerformanceMain extends MainCommand<PerformanceResult> {
    private static final Logger staticLogger = Logger.getLoggerForName("Performance");

    public PerformanceMain() {
        super("Performance", PerformanceResult.class);
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: performance <subcmd> [args...]

                性能分析命令，支持多种分析方式。

                子命令:
                    sample start [rate] [-e <exclude>]       - 开始采样
                    sample stop <id>                        - 停止采样
                    sample report [id]                       - 查看采样报告
                    sample export <id> <file>               - 导出采样数据

                    multithread start [rate] [-e <exclude>]  - 开始多线程采样
                    multithread stop <id>                   - 停止多线程采样
                    multithread report [id]                 - 查看多线程采样报告
                    multithread export <id> <file>          - 导出多线程采样数据

                    hierarchical start [rate] [-e <exclude>] - 开始分层采样
                    hierarchical stop <id>                  - 停止分层采样
                    hierarchical report [id]                - 查看分层采样报告
                    hierarchical export <id> <file>         - 导出分层采样数据

                    trace start                             - 开始 Trace
                    trace stop <id>                         - 停止 Trace
                    trace report [id]                       - 查看 Trace 报告
                    trace export <id> <file>                - 导出 Trace 数据

                    systrace start [duration] [categories]  - 开始 Systrace
                    systrace stop <id>                     - 停止 Systrace
                    systrace report [id]                    - 查看 Systrace 报告
                    systrace export <id> <file>             - 导出 Systrace 数据

                    hook <class> [method] [sig]             - Hook 方法
                    hook stop <id>                          - 停止 Hook
                    hook report [id]                        - 查看 Hook 报告
                    hook export <id> <file>                 - 导出 Hook 数据

                    list                                    - 列出所有任务
                    clear                                   - 清除所有任务

                选项:
                    rate   - 采样频率（Hz），默认 100

                示例:
                    performance sample start 100
                    performance sample stop 1
                    performance hook com.example.MyClass myMethod

                (Submodule performance %s)
                """, CMD_PERFORMANCE_VER);
    }

    @Override
    public PerformanceResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String cmdName = context.cmdName();
        String[] args = context.args();

        logger.debug("执行performance命令（使用新架构），参数: " + Arrays.toString(args));

        AbstractPerfCommand<? extends PerformanceRequest, ? extends CommandResult> command;

        if (args.length >= 1) {
            String subCmdName = cmdName != null ? cmdName : args[0];
            logger.debug("CLI模式: 从args获取子命令: " + subCmdName);
            command = findSubCommandByName(subCmdName);
            if (command == null) {
                handleUnknownCommand(subCmdName, context);
                return null;
            }

            try {
                CommandRequest parsedRequest = context.parseRequest();
                if (parsedRequest != null) {
                    logger.debug("CLI模式: parseRequest成功, 类型=" + parsedRequest.getClass().getSimpleName());
                } else {
                    logger.warn("CLI模式: parseRequest返回null, 使用默认Request");
                }
            } catch (Exception e) {
                logger.warn("CLI模式: parseRequest失败: " + e.getMessage());
            }
        } else {
            CommandRequest request = context.getRequest();
            if (request != null) {
                command = resolveSubCommandFromRequest(request);
                logger.debug("GUI模式: 从Request注解解析子命令: " + request.getClass().getSimpleName());
                if (command == null) {
                    context.println("错误: 无法找到对应的子命令处理", Colors.RED);
                    return createErrorResult("无法找到对应的子命令处理");
                }
            } else {
                context.println(getHelpText(), Colors.WHITE);
                return null;
            }
        }

        try {
            CommandResult result = command.execute(context);

            if (result != null && shouldReturnStructuredData(context)) {
                PerformanceResult perfResult = new PerformanceResult();
                String subCommand = extractSubCommandName(command);
                perfResult.setSubCommand(subCommand);
                perfResult.setSubResult(result.toJson().toString());
                return perfResult;
            }

            return null;
        } catch (Exception e) {
            String subCmdName = args.length >= 1 ? args[0] : "unknown";
            CommandExceptionHandler.handleException(
                    "performance " + subCmdName,
                    e,
                    context,
                    "执行失败"
            );
            if (shouldReturnStructuredData(context)) {
                return createErrorResult("执行performance命令失败: " + e.getMessage());
            }
            return null;
        }
    }

    private AbstractPerfCommand<? extends PerformanceRequest, ? extends CommandResult>
                findSubCommandByName(String name) {
        SubCommands subCommandsAnnotation = this.getClass().getAnnotation(SubCommands.class);
        if (subCommandsAnnotation != null) {
            for (SubCommand subCmd : subCommandsAnnotation.value()) {
                if (subCmd.value().equals(name)) {
                    try {
                        return (AbstractPerfCommand<? extends PerformanceRequest, ? extends CommandResult>)
                                subCmd.command().getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        logger.error("创建子命令实例失败: " + name, e);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected AbstractPerfCommand<? extends PerformanceRequest, ? extends CommandResult>
                resolveSubCommandFromRequest(CommandRequest request) {
        SubCommands subCommandsAnnotation = this.getClass().getAnnotation(SubCommands.class);
        if (subCommandsAnnotation != null) {
            for (SubCommand subCmd : subCommandsAnnotation.value()) {
                if (subCmd.request().isInstance(request)) {
                    try {
                        return (AbstractPerfCommand<? extends PerformanceRequest, ? extends CommandResult>)
                                subCmd.command().getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        logger.error("从Request创建子命令实例失败: " + request.getClass().getSimpleName(), e);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private String extractSubCommandName(AbstractPerfCommand<?, ?> command) {
        SubCommands subCommandsAnnotation = this.getClass().getAnnotation(SubCommands.class);
        if (subCommandsAnnotation != null) {
            for (SubCommand subCmd : subCommandsAnnotation.value()) {
                if (subCmd.command().equals(command.getClass())) {
                    return subCmd.value();
                }
            }
        }
        return "unknown";
    }

    private void handleUnknownCommand(String subCmdName, CommandExecutor.CmdExecContext<?> context) {
        switch (subCmdName) {
            case "list" -> handleList(context);
            case "clear" -> handleClear(context);
            default -> {
                context.println("错误: 未知子命令 " + subCmdName, Colors.RED);
                context.println(getHelpText(), Colors.GRAY);
            }
        }
    }

    private void handleList(CommandExecutor.CmdExecContext<?> ctx) {
        PerfTaskManager mgr = PerfTaskManager.getInstance();
        ctx.println("=== 性能分析任务列表 ===", Colors.CYAN);
        ctx.println("", Colors.WHITE);

        printSamplers(ctx, mgr.getSimpleSamplers(), mgr.getSimpleSampleDataMap(), "单线程采样");
        printSamplers(ctx, mgr.getMultiThreadSamplers(), mgr.getMultiThreadSampleDataMap(), "多线程采样");
        printHierarchicalSamplers(ctx, mgr.getHierarchicalSamplers(), mgr.getHierarchicalSampleDataMap());
        printTracers(ctx, mgr.getTracers(), mgr.getTraceDataMap());
        printSystrace(ctx, mgr.getSystraceRunners(), mgr.getSystraceDataMap());
        printHooks(ctx);

        if (mgr.getTotalRunningCount() == 0 && mgr.getTotalCompletedCount() == 0) {
            ctx.println("没有运行中的性能分析任务", Colors.GRAY);
        }
    }

    private void printSamplers(CommandExecutor.CmdExecContext<?> ctx,
                               Map<Integer, ?> running, Map<?, ?> completed, String label) {
        if (!running.isEmpty()) {
            ctx.println("运行中的" + label + "器:", Colors.CYAN);
            for (Map.Entry<Integer, ?> e : running.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.println("    类型: " + label, Colors.WHITE);
                ctx.println("    状态: 运行中", Colors.GREEN);
            }
            ctx.println("", Colors.WHITE);
        }
        if (!completed.isEmpty()) {
            ctx.println("已完成的" + label + "数据:", Colors.CYAN);
            for (Map.Entry<?, ?> e : completed.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.println("    状态: 已完成", Colors.GRAY);
            }
            ctx.println("", Colors.WHITE);
        }
    }

    private void printHierarchicalSamplers(CommandExecutor.CmdExecContext<?> ctx,
                                           Map<Integer, HierarchicalSampler> running,
                                           Map<Integer, HierarchicalSampleData> completed) {
        if (!running.isEmpty()) {
            ctx.println("运行中的分层采样器:", Colors.CYAN);
            for (var e : running.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print("    方法数: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getValue().getMethodCount()), Colors.YELLOW);
            }
            ctx.println("", Colors.WHITE);
        }
        if (!completed.isEmpty()) {
            ctx.println("已完成的分层数据:", Colors.CYAN);
            for (var e : completed.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN); ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
            }
            ctx.println("", Colors.WHITE);
        }
    }

    private void printTracers(CommandExecutor.CmdExecContext<?> ctx,
                              Map<Integer, Tracer> running, Map<Integer, List<TraceData>> completed) {
        if (!running.isEmpty()) {
            ctx.println("运行中的 Tracer:", Colors.CYAN);
            for (var e : running.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print("    Trace数: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getValue().getSectionCount()), Colors.YELLOW);
            }
            ctx.println("", Colors.WHITE);
        }
        if (!completed.isEmpty()) {
            ctx.println("已完成的 Trace:", Colors.CYAN);
            for (var e : completed.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print("    数量: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getValue().size()), Colors.YELLOW);
            }
            ctx.println("", Colors.WHITE);
        }
    }

    private void printSystrace(CommandExecutor.CmdExecContext<?> ctx,
                               Map<Integer, SystraceRunner> running, Map<Integer, SystraceData> completed) {
        if (!running.isEmpty()) {
            ctx.println("运行中的 Systrace:", Colors.CYAN);
            for (var e : running.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN); ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print("    持续: ", Colors.CYAN); ctx.println(e.getValue().getDuration() / 1000.0 + " 秒", Colors.YELLOW);
            }
            ctx.println("", Colors.WHITE);
        }
        if (!completed.isEmpty()) {
            ctx.println("已完成的 Systrace:", Colors.CYAN);
            for (var e : completed.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN); ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print("    文件: ", Colors.CYAN); ctx.println(e.getValue().file(), Colors.GREEN);
            }
            ctx.println("", Colors.WHITE);
        }
    }

    private void printHooks(CommandExecutor.CmdExecContext<?> ctx) {
        var hooks = PerformanceManager.getInstance().listPerformanceHooks();
        if (!hooks.isEmpty()) {
            ctx.println("运行中的 Hook:", Colors.CYAN);
            for (var task : hooks) {
                var s = task.getStats();
                ctx.print("  ID: ", Colors.CYAN); ctx.println(String.valueOf(s.id()), Colors.YELLOW);
                ctx.print("    状态: ", Colors.CYAN);
                ctx.println(task.isRunning() ? "运行中" : "已停止", task.isRunning() ? Colors.GREEN : Colors.GRAY);
                ctx.print("    类: ", Colors.CYAN); ctx.println(s.className(), Colors.GREEN);
                ctx.print("    方法: ", Colors.CYAN); ctx.println(s.methodName(), Colors.GREEN);
                ctx.print("    次数: ", Colors.CYAN); ctx.println(String.valueOf(s.callCount()), Colors.YELLOW);
                ctx.println("", Colors.WHITE);
            }
        }
    }

    private void handleClear(CommandExecutor.CmdExecContext<?> ctx) {
        PerfTaskManager mgr = PerfTaskManager.getInstance();
        int total = mgr.getTotalRunningCount() + mgr.getTotalCompletedCount();
        mgr.clearAll();

        staticLogger.info("已清除所有性能分析任务 (" + total + " 个)");
        ctx.println("已清除所有性能分析任务", Colors.GREEN);
        ctx.print("共清除: ", Colors.CYAN); ctx.println(total + " 个", Colors.YELLOW);
    }
}
