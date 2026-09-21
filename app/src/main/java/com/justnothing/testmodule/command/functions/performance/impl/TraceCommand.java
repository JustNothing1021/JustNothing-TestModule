package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.functions.performance.request.PerformanceRequest;
import com.justnothing.testmodule.command.functions.performance.util.PerfTaskManager;
import com.justnothing.testmodule.command.functions.performance.request.*;
import com.justnothing.testmodule.command.functions.performance.response.PerfTraceResult;
import com.justnothing.testmodule.command.functions.performance.trace.TraceData;
import com.justnothing.testmodule.command.functions.performance.trace.Tracer;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SubCommandInfo(
    description = PerformanceTexts.SUB_PERFORMANCE_TRACE_DESC,
    usage = "performance trace <action> [args...]",
    examples = {
        "performance trace start",
        "performance trace stop 1",
        "performance trace report 1",
        "performance trace export 1 /sdcard/trace.json"
    },
    optionsDesc = PerformanceTexts.SUB_PERFORMANCE_TRACE_OPTIONS
)
public class TraceCommand extends AbstractPerfCommand<PerformanceRequest<?>, PerfTraceResult> {

    @SuppressWarnings("unchecked")
    public TraceCommand() {
        super("performance trace", (Class) PerformanceRequest.class, PerfTraceResult.class);
    }

    @Override
    protected PerfTraceResult executePerfCommand(PerformanceRequest<?> req) throws Exception {
        logger.debug("[trace] 收到请求: %s", req.getClass().getSimpleName());

        if (req instanceof TraceStartRequest) {
            return handleStart();
        } else if (req instanceof TraceStopRequest stopReq) {
            return handleStop(stopReq);
        } else if (req instanceof TraceReportRequest reportReq) {
            return handleReport(reportReq);
        } else if (req instanceof TraceExportRequest exportReq) {
            return handleExport(exportReq);
        } else {
            logger.warn("[trace] 未知请求类型: %s", req.getClass().getName());
            outln(PerformanceTexts.UNKNOWN_REQUEST_TYPE.text(), Colors.RED);
            return null;
        }
    }

    private PerfTraceResult handleStart() {
        logger.info("[trace/start] 开始 Trace 追踪");

        PerfTaskManager mgr = getTaskManager();
        int id = mgr.addTracer(new Tracer());
        mgr.getTracer(id).start();

        logger.info("[trace/start] ✅ Tracer 已启动: ID=%d", id);

        PerfTraceResult r = new PerfTraceResult();
        r.setTaskId(id);
        r.setStatus("running");
        outln(Text.zhEn("Tracer 已启动", "Tracer started").text(), Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        return r;
    }

    private PerfTraceResult handleStop(TraceStopRequest req) {
        int taskId = req.getTaskId();
        PerfTaskManager mgr = getTaskManager();

        logger.info("[trace/stop] 停止追踪: ID=%d", taskId);

        Tracer t = mgr.getTracer(taskId);
        if (t == null) {
            logger.warn("[trace/stop] ❌ Tracer 不存在: ID=%d, 当前运行中的IDs=%s",
                    taskId, mgr.getTracers().keySet());
            outln(Text.zhEn("错误: Tracer 不存在 (ID: %d)", "Error: tracer not found (ID: %d)").format(taskId), Colors.RED);
            outln(PerformanceTexts.AVAILABLE_IDS.format(mgr.getTracers().keySet()), Colors.GRAY);
            outln(Text.zhEn("提示: 先用 'performance trace start' 启动，再用 'performance trace stop <ID>' 停止",
                    "Hint: run 'performance trace start' first, then 'performance trace stop <ID>' to stop").text(), Colors.GRAY);
            return null;
        }

        t.stop();
        List<TraceData> traceData = t.getTraceData();

        logger.info("[trace/stop] Tracer 已停止: ID=%d, 追踪数=%d", taskId, traceData.size());

        mgr.addTraceData(taskId, traceData);

        logger.debug("[trace/stop] 数据已存储: ID=%d, 追踪数=%d", taskId, traceData.size());

        PerfTraceResult r = new PerfTraceResult();
        r.setTaskId(taskId);
        r.setStatus("stopped");
        r.setTraceCount(traceData.size());
        outln(PerformanceTexts.STATUS_STOPPED.text(), Colors.YELLOW);
        out("ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out(Text.zhEn("捕获追踪数: ", "Traces captured: ").text(), Colors.CYAN); outln(String.valueOf(traceData.size()), Colors.YELLOW);
        return r;
    }

    private PerfTraceResult handleReport(TraceReportRequest req) {
        Integer taskId = req.getTaskId();

        logger.info("[trace/report] 查询报告: ID=%s", taskId != null ? String.valueOf(taskId) : "(最新)");

        if (taskId == null) {
            logger.warn("[trace/report] 未指定ID，尝试查找最新数据");
            outln(Text.zhEn("未指定ID，查找最新完成的追踪...", "No ID given; looking for the latest completed trace...").text(), Colors.GRAY);
        }

        PerfTaskManager mgr = getTaskManager();

        if (taskId == null) {
            Integer latestId = findLatestId(mgr.getTraceDataMap());
            if (latestId == null) {
                logger.warn("[trace/report] ❌ 无任何已完成数据");
                outln(Text.zhEn("错误: 没有已完成的追踪数据", "Error: no completed trace data").text(), Colors.RED);
                outln(Text.zhEn("提示: 先用 'performance trace start' 开始追踪，再用 'performance trace stop <ID>' 停止",
                        "Hint: run 'performance trace start' to start tracing, then 'performance trace stop <ID>' to stop").text(), Colors.GRAY);
                return null;
            }
            taskId = latestId;
            logger.info("[trace/report] 自动选择最新ID: %d", taskId);
            out(PerformanceTexts.USING_LATEST_ID.text(), Colors.GRAY); outln(String.valueOf(taskId), Colors.YELLOW);
        }

        List<TraceData> data = mgr.getTraceData(taskId);

        if (data == null || data.isEmpty()) {
            Map<Integer, ?> availableIds = mgr.getTraceDataMap();
            logger.warn("[trace/report] ❌ 数据不存在或为空: ID=%d, 可用IDs=%s", taskId, availableIds.keySet());
            outln(PerformanceTexts.ERR_DATA_NOT_FOUND.format(taskId), Colors.RED);
            if (!availableIds.isEmpty()) {
                outln(PerformanceTexts.AVAILABLE_REPORT_IDS.format(availableIds.keySet()), Colors.GRAY);
            } else {
                outln(Text.zhEn("提示: 没有任何已完成的追踪。请先执行 'performance trace stop <ID>'",
                        "Hint: there is no completed trace data. Run 'performance trace stop <ID>' first").text(), Colors.GRAY);
            }
            return null;
        }

        long totalDuration = 0;
        for (TraceData td : data) {
            totalDuration += td.duration();
        }

        logger.info("[trace/report] ✅ 找到数据: ID=%d, 追踪数=%d, 总耗时=%s",
                taskId, data.size(), formatDurationNs(totalDuration));

        PerfTraceResult r = new PerfTraceResult();
        r.setTaskId(taskId);
        r.setTraceCount(data.size());

        outln("", Colors.DEFAULT);
        outln(Text.zhEn("=== Trace 追踪报告 ===", "=== Trace report ===").text(), Colors.CYAN);
        out(PerformanceTexts.LABEL_TASK_ID.text(), Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out(PerformanceTexts.LABEL_TRACE_COUNT.text(), Colors.CYAN); outln(String.valueOf(data.size()), Colors.WHITE);
        out(PerformanceTexts.LABEL_TOTAL_TIME.text(), Colors.CYAN); outln(formatDurationNs(totalDuration), Colors.WHITE);
        outln("", Colors.DEFAULT);

        ArrayList<PerfTraceResult.TraceEntry> entries = new ArrayList<>();
        int index = 0;
        for (TraceData d : data) {
            index++;
            outln(String.format(
                    Locale.getDefault(),
                    "[%4d]  %-40s %s (%s)", index, d.name(), d.getDurationString(), d.threadName()), Colors.GRAY);
            var te = new PerfTraceResult.TraceEntry();
            te.setName(d.name());
            te.setStartTime(d.startTime());
            te.setDuration(d.duration());
            te.setThreadId(d.threadId());
            te.setThreadName(d.threadName());
            entries.add(te);
        }
        r.setTraces(entries);

        logger.debug("[trace/report] 报告生成完毕: %d 条追踪记录", entries.size());
        return r;
    }

    private PerfTraceResult handleExport(TraceExportRequest req) throws JSONException {
        int taskId = req.getTaskId();
        String filePath = req.getFilePath();

        logger.info("[trace/export] 导出数据: ID=%d, path=%s", taskId, filePath);

        PerfTaskManager mgr = getTaskManager();
        List<TraceData> td = mgr.getTraceData(taskId);
        if (td == null || td.isEmpty()) {
            logger.warn("[trace/export] ❌ 数据不存在或为空: ID=%d, 可用IDs=%s", taskId, mgr.getTraceDataMap().keySet());
            outln(PerformanceTexts.ERR_DATA_NOT_FOUND.format(taskId), Colors.RED);
            outln(PerformanceTexts.AVAILABLE_REPORT_IDS.format(mgr.getTraceDataMap().keySet()), Colors.GRAY);
            return null;
        }

        JSONObject j = new JSONObject();
        j.put("id", taskId).put("traceCount", td.size());
        JSONArray a = new JSONArray();
        for (TraceData d : td) {
            JSONObject o = new JSONObject();
            o.put("name", d.name()).put("duration", d.duration()).put("threadName", d.threadName());
            a.put(o);
        }
        j.put("traces", a);

        if (!writeToFile(filePath, j.toString(2))) {
            logger.error("[trace/export] ❌ 写入文件失败: %s", filePath);
            outln(PerformanceTexts.ERR_EXPORT_WRITE_FAILED.text(), Colors.RED);
            return null;
        }

        logger.info("[trace/export] ✅ 导出成功: %s, 追踪数=%d", filePath, td.size());

        PerfTraceResult r = new PerfTraceResult();
        r.setTaskId(taskId);
        r.setStatus("exported");
        r.setExportPath(filePath);
        outln(PerformanceTexts.DATA_EXPORTED.text(), Colors.GREEN);
        out(PerformanceTexts.LABEL_PATH.text(), Colors.CYAN); outln(filePath, Colors.YELLOW);
        out(PerformanceTexts.LABEL_TRACE_COUNT.text(), Colors.CYAN); outln(String.valueOf(td.size()), Colors.WHITE);
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
