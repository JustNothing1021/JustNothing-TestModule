package com.justnothing.testmodule.command.functions.performance.impl;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.functions.intercept.PerformanceInterceptTask;
import com.justnothing.testmodule.command.functions.performance.PerformanceManager;
import com.justnothing.testmodule.command.functions.performance.request.PerfHookRequest;
import com.justnothing.testmodule.command.functions.performance.response.PerfHookResult;
import com.justnothing.testmodule.command.output.Colors;

import java.util.List;

public class HookCommand extends AbstractPerfCommand<PerfHookRequest, PerfHookResult> {

    public HookCommand() {
        super("performance hook", PerfHookRequest.class, PerfHookResult.class);
    }

    @Override
    protected PerfHookResult executePerfCommand(PerfHookRequest req) {
        String action = req.getAction();
        if ("start".equals(action) || "add".equals(action) || action == null || action.isEmpty())
            return handleAdd(req);
        return switch (action) {
            case "stop" -> handleStop(req);
            case "report" -> handleReport(req);
            case "export" -> handleExport(req);
            default -> handleAdd(req);
        };
    }

    private PerfHookResult handleAdd(PerfHookRequest req) {
        String cn = req.getClassName();
        if (cn == null || cn.isEmpty()) {
            outln("需要类名", Colors.RED);
            return null;
        }
        String mn = req.getMethodName();
        String sig = req.getSignature();
        try {
            int id = PerformanceManager.getInstance().addPerformanceHook(cn, mn != null ? mn : "*", sig,
                    context.classLoader());
            PerfHookResult r = new PerfHookResult();
            r.setTaskId(id);
            r.setStatus("running");
            r.setClassName(cn);
            r.setMethodName(mn != null ? mn : "*");
            outln("Hook 已添加", Colors.GREEN);
            out("ID: ", Colors.CYAN);
            outln(String.valueOf(id), Colors.YELLOW);
            return r;
        } catch (Exception e) {
            outln("错误: " + e.getMessage(), Colors.RED);
            return null;
        }
    }

    private PerfHookResult handleStop(PerfHookRequest req) {
        if (req.getTaskId() == null) {
            outln("需要 ID", Colors.RED);
            return null;
        }
        if (!PerformanceManager.getInstance().hasPerformanceHook(req.getTaskId())) {
            outln("不存在", Colors.RED);
            return null;
        }
        PerformanceManager.getInstance().stopPerformanceHook(req.getTaskId());
        var stats = PerformanceManager.getInstance().getStats(req.getTaskId());
        PerfHookResult r = setResultData(req, stats);
        outln("已停止", Colors.YELLOW);
        return r;
    }

    @NonNull
    private static PerfHookResult setResultData(PerfHookRequest req, PerformanceInterceptTask.PerformanceStats stats) {
        PerfHookResult r = new PerfHookResult();
        r.setTaskId(req.getTaskId());
        r.setStatus("stopped");
        if (stats != null) {
            r.setClassName(stats.className());
            r.setMethodName(stats.methodName());
            r.setSignature(stats.signature());
            r.setCallCount(stats.callCount());
            r.setTotalDurationNs(stats.totalDurationNs());
            r.setAvgDurationNs(stats.avgDurationNs());
            r.setMinDurationNs(stats.minDurationNs());
            r.setMaxDurationNs(stats.maxDurationNs());
            r.setMonitorDuration(stats.getDurationMs());
        }
        return r;
    }

    private PerfHookResult handleReport(PerfHookRequest req) {
        if (req.getTaskId() != null) {
            var stats = PerformanceManager.getInstance().getStats(req.getTaskId());
            if (stats == null) {
                outln("不存在", Colors.RED);
                return null;
            }
            PerfHookResult r = new PerfHookResult();
            r.setTaskId(req.getTaskId());
            r.setStatus("reported");
            r.setClassName(stats.className());
            r.setMethodName(stats.methodName());
            r.setSignature(stats.signature());
            r.setCallCount(stats.callCount());
            r.setTotalDurationNs(stats.totalDurationNs());
            r.setAvgDurationNs(stats.avgDurationNs());
            r.setMinDurationNs(stats.minDurationNs());
            r.setMaxDurationNs(stats.maxDurationNs());
            r.setMonitorDuration(stats.getDurationMs());
            printStats(stats);
            return r;
        } else {
            List<PerformanceInterceptTask> hooks = PerformanceManager.getInstance().listPerformanceHooks();
            if (hooks.isEmpty()) {
                outln("无数据", Colors.GRAY);
                return null;
            }
            for (var t : hooks)
                printStats(t.getStats());
        }
        return null;
    }

    private void printStats(PerformanceInterceptTask.PerformanceStats s) {
        outln("=== Hook 报告 ===", Colors.CYAN);
        out("ID/类/方法: ", Colors.CYAN);
        outln(s.id() + "/" + s.className() + "/" + s.methodName(), Colors.YELLOW);
        out("调用次数: ", Colors.CYAN);
        outln(String.valueOf(s.callCount()), Colors.YELLOW);
        out("总耗时: ", Colors.CYAN);
        outln(formatDurationNs(s.totalDurationNs()), Colors.YELLOW);
        out("平均/最小/最大: ", Colors.CYAN);
        outln(formatDurationNs((long) s.avgDurationNs()) + "/" + formatDurationNs(s.minDurationNs()) + "/"
                + formatDurationNs(s.maxDurationNs()), Colors.YELLOW);
    }

    private PerfHookResult handleExport(PerfHookRequest req) {
        if (req.getTaskId() == null || req.getFilePath() == null) {
            outln("参数不足", Colors.RED);
            return null;
        }
        var stats = PerformanceManager.getInstance().getStats(req.getTaskId());
        if (stats == null) {
            outln("不存在", Colors.RED);
            return null;
        }
        PerfHookResult r = new PerfHookResult();
        r.setTaskId(req.getTaskId());
        r.setStatus("exported");
        r.setClassName(stats.className());
        r.setMethodName(stats.methodName());
        r.setSignature(stats.signature());
        r.setCallCount(stats.callCount());
        r.setTotalDurationNs(stats.totalDurationNs());
        r.setAvgDurationNs(stats.avgDurationNs());
        r.setExportPath(req.getFilePath());
        outln("已导出", Colors.GREEN);
        return r;
    }
}
