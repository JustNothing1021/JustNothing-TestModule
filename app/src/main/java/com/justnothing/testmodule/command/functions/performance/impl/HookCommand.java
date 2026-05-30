package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.functions.intercept.PerformanceInterceptTask;
import com.justnothing.testmodule.command.functions.performance.PerformanceManager;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.functions.performance.request.*;
import com.justnothing.testmodule.command.functions.performance.response.PerfHookResult;
import com.justnothing.testmodule.command.output.Colors;

import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

@SubCommandInfo(
    description = "性能 Hook 注入，在目标方法前后插入计时探针",
    usage = "performance hook <action> [args...]",
    examples = {
        "performance hook start com.example.MyClass myMethod",
        "performance hook start com.example.MyClass myMethod '(Ljava/lang/String;)V'",
        "performance hook stop 1",
        "performance hook report 1",
        "performance hook export 1 /sdcard/hook.json"
    },
    optionsDesc = """
        Actions:
            start <class> <method> [sig]         添加性能 Hook
            stop <id>                            停止 Hook
            report [id]                          查看报告 (默认最新)
            export <id> <path>                   导出数据"""
)
public class HookCommand extends AbstractPerfCommand<PerformanceRequest, PerfHookResult> {

    public HookCommand() {
        super("performance hook", PerformanceRequest.class, PerfHookResult.class);
    }

    @Override
    protected PerfHookResult executePerfCommand(PerformanceRequest req) {
        logger.debug("[hook] 收到请求: %s", req.getClass().getSimpleName());

        if (req instanceof PerfHookStartRequest startReq) {
            return handleAdd(startReq);
        } else if (req instanceof PerfHookStopRequest stopReq) {
            return handleStop(stopReq);
        } else if (req instanceof PerfHookReportRequest reportReq) {
            return handleReport(reportReq);
        } else if (req instanceof PerfHookExportRequest exportReq) {
            return handleExport(exportReq);
        } else {
            logger.warn("[hook] 未知请求类型: %s", req.getClass().getName());
            outln("未知请求类型", Colors.RED);
            return null;
        }
    }

    private PerfHookResult handleAdd(PerfHookStartRequest req) {
        String cn = req.getClassName();
        String mn = req.getMethodName();
        String sig = req.getSignature();

        logger.info("[hook/start] 添加Hook: class=%s, method=%s, sig=%s",
                cn, mn != null ? mn : "*", sig != null ? sig : "(auto)");

        if (cn == null || cn.isEmpty()) {
            logger.warn("[hook/start] ❌ 类名为空");
            outln("错误: 需要类名", Colors.RED);
            return null;
        }

        try {
            int id = PerformanceManager.getInstance().addPerformanceHook(cn, mn != null ? mn : "*", sig,
                    context.classLoader());

            logger.info("[hook/start] ✅ Hook已添加: ID=%d, class=%s, method=%s",
                    id, cn, mn != null ? mn : "*");

            PerfHookResult r = new PerfHookResult();
            r.setTaskId(id);
            r.setStatus("running");
            r.setClassName(cn);
            r.setMethodName(mn != null ? mn : "*");
            outln("Hook 已添加", Colors.GREEN);
            out("ID: ", Colors.CYAN);
            outln(String.valueOf(id), Colors.YELLOW);
            out("目标类: ", Colors.CYAN); outln(cn, Colors.YELLOW);
            out("目标方法: ", Colors.CYAN); outln(mn != null ? mn : "* (所有方法)", Colors.YELLOW);
            if (sig != null && !sig.isEmpty()) {
                out("方法签名: ", Colors.CYAN); outln(sig, Colors.GRAY);
            }
            return r;
        } catch (Exception e) {
            logger.warn("[hook/start] ❌ 添加Hook失败: class=%s, err=%s", cn, e.getMessage());
            outln("错误: " + e.getMessage(), Colors.RED);
            return null;
        }
    }

    private PerfHookResult handleStop(PerfHookStopRequest req) {
        int taskId = req.getTaskId();

        logger.info("[hook/stop] 停止Hook: ID=%d", taskId);

        if (!PerformanceManager.getInstance().hasPerformanceHook(taskId)) {
            List<PerformanceInterceptTask> allHooks = PerformanceManager.getInstance().listPerformanceHooks();
            List<Integer> availableIds = allHooks.stream()
                    .map(h -> h.getStats().id())
                    .collect(Collectors.toList());
            logger.warn("[hook/stop] ❌ Hook不存在: ID=%d, 当前活跃IDs=%s",
                    taskId, availableIds);
            outln("错误: Hook不存在 (ID: " + taskId + ")", Colors.RED);
            if (!availableIds.isEmpty()) {
                outln("当前活跃的Hook IDs: " + availableIds, Colors.GRAY);
            } else {
                outln("提示: 没有任何正在运行的Hook任务", Colors.GRAY);
            }
            outln("提示: 使用 'performance list' 查看当前任务", Colors.GRAY);
            return null;
        }

        var statsBeforeRemoval = PerformanceManager.getInstance().getStats(taskId);
        if (statsBeforeRemoval != null) {
            logger.info("[hook/stop] 移除前统计: ID=%d, calls=%d, totalNs=%s",
                    taskId, statsBeforeRemoval.callCount(),
                    formatDurationNs(statsBeforeRemoval.totalDurationNs()));
        }

        PerformanceManager.getInstance().stopPerformanceHook(taskId);
        var stats = PerformanceManager.getInstance().getStats(taskId);
        PerfHookResult r = setResultData(taskId, stats);

        logger.info("[hook/stop] ✅ Hook已停止: ID=%d, calls=%d, totalDuration=%s",
                taskId, stats != null ? stats.callCount() : 0,
                stats != null ? formatDurationNs(stats.totalDurationNs()) : "N/A");

        outln("Hook 已停止", Colors.YELLOW);
        out("ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        if (stats != null) {
            out("目标类: ", Colors.CYAN); outln(stats.className(), Colors.WHITE);
            out("目标方法: ", Colors.CYAN); outln(stats.methodName(), Colors.WHITE);
            out("调用次数: ", Colors.CYAN); outln(String.valueOf(stats.callCount()), Colors.YELLOW);
            out("总耗时: ", Colors.CYAN); outln(formatDurationNs(stats.totalDurationNs()), Colors.YELLOW);
            out("平均/最小/最大: ", Colors.CYAN);
            outln(formatDurationNs((long) stats.avgDurationNs()) + "/"
                    + formatDurationNs(stats.minDurationNs()) + "/"
                    + formatDurationNs(stats.maxDurationNs()), Colors.YELLOW);
        }
        return r;
    }

    @NonNull
    private static PerfHookResult setResultData(int taskId, PerformanceInterceptTask.PerformanceStats stats) {
        PerfHookResult r = new PerfHookResult();
        r.setTaskId(taskId);
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

    private PerfHookResult handleReport(PerfHookReportRequest req) {
        Integer taskId = req.getTaskId();

        logger.info("[hook/report] 查询报告: ID=%s", taskId != null ? String.valueOf(taskId) : "(全部)");

        if (taskId == null) {
            logger.warn("[hook/report] 未指定ID，列出所有Hook数据");
            outln("未指定ID，显示所有Hook报告...", Colors.GRAY);
        }

        if (taskId != null) {
            var stats = PerformanceManager.getInstance().getStats(taskId);
            if (stats == null) {
                List<PerformanceInterceptTask> allHooks = PerformanceManager.getInstance().listPerformanceHooks();
                List<Integer> availableIds = allHooks.stream()
                        .map(h -> h.getStats().id())
                        .collect(Collectors.toList());
                logger.warn("[hook/report] ❌ 数据不存在: ID=%d, 可用IDs=%s", taskId, availableIds);
                outln("错误: Hook数据不存在 (ID: " + taskId + ")", Colors.RED);
                if (!availableIds.isEmpty()) {
                    outln("可用的Hook IDs: " + availableIds, Colors.GRAY);
                } else {
                    outln("提示: 没有任何Hook数据。请先执行 'performance hook stop <ID>' 生成报告", Colors.GRAY);
                }
                outln("提示: 使用 'performance list' 查看当前任务", Colors.GRAY);
                return null;
            }

            logger.info("[hook/report] ✅ 找到数据: ID=%d, class=%s, method=%s, calls=%d",
                    taskId, stats.className(), stats.methodName(), stats.callCount());

            PerfHookResult r = new PerfHookResult();
            r.setTaskId(taskId);
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
                logger.warn("[hook/report] ❌ 无任何Hook数据");
                outln("错误: 没有Hook数据", Colors.RED);
                outln("提示: 先用 'performance hook start <class> <method>' 添加Hook，再用 'performance hook stop <ID>' 停止以生成报告", Colors.GRAY);
                return null;
            }

            logger.info("[hook/report] 列出全部Hook: count=%d", hooks.size());

            for (var t : hooks) {
                var s = t.getStats();
                if (s == null || s.callCount() <= 0) {
                    logger.debug("[hook/report] 跳过空Hook: ID=%d", t.getStats() != null ? t.getStats().id() : -1);
                    continue;
                }
                printStats(s);
            }
        }
        return null;
    }

    private void printStats(PerformanceInterceptTask.PerformanceStats s) {
        logger.debug("[hook/report] detail: ID=%d, class=%s, method=%s, calls=%d, totalNs=%d",
                s.id(), s.className(), s.methodName(), s.callCount(), s.totalDurationNs());

        outln("", Colors.DEFAULT);
        outln("=== Hook 报告 ===", Colors.CYAN);
        out("ID: ", Colors.CYAN); outln(String.valueOf(s.id()), Colors.YELLOW);
        out("目标类/方法: ", Colors.CYAN);
        outln(s.className() + "." + s.methodName(), Colors.YELLOW);
        if (s.signature() != null && !s.signature().isEmpty()) {
            out("签名: ", Colors.CYAN); outln(s.signature(), Colors.GRAY);
        }
        out("调用次数: ", Colors.CYAN); outln(String.valueOf(s.callCount()), Colors.YELLOW);
        out("总耗时: ", Colors.CYAN); outln(formatDurationNs(s.totalDurationNs()), Colors.YELLOW);
        out("平均/最小/最大: ", Colors.CYAN);
        outln(formatDurationNs((long) s.avgDurationNs()) + "/" + formatDurationNs(s.minDurationNs()) + "/"
                + formatDurationNs(s.maxDurationNs()), Colors.YELLOW);
        if (s.getDurationMs() > 0) {
            out("监控时长: ", Colors.CYAN);
            outln(s.getDurationMs() + "ms", Colors.WHITE);
        }
    }

    private PerfHookResult handleExport(PerfHookExportRequest req) {
        int taskId = req.getTaskId();
        String filePath = req.getFilePath();

        logger.info("[hook/export] 导出数据: ID=%d, path=%s", taskId, filePath);

        var stats = PerformanceManager.getInstance().getStats(taskId);
        if (stats == null) {
            List<PerformanceInterceptTask> allHooks = PerformanceManager.getInstance().listPerformanceHooks();
            List<Integer> availableIds = allHooks.stream()
                    .map(h -> h.getStats().id())
                    .collect(Collectors.toList());
            logger.warn("[hook/export] ❌ 数据不存在: ID=%d, 可用IDs=%s", taskId, availableIds);
            outln("错误: Hook数据不存在 (ID: " + taskId + ")", Colors.RED);
            if (!availableIds.isEmpty()) {
                outln("可用的Hook IDs: " + availableIds, Colors.GRAY);
            } else {
                outln("提示: 没有可导出的Hook数据", Colors.GRAY);
            }
            return null;
        }

        logger.info("[hook/export] 准备导出: ID=%d, class=%s, method=%s, calls=%d",
                taskId, stats.className(), stats.methodName(), stats.callCount());

        PerfHookResult r = new PerfHookResult();
        r.setTaskId(taskId);
        r.setStatus("exported");
        r.setClassName(stats.className());
        r.setMethodName(stats.methodName());
        r.setSignature(stats.signature());
        r.setCallCount(stats.callCount());
        r.setTotalDurationNs(stats.totalDurationNs());
        r.setAvgDurationNs(stats.avgDurationNs());
        r.setExportPath(filePath);

        JSONObject json = new JSONObject();
        try {
            json.put("id", taskId)
               .put("className", stats.className())
               .put("methodName", stats.methodName())
               .put("signature", stats.signature() != null ? stats.signature() : "")
               .put("callCount", stats.callCount())
               .put("totalDurationNs", stats.totalDurationNs())
               .put("avgDurationNs", stats.avgDurationNs())
               .put("minDurationNs", stats.minDurationNs())
               .put("maxDurationNs", stats.maxDurationNs())
               .put("monitorDurationMs", stats.getDurationMs());

            if (!writeToFile(filePath, json.toString(2))) {
                logger.error("[hook/export] ❌ 写入文件失败: %s", filePath);
                outln("导出失败: 无法写入文件", Colors.RED);
                return null;
            }

            logger.info("[hook/export] ✅ 导出成功: ID=%d, path=%s, bytes=%d",
                    taskId, filePath, json.toString().length());
        } catch (org.json.JSONException e) {
            logger.error("[hook/export] ❌ JSON构建失败: ID=%d, err=%s", taskId, e.getMessage());
            outln("导出失败: JSON构建错误 - " + e.getMessage(), Colors.RED);
            return null;
        }

        outln("Hook 数据已导出", Colors.GREEN);
        out("路径: ", Colors.CYAN); outln(filePath, Colors.YELLOW);
        out("任务ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.WHITE);
        out("目标: ", Colors.CYAN);
        outln(stats.className() + "." + stats.methodName(), Colors.WHITE);
        out("调用次数: ", Colors.CYAN); outln(String.valueOf(stats.callCount()), Colors.WHITE);
        out("总耗时: ", Colors.CYAN); outln(formatDurationNs(stats.totalDurationNs()), Colors.WHITE);
        return r;
    }
}
