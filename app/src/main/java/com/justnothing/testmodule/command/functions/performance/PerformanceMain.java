package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.performance.impl.*;
import com.justnothing.testmodule.command.functions.performance.request.*;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampler;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampleData;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceData;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceRunner;
import com.justnothing.testmodule.command.functions.performance.trace.TraceData;
import com.justnothing.testmodule.command.functions.performance.trace.Tracer;
import com.justnothing.testmodule.constants.CommandServer;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Cmd(
    version = CommandServer.CMD_PERFORMANCE_VER,
    name = "performance",
    description = "性能分析命令, 支持多种分析方式（采样, 多线程, 分层, Trace, Systrace, Hook)",
    defaultResultType = PerformanceResult.class
)
@CmdRoutes({
    // Sample (4 routes) → SampleCommand
    @CmdRoutes.Route(path = "sample/start", request = SampleStartRequest.class, handler = SampleCommand.class, description = "开始单线程采样"),
    @CmdRoutes.Route(path = "sample/stop", request = SampleStopRequest.class, handler = SampleCommand.class, description = "停止单线程采样"),
    @CmdRoutes.Route(path = "sample/report", request = SampleReportRequest.class, handler = SampleCommand.class, description = "查看采样报告"),
    @CmdRoutes.Route(path = "sample/export", request = SampleExportRequest.class, handler = SampleCommand.class, description = "导出采样数据"),

    // MultiThread (4 routes) → MultiThreadCommand
    @CmdRoutes.Route(path = "multithread/start", request = MultiThreadStartRequest.class, handler = MultiThreadCommand.class, description = "开始多线程采样"),
    @CmdRoutes.Route(path = "multithread/stop", request = MultiThreadStopRequest.class, handler = MultiThreadCommand.class, description = "停止多线程采样"),
    @CmdRoutes.Route(path = "multithread/report", request = MultiThreadReportRequest.class, handler = MultiThreadCommand.class, description = "查看多线程采样报告"),
    @CmdRoutes.Route(path = "multithread/export", request = MultiThreadExportRequest.class, handler = MultiThreadCommand.class, description = "导出多线程采样数据"),

    // Hierarchical (4 routes) → HierarchicalCommand
    @CmdRoutes.Route(path = "hierarchical/start", request = HierarchicalStartRequest.class, handler = HierarchicalCommand.class, description = "开始分层采样"),
    @CmdRoutes.Route(path = "hierarchical/stop", request = HierarchicalStopRequest.class, handler = HierarchicalCommand.class, description = "停止分层采样"),
    @CmdRoutes.Route(path = "hierarchical/report", request = HierarchicalReportRequest.class, handler = HierarchicalCommand.class, description = "查看分层采样报告"),
    @CmdRoutes.Route(path = "hierarchical/export", request = HierarchicalExportRequest.class, handler = HierarchicalCommand.class, description = "导出分层数据"),

    // Trace (4 routes) → TraceCommand
    @CmdRoutes.Route(path = "trace/start", request = TraceStartRequest.class, handler = TraceCommand.class, description = "开始 Trace"),
    @CmdRoutes.Route(path = "trace/stop", request = TraceStopRequest.class, handler = TraceCommand.class, description = "停止 Trace"),
    @CmdRoutes.Route(path = "trace/report", request = TraceReportRequest.class, handler = TraceCommand.class, description = "查看 Trace 报告"),
    @CmdRoutes.Route(path = "trace/export", request = TraceExportRequest.class, handler = TraceCommand.class, description = "导出 Trace 数据"),

    // Systrace (4 routes) → SystraceCommand
    @CmdRoutes.Route(path = "systrace/start", request = SystraceStartRequest.class, handler = SystraceCommand.class, description = "开始 Systrace"),
    @CmdRoutes.Route(path = "systrace/stop", request = SystraceStopRequest.class, handler = SystraceCommand.class, description = "停止 Systrace"),
    @CmdRoutes.Route(path = "systrace/report", request = SystraceReportRequest.class, handler = SystraceCommand.class, description = "查看 Systrace 报告"),
    @CmdRoutes.Route(path = "systrace/export", request = SystraceExportRequest.class, handler = SystraceCommand.class, description = "导出 Systrace 数据"),

    // Hook (4 routes) → HookCommand
    @CmdRoutes.Route(path = "hook/start", request = PerfHookStartRequest.class, handler = HookCommand.class, description = "添加性能 Hook"),
    @CmdRoutes.Route(path = "hook/stop", request = PerfHookStopRequest.class, handler = HookCommand.class, description = "停止 Hook"),
    @CmdRoutes.Route(path = "hook/report", request = PerfHookReportRequest.class, handler = HookCommand.class, description = "查看 Hook 报告"),
    @CmdRoutes.Route(path = "hook/export", request = PerfHookExportRequest.class, handler = HookCommand.class, description = "导出 Hook 数据"),

    // Utility (2 routes) → handled by PerformanceMain itself
    @CmdRoutes.Route(path = "list", request = PerfListRequest.class, handler = PerformanceMain.class, description = "列出所有任务"),
    @CmdRoutes.Route(path = "clear", request = PerfClearRequest.class, handler = PerformanceMain.class, description = "清除所有任务")
})
public class PerformanceMain extends MainCommand<PerformanceResult> {

    private static final Logger logger = Logger.getLoggerForName("Performance");

    public PerformanceMain() {
        super("performance", PerformanceResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("performance");
    }

    @Override
    public PerformanceResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        if (args.length < 1) {
            context.println(getHelpText(), Colors.WHITE);
            return null;
        }

        return switch (args[0]) {
            case "list" -> { handleList(context); yield null; }
            case "clear" -> { handleClear(context); yield null; }
            default -> {
                context.println(getHelpText(), Colors.GRAY);
                yield createErrorResult("未知子命令: " + args[0]);
            }
        };
    }

    private void handleList(CommandExecutor.CmdExecContext<?> ctx) {
        logger.debug("列出所有性能分析任务");

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

    private void handleClear(CommandExecutor.CmdExecContext<?> ctx) {
        PerfTaskManager mgr = PerfTaskManager.getInstance();
        int total = mgr.getTotalRunningCount() + mgr.getTotalCompletedCount();
        mgr.clearAll();

        logger.info("已清除所有性能分析任务 (%d 个)", total);
        ctx.println("已清除所有性能分析任务", Colors.GREEN);
        ctx.print("共清除: ", Colors.CYAN);
        ctx.println(total + " 个", Colors.YELLOW);
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
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
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
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print("    持续: ", Colors.CYAN);
                ctx.println(e.getValue().getDuration() / 1000.0 + " 秒", Colors.YELLOW);
            }
            ctx.println("", Colors.WHITE);
        }
        if (!completed.isEmpty()) {
            ctx.println("已完成的 Systrace:", Colors.CYAN);
            for (var e : completed.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print("    文件: ", Colors.CYAN);
                ctx.println(e.getValue().file(), Colors.GREEN);
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
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(s.id()), Colors.YELLOW);
                ctx.print("    状态: ", Colors.CYAN);
                ctx.println(task.isRunning() ? "运行中" : "已停止", task.isRunning() ? Colors.GREEN : Colors.GRAY);
                ctx.print("    类: ", Colors.CYAN);
                ctx.println(s.className(), Colors.GREEN);
                ctx.print("    方法: ", Colors.CYAN);
                ctx.println(s.methodName(), Colors.GREEN);
                ctx.print("    次数: ", Colors.CYAN);
                ctx.println(String.valueOf(s.callCount()), Colors.YELLOW);
                ctx.println("", Colors.WHITE);
            }
        }
    }
}
