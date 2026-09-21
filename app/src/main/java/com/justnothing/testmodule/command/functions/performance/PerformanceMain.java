package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.performance.response.PerformanceResult;
import com.justnothing.testmodule.command.functions.performance.sampler.SampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.Sampler;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.performance.impl.*;
import com.justnothing.testmodule.command.functions.performance.request.*;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampler;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampleData;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceData;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceRunner;
import com.justnothing.testmodule.command.functions.performance.trace.TraceData;
import com.justnothing.testmodule.command.functions.performance.trace.Tracer;
import com.justnothing.testmodule.command.functions.performance.util.PerfTaskManager;
import com.justnothing.testmodule.command.functions.performance.util.PerformanceManager;
import com.justnothing.testmodule.constants.CommandServer;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.List;
import java.util.Map;

@Cmd(
    version = CommandServer.CMD_PERFORMANCE_VER,
    name = "performance",
    description = PerformanceTexts.CMD_PERFORMANCE_DESC
)
@CmdRoutes({
    // Sample (4 routes) → SampleCommand
    @CmdRoutes.Route(path = "sample/start", request = SampleStartRequest.class, handler = SampleCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_SAMPLE_START_DESC),
    @CmdRoutes.Route(path = "sample/stop", request = SampleStopRequest.class, handler = SampleCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_SAMPLE_STOP_DESC),
    @CmdRoutes.Route(path = "sample/report", request = SampleReportRequest.class, handler = SampleCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_SAMPLE_REPORT_DESC),
    @CmdRoutes.Route(path = "sample/export", request = SampleExportRequest.class, handler = SampleCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_SAMPLE_EXPORT_DESC),

    // MultiThread (4 routes) → MultiThreadCommand
    @CmdRoutes.Route(path = "multithread/start", request = MultiThreadStartRequest.class, handler = MultiThreadCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_MULTITHREAD_START_DESC),
    @CmdRoutes.Route(path = "multithread/stop", request = MultiThreadStopRequest.class, handler = MultiThreadCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_MULTITHREAD_STOP_DESC),
    @CmdRoutes.Route(path = "multithread/report", request = MultiThreadReportRequest.class, handler = MultiThreadCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_MULTITHREAD_REPORT_DESC),
    @CmdRoutes.Route(path = "multithread/export", request = MultiThreadExportRequest.class, handler = MultiThreadCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_MULTITHREAD_EXPORT_DESC),

    // Hierarchical (4 routes) → HierarchicalCommand
    @CmdRoutes.Route(path = "hierarchical/start", request = HierarchicalStartRequest.class, handler = HierarchicalCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_HIERARCHICAL_START_DESC),
    @CmdRoutes.Route(path = "hierarchical/stop", request = HierarchicalStopRequest.class, handler = HierarchicalCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_HIERARCHICAL_STOP_DESC),
    @CmdRoutes.Route(path = "hierarchical/report", request = HierarchicalReportRequest.class, handler = HierarchicalCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_HIERARCHICAL_REPORT_DESC),
    @CmdRoutes.Route(path = "hierarchical/export", request = HierarchicalExportRequest.class, handler = HierarchicalCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_HIERARCHICAL_EXPORT_DESC),


    // Trace (4 routes) → TraceCommand
    @CmdRoutes.Route(path = "trace/start", request = TraceStartRequest.class, handler = TraceCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_TRACE_START_DESC),
    @CmdRoutes.Route(path = "trace/stop", request = TraceStopRequest.class, handler = TraceCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_TRACE_STOP_DESC),
    @CmdRoutes.Route(path = "trace/report", request = TraceReportRequest.class, handler = TraceCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_TRACE_REPORT_DESC),
    @CmdRoutes.Route(path = "trace/export", request = TraceExportRequest.class, handler = TraceCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_TRACE_EXPORT_DESC),

    // Systrace (4 routes) → SystraceCommand
    @CmdRoutes.Route(path = "systrace/start", request = SystraceStartRequest.class, handler = SystraceCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_SYSTRACE_START_DESC),
    @CmdRoutes.Route(path = "systrace/stop", request = SystraceStopRequest.class, handler = SystraceCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_SYSTRACE_STOP_DESC),
    @CmdRoutes.Route(path = "systrace/report", request = SystraceReportRequest.class, handler = SystraceCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_SYSTRACE_REPORT_DESC),
    @CmdRoutes.Route(path = "systrace/export", request = SystraceExportRequest.class, handler = SystraceCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_SYSTRACE_EXPORT_DESC),

    // Hook (4 routes) → HookCommand
    @CmdRoutes.Route(path = "hook/start", request = PerfHookStartRequest.class, handler = HookCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_HOOK_START_DESC),
    @CmdRoutes.Route(path = "hook/stop", request = PerfHookStopRequest.class, handler = HookCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_HOOK_STOP_DESC),
    @CmdRoutes.Route(path = "hook/report", request = PerfHookReportRequest.class, handler = HookCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_HOOK_REPORT_DESC),
    @CmdRoutes.Route(path = "hook/export", request = PerfHookExportRequest.class, handler = HookCommand.class, description = PerformanceTexts.ROUTE_PERFORMANCE_HOOK_EXPORT_DESC),

    // Utility (2 routes) → handled by PerformanceMain itself
    @CmdRoutes.Route(path = "list", request = PerfListRequest.class, handler = PerformanceMain.class, description = PerformanceTexts.ROUTE_PERFORMANCE_LIST_DESC),
    @CmdRoutes.Route(path = "clear", request = PerfClearRequest.class, handler = PerformanceMain.class, description = PerformanceTexts.ROUTE_PERFORMANCE_CLEAR_DESC)
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
    protected PerformanceResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
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
                yield createErrorResult(Text.zhEn("未知子命令: %s", "Unknown subcommand: %s").format(args[0]));
            }
        };
    }

    private void handleList(CommandExecutor.CmdExecContext<?> ctx) {
        logger.debug("列出所有性能分析任务");

        PerfTaskManager mgr = PerfTaskManager.getInstance();
        ctx.println(Text.zhEn("=== 性能分析任务列表 ===", "=== Performance tasks ===").text(), Colors.CYAN);
        ctx.println("", Colors.WHITE);

        printSamplers(ctx, mgr.getSimpleSamplers(), mgr.getSimpleSampleDataMap(),
                Text.zhEn("单线程采样", "single-threaded sampling").text());
        printSamplers(ctx, mgr.getMultiThreadSamplers(), mgr.getMultiThreadSampleDataMap(),
                Text.zhEn("多线程采样", "multi-threaded sampling").text());
        printHierarchicalSamplers(ctx, mgr.getHierarchicalSamplers(), mgr.getHierarchicalSampleDataMap());
        printTracers(ctx, mgr.getTracers(), mgr.getTraceDataMap());
        printSystrace(ctx, mgr.getSystraceRunners(), mgr.getSystraceDataMap());
        printHooks(ctx);

        if (mgr.getTotalRunningCount() == 0 && mgr.getTotalCompletedCount() == 0) {
            ctx.println(Text.zhEn("没有运行中的性能分析任务", "No performance tasks are running").text(), Colors.GRAY);
        }
    }

    private void handleClear(CommandExecutor.CmdExecContext<?> ctx) {
        PerfTaskManager mgr = PerfTaskManager.getInstance();
        int total = mgr.getTotalRunningCount() + mgr.getTotalCompletedCount();
        mgr.clearAll();

        logger.info("已清除所有性能分析任务 (%d 个)", total);
        ctx.println(Text.zhEn("已清除所有性能分析任务", "All performance tasks cleared").text(), Colors.GREEN);
        ctx.print(Text.zhEn("共清除: ", "Cleared: ").text(), Colors.CYAN);
        ctx.println(Text.zhEn("%d 个", "%d task(s)").format(total), Colors.YELLOW);
    }

    private void printSamplers(CommandExecutor.CmdExecContext<?> ctx,
                               Map<Integer, ? extends Sampler<?>> running, Map<Integer, ? extends SampleData> completed, String label) {
        if (!running.isEmpty()) {
            ctx.println(Text.zhEn("记录中的%s器:", "Samplers in progress: %s").format(label), Colors.CYAN);
            for (Map.Entry<Integer, ? extends Sampler<?>> e : running.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.println(Text.zhEn("    类型: %s", "    Type: %s").format(label), Colors.WHITE);
                ctx.print(PerformanceTexts.LABEL_STATUS_INDENTED.text(), Colors.YELLOW);
                ctx.println(e.getValue().isRunning() ? PerformanceTexts.STATUS_RUNNING.text() : PerformanceTexts.STATUS_STOPPED.text(),
                            e.getValue().isRunning() ? Colors.GREEN : Colors.GRAY);
                ctx.print(PerformanceTexts.LABEL_SAMPLE_COUNT_INDENTED.text(), Colors.YELLOW);
                ctx.println(String.valueOf(e.getValue().getTotalSamples()), Colors.CYAN);
            }
            ctx.println("", Colors.WHITE);
        }
        if (!completed.isEmpty()) {
            ctx.println(Text.zhEn("已收集完成的%s数据:", "Completed %s data:").format(label), Colors.CYAN);
            for (Map.Entry<Integer, ? extends SampleData> e : completed.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.println(PerformanceTexts.LABEL_SAMPLE_COUNT_INDENTED.text() + e.getValue().totalSamples(), Colors.GREEN);

            }
            ctx.println("", Colors.WHITE);
        }
    }

    private void printHierarchicalSamplers(CommandExecutor.CmdExecContext<?> ctx,
                                           Map<Integer, HierarchicalSampler> running,
                                           Map<Integer, HierarchicalSampleData> completed) {
        if (!running.isEmpty()) {
            ctx.println(Text.zhEn("运行中的分层采样器:", "Hierarchical samplers in progress:").text(), Colors.CYAN);
            for (var e : running.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print(Text.zhEn("    方法数: ", "    Methods: ").text(), Colors.CYAN);
                ctx.println(String.valueOf(e.getValue().getMethodCount()), Colors.YELLOW);
            }
            ctx.println("", Colors.WHITE);
        }
        if (!completed.isEmpty()) {
            ctx.println(Text.zhEn("已完成的分层数据:", "Completed hierarchical data:").text(), Colors.CYAN);
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
            ctx.println(Text.zhEn("运行中的 Tracer:", "Tracers in progress:").text(), Colors.CYAN);
            for (var e : running.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print(Text.zhEn("    Trace数: ", "    Traces: ").text(), Colors.CYAN);
                ctx.println(String.valueOf(e.getValue().getSectionCount()), Colors.YELLOW);
            }
            ctx.println("", Colors.WHITE);
        }
        if (!completed.isEmpty()) {
            ctx.println(Text.zhEn("已完成的 Trace:", "Completed traces:").text(), Colors.CYAN);
            for (var e : completed.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print(Text.zhEn("    数量: ", "    Count: ").text(), Colors.CYAN);
                ctx.println(String.valueOf(e.getValue().size()), Colors.YELLOW);
            }
            ctx.println("", Colors.WHITE);
        }
    }

    private void printSystrace(CommandExecutor.CmdExecContext<?> ctx,
                               Map<Integer, SystraceRunner> running, Map<Integer, SystraceData> completed) {
        if (!running.isEmpty()) {
            ctx.println(Text.zhEn("运行中的 Systrace:", "Systrace runs in progress:").text(), Colors.CYAN);
            for (var e : running.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print(Text.zhEn("    持续: ", "    Duration: ").text(), Colors.CYAN);
                ctx.println(e.getValue().getDuration() / 1000.0 + PerformanceTexts.UNIT_SECONDS.text(), Colors.YELLOW);
            }
            ctx.println("", Colors.WHITE);
        }
        if (!completed.isEmpty()) {
            ctx.println(Text.zhEn("已完成的 Systrace:", "Completed systrace runs:").text(), Colors.CYAN);
            for (var e : completed.entrySet()) {
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(e.getKey()), Colors.YELLOW);
                ctx.print(Text.zhEn("    文件: ", "    File: ").text(), Colors.CYAN);
                ctx.println(e.getValue().file(), Colors.GREEN);
            }
            ctx.println("", Colors.WHITE);
        }
    }

    private void printHooks(CommandExecutor.CmdExecContext<?> ctx) {
        var hooks = PerformanceManager.getInstance().listPerformanceHooks();
        if (!hooks.isEmpty()) {
            ctx.println(Text.zhEn("运行中的 Hook:", "Hooks in progress:").text(), Colors.CYAN);
            for (var task : hooks) {
                var s = task.getStats();
                ctx.print("  ID: ", Colors.CYAN);
                ctx.println(String.valueOf(s.id()), Colors.YELLOW);
                ctx.print(PerformanceTexts.LABEL_STATUS_INDENTED.text(), Colors.CYAN);
                ctx.println(task.isRunning() ? PerformanceTexts.STATUS_RUNNING.text() : PerformanceTexts.STATUS_STOPPED.text(),
                            task.isRunning() ? Colors.GREEN : Colors.GRAY);
                ctx.print(Text.zhEn("    类: ", "    Class: ").text(), Colors.CYAN);
                ctx.println(s.className(), Colors.GREEN);
                ctx.print(Text.zhEn("    方法: ", "    Method: ").text(), Colors.CYAN);
                ctx.println(s.methodName(), Colors.GREEN);
                ctx.print(Text.zhEn("    次数: ", "    Calls: ").text(), Colors.CYAN);
                ctx.println(String.valueOf(s.callCount()), Colors.YELLOW);
                ctx.println("", Colors.WHITE);
            }
        }
    }
}
