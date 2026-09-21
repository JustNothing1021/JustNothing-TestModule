package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.functions.performance.util.PerfTaskManager;
import com.justnothing.testmodule.command.functions.performance.request.PerformanceRequest;
import com.justnothing.testmodule.command.functions.performance.request.*;
import com.justnothing.testmodule.command.functions.performance.response.SystraceResult;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceData;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceParser;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceRunner;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

@SubCommandInfo(
    description = PerformanceTexts.SUB_PERFORMANCE_SYSTRACE_DESC,
    usage = "performance systrace <action> [args...]",
    examples = {
        "performance systrace start 10 gfx view am",
        "performance systrace stop 1",
        "performance systrace report 1",
        "performance systrace export 1 /sdcard/systrace.html"
    },
    optionsDesc = PerformanceTexts.SUB_PERFORMANCE_SYSTRACE_OPTIONS
)
public class SystraceCommand extends AbstractPerfCommand<PerformanceRequest<?>, SystraceResult> {

    @SuppressWarnings("unchecked")
    public SystraceCommand() {
        super("performance systrace", (Class) PerformanceRequest.class, SystraceResult.class);
    }

    @Override
    protected SystraceResult executePerfCommand(PerformanceRequest<?> req) {
        logger.debug("[systrace] 收到请求: %s", req.getClass().getSimpleName());

        if (req instanceof SystraceStartRequest startReq) {
            return handleStart(startReq);
        } else if (req instanceof SystraceStopRequest stopReq) {
            return handleStop(stopReq);
        } else if (req instanceof SystraceReportRequest reportReq) {
            return handleReport(reportReq);
        } else if (req instanceof SystraceExportRequest exportReq) {
            return handleExport(exportReq);
        } else {
            logger.warn("[systrace] 未知请求类型: %s", req.getClass().getName());
            outln(PerformanceTexts.UNKNOWN_REQUEST_TYPE.text(), Colors.RED);
            return null;
        }
    }

    private SystraceResult handleStart(SystraceStartRequest req) {
        Integer dur = req.getDuration();
        int duration = dur != null ? dur : 10;
        String categoriesStr = req.getCategories();
        String[] categories = categoriesStr != null && !categoriesStr.isEmpty()
            ? categoriesStr.split("\\s+") : null;

        logger.info("[systrace/start] 启动Systrace: duration=%ds, categories=%s",
                duration, categories != null ? Arrays.toString(categories) : "(default)");

        if (duration <= 0) {
            logger.warn("[systrace/start] ❌ 持续时间无效: %d", duration);
            outln(Text.zhEn("错误: 持续时间必须 > 0", "Error: duration must be greater than 0").text(), Colors.RED);
            return null;
        }
        if (duration > 300) {
            logger.warn("[systrace/start] 持续时间过长: %ds，可能产生大量数据", duration);
            outln(Text.zhEn("警告: 持续时间过长，可能产生大量数据",
                    "Warning: duration is too long; it may produce a lot of data").text(), Colors.YELLOW);
        }

        SystraceRunner runner = new SystraceRunner("/data/local/tmp/systrace");
        runner.start(duration, categories);
        int id = getTaskManager().addSystraceRunner(runner);

        logger.info("[systrace/start] ✅ Systrace已启动: ID=%d, duration=%ds, categories=%s",
                id, duration, categories != null ? Arrays.toString(categories) : "(default)");

        SystraceResult r = new SystraceResult();
        r.setTaskId(id);
        r.setStatus("running");
        r.setDuration(duration);
        outln(Text.zhEn("Systrace 已启动", "Systrace started").text(), Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out(PerformanceTexts.LABEL_DURATION.text(), Colors.CYAN);
        outln(duration + "s", Colors.YELLOW);
        if (categories != null && categories.length > 0) {
            out(PerformanceTexts.LABEL_TRACE_CATEGORIES.text(), Colors.CYAN);
            outln(String.join(", ", categories), Colors.YELLOW);
        } else {
            out(PerformanceTexts.LABEL_TRACE_CATEGORIES.text(), Colors.CYAN);
            outln(Text.zhEn("(默认全部)", "(all by default)").text(), Colors.GRAY);
        }
        return r;
    }

    private SystraceResult handleStop(SystraceStopRequest req) {
        int taskId = req.getTaskId();
        PerfTaskManager mgr = getTaskManager();

        logger.info("[systrace/stop] 停止Systrace: ID=%d", taskId);

        SystraceRunner sr = mgr.getSystraceRunner(taskId);
        if (sr == null) {
            Map<Integer, ?> availableIds = mgr.getSystraceRunners();
            logger.warn("[systrace/stop] ❌ 运行器不存在: ID=%d, 当前运行中的IDs=%s",
                    taskId, availableIds.keySet());
            outln(Text.zhEn("错误: Systrace运行器不存在 (ID: %d)", "Error: systrace runner not found (ID: %d)").format(taskId), Colors.RED);
            if (!availableIds.isEmpty()) {
                outln(Text.zhEn("当前运行的Systrace IDs: %s", "Running systrace IDs: %s").format(availableIds.keySet()), Colors.GRAY);
            } else {
                outln(Text.zhEn("提示: 没有任何正在运行的Systrace任务", "Hint: no systrace task is running").text(), Colors.GRAY);
            }
            outln(PerformanceTexts.HINT_VIEW_TASKS.text(), Colors.GRAY);
            return null;
        }

        sr.stop();
        long actualDuration = sr.getDuration();

        logger.info("[systrace/stop] 已停止: ID=%d, 实际持续=%.2fs", taskId, actualDuration / 1000.0);

        SystraceResult r = new SystraceResult();
        r.setTaskId(taskId);
        r.setStatus("stopped");
        r.setDuration((int) actualDuration);
        String f = sr.getOutputFile();
        if (f != null && !f.isEmpty()) {
            try {
                SystraceData d = SystraceParser.parse(f);
                getTaskManager().addSystraceData(taskId, d);
                r.setOutputFile(f);

                logger.info("[systrace/stop] ✅ 数据已解析并存储: ID=%d, file=%s, threads=%d",
                        taskId, f, d.threadData() != null ? d.threadData().size() : 0);
                logger.debug("[systrace/stop] detail: file=%s, size=%s", f, d.file());

                outln(Text.zhEn("Systrace 已停止，数据已解析", "Systrace stopped; data parsed").text(), Colors.GREEN);
                out("ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
                out(PerformanceTexts.LABEL_OUTPUT_FILE.text(), Colors.CYAN); outln(f, Colors.YELLOW);
                out(Text.zhEn("实际持续时间: ", "Actual duration: ").text(), Colors.CYAN);
                outln(String.format(Locale.getDefault(), "%.2fs", actualDuration / 1000.0), Colors.YELLOW);
                if (d.threadData() != null) {
                    out(Text.zhEn("线程数量: ", "Thread count: ").text(), Colors.CYAN);
                    outln(String.valueOf(d.threadData().size()), Colors.YELLOW);
                }
            } catch (Exception e) {
                logger.warn("[systrace/stop] ⚠️ 停止成功但解析失败: ID=%d, file=%s, err=%s",
                        taskId, f, e.getMessage());
                outln(Text.zhEn("已停止但解析失败: %s", "Stopped, but parsing failed: %s").format(e.getMessage()), Colors.YELLOW);
                out("ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
                out(PerformanceTexts.LABEL_OUTPUT_FILE.text(), Colors.CYAN); outln(f, Colors.GRAY);
            }
        } else {
            logger.warn("[systrace/stop] ⚠️ 无输出文件: ID=%d", taskId);
            outln(Text.zhEn("已停止，但未生成输出文件", "Stopped, but no output file was produced").text(), Colors.YELLOW);
            out("ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        }
        return r;
    }

    private SystraceResult handleReport(SystraceReportRequest req) {
        Integer taskId = req.getTaskId();

        logger.info("[systrace/report] 查询报告: ID=%s", taskId != null ? String.valueOf(taskId) : "(最新)");

        if (taskId == null) {
            logger.warn("[systrace/report] 未指定ID，尝试查找最新数据");
            outln(Text.zhEn("未指定ID，查找最新的Systrace数据...", "No ID given; looking for the latest systrace data...").text(), Colors.GRAY);
        }

        PerfTaskManager mgr = getTaskManager();

        if (taskId == null) {
            Integer latestId = findLatestId(mgr.getSystraceDataMap());
            if (latestId == null) {
                logger.warn("[systrace/report] ❌ 无任何已完成数据");
                outln(Text.zhEn("错误: 没有已完成的Systrace数据", "Error: no completed systrace data").text(), Colors.RED);
                outln(Text.zhEn("提示: 先用 'performance systrace start' 开始追踪，再用 'performance systrace stop <ID>' 停止",
                        "Hint: run 'performance systrace start' to start tracing, then 'performance systrace stop <ID>' to stop").text(), Colors.GRAY);
                return null;
            }
            taskId = latestId;
            logger.info("[systrace/report] 自动选择最新ID: %d", taskId);
            out(PerformanceTexts.USING_LATEST_ID.text(), Colors.GRAY); outln(String.valueOf(taskId), Colors.YELLOW);
        }

        SystraceData d = mgr.getSystraceData(taskId);

        if (d == null) {
            Map<Integer, ?> availableIds = mgr.getSystraceDataMap();
            logger.warn("[systrace/report] ❌ 数据不存在: ID=%d, 可用IDs=%s",
                    taskId, availableIds.keySet());
            outln(PerformanceTexts.ERR_DATA_NOT_FOUND.format(taskId), Colors.RED);
            if (!availableIds.isEmpty()) {
                outln(PerformanceTexts.AVAILABLE_REPORT_IDS.format(availableIds.keySet()), Colors.GRAY);
            } else {
                outln(Text.zhEn("提示: 没有已完成的Systrace数据。请先执行 'performance systrace stop <ID>'",
                        "Hint: there is no completed systrace data. Run 'performance systrace stop <ID>' first").text(), Colors.GRAY);
            }
            outln(PerformanceTexts.HINT_VIEW_TASKS.text(), Colors.GRAY);
            return null;
        }

        boolean hasData = d.threadData() != null && !d.threadData().isEmpty();
        if (!hasData) {
            logger.warn("[systrace/report] ⚠️ 数据为空: ID=%d (无线程数据)", taskId);
            outln(PerformanceTexts.WARN_REPORT_DATA_EMPTY.format(taskId), Colors.YELLOW);
            outln(Text.zhEn("该Systrace周期内没有捕获到任何trace事件",
                    "No trace events were captured during this systrace window").text(), Colors.GRAY);
            out(PerformanceTexts.LABEL_SOURCE_FILE.text(), Colors.CYAN); outln(d.file(), Colors.WHITE);
            return null;
        }

        logger.info("[systrace/report] ✅ 找到数据: ID=%d, threads=%d, duration=%s",
                taskId, d.threadData().size(),
                String.format(Locale.getDefault(), "%.2fs", d.duration() / 1000.0));

        String report = SystraceParser.generateReport(d);
        SystraceResult r = new SystraceResult();
        r.setTaskId(taskId);
        r.setOutputFile(d.file());
        r.setReport(report);

        outln("", Colors.DEFAULT);
        outln(Text.zhEn("=== Systrace 报告 ===", "=== Systrace report ===").text(), Colors.CYAN);
        out(PerformanceTexts.LABEL_TASK_ID.text(), Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out(PerformanceTexts.LABEL_SOURCE_FILE.text(), Colors.CYAN); outln(d.file(), Colors.WHITE);
        out(PerformanceTexts.LABEL_THREAD_COUNT.text(), Colors.CYAN); outln(String.valueOf(d.threadData().size()), Colors.WHITE);
        outln(report, Colors.WHITE);

        logger.debug("[systrace/report] 报告生成完毕: ID=%d, reportLen=%d", taskId, report.length());
        return r;
    }

    private SystraceResult handleExport(SystraceExportRequest req) {
        int taskId = req.getTaskId();
        String filePath = req.getFilePath();

        logger.info("[systrace/export] 导出数据: ID=%d, path=%s", taskId, filePath);

        PerfTaskManager mgr = getTaskManager();
        SystraceData d = mgr.getSystraceData(taskId);
        if (d == null) {
            Map<Integer, ?> availableIds = mgr.getSystraceDataMap();
            logger.warn("[systrace/export] ❌ 数据不存在: ID=%d, 可用IDs=%s",
                    taskId, availableIds.keySet());
            outln(PerformanceTexts.ERR_DATA_NOT_FOUND.format(taskId), Colors.RED);
            if (!availableIds.isEmpty()) {
                outln(PerformanceTexts.AVAILABLE_REPORT_IDS.format(availableIds.keySet()), Colors.GRAY);
            } else {
                outln(Text.zhEn("提示: 没有已完成的Systrace数据", "Hint: there is no completed systrace data").text(), Colors.GRAY);
            }
            return null;
        }

        String rpt = SystraceParser.generateReport(d);
        if (!writeToFile(filePath, rpt)) {
            logger.error("[systrace/export] ❌ 写入文件失败: %s", filePath);
            outln(PerformanceTexts.ERR_EXPORT_WRITE_FAILED.text(), Colors.RED);
            return null;
        }

        logger.info("[systrace/export] ✅ 导出成功: ID=%d, path=%s, threads=%d",
                taskId, filePath, d.threadData() != null ? d.threadData().size() : 0);

        SystraceResult r = new SystraceResult();
        r.setTaskId(taskId);
        r.setOutputFile(d.file());
        r.setExportPath(filePath);
        outln(Text.zhEn("Systrace 报告已导出", "Systrace report exported").text(), Colors.GREEN);
        out(PerformanceTexts.LABEL_PATH.text(), Colors.CYAN); outln(filePath, Colors.YELLOW);
        out(PerformanceTexts.LABEL_TASK_ID.text(), Colors.CYAN); outln(String.valueOf(taskId), Colors.WHITE);
        out(PerformanceTexts.LABEL_THREAD_COUNT.text(), Colors.CYAN);
        outln(d.threadData() != null ? String.valueOf(d.threadData().size()) : "N/A", Colors.WHITE);
        return r;
    }

    private Integer findLatestId(Map<Integer, ?> dataMap) {
        int maxId = -1;
        for (Integer id : dataMap.keySet()) {
            if (id > maxId) maxId = id;
        }
        return maxId >= 0 ? maxId : null;
    }
}
