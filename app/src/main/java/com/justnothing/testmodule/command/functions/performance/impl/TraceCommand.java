package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.functions.performance.PerfTaskManager;
import com.justnothing.testmodule.command.functions.performance.request.*;
import com.justnothing.testmodule.command.functions.performance.response.TraceResult;
import com.justnothing.testmodule.command.functions.performance.trace.TraceData;
import com.justnothing.testmodule.command.functions.performance.trace.Tracer;
import com.justnothing.testmodule.command.output.Colors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SubCommandInfo(
    description = "方法调用链追踪，记录完整的方法进入/退出链路",
    usage = "performance trace <action> [args...]",
    examples = {
        "performance trace start",
        "performance trace stop 1",
        "performance trace report 1",
        "performance trace export 1 /sdcard/trace.json"
    },
    optionsDesc = """
        Actions:
            start                              开始 Trace 追踪
            stop <id>                           停止追踪
            report [id]                         查看报告 (默认最新)
            export <id> <path>                  导出数据"""
)
public class TraceCommand extends AbstractPerfCommand<PerformanceRequest, TraceResult> {

    public TraceCommand() {
        super("performance trace", PerformanceRequest.class, TraceResult.class);
    }

    @Override
    protected TraceResult executePerfCommand(PerformanceRequest req) throws Exception {
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
            outln("未知请求类型", Colors.RED);
            return null;
        }
    }

    private TraceResult handleStart() {
        logger.info("[trace/start] 开始 Trace 追踪");

        PerfTaskManager mgr = getTaskManager();
        int id = mgr.addTracer(new Tracer());
        mgr.getTracer(id).start();

        logger.info("[trace/start] ✅ Tracer 已启动: ID=%d", id);

        TraceResult r = new TraceResult();
        r.setTaskId(id);
        r.setStatus("running");
        outln("Tracer 已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        return r;
    }

    private TraceResult handleStop(TraceStopRequest req) {
        int taskId = req.getTaskId();
        PerfTaskManager mgr = getTaskManager();

        logger.info("[trace/stop] 停止追踪: ID=%d", taskId);

        Tracer t = mgr.getTracer(taskId);
        if (t == null) {
            logger.warn("[trace/stop] ❌ Tracer 不存在: ID=%d, 当前运行中的IDs=%s",
                    taskId, mgr.getTracers().keySet());
            outln("错误: Tracer 不存在 (ID: " + taskId + ")", Colors.RED);
            outln("可用的IDs: " + mgr.getTracers().keySet(), Colors.GRAY);
            outln("提示: 先用 'performance trace start' 启动，再用 'performance trace stop <ID>' 停止", Colors.GRAY);
            return null;
        }

        t.stop();
        List<TraceData> traceData = t.getTraceData();

        logger.info("[trace/stop] Tracer 已停止: ID=%d, 追踪数=%d", taskId, traceData.size());

        mgr.addTraceData(taskId, traceData);

        logger.debug("[trace/stop] 数据已存储: ID=%d, 追踪数=%d", taskId, traceData.size());

        TraceResult r = new TraceResult();
        r.setTaskId(taskId);
        r.setStatus("stopped");
        r.setTraceCount(traceData.size());
        outln("已停止", Colors.YELLOW);
        out("ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out("捕获追踪数: ", Colors.CYAN); outln(String.valueOf(traceData.size()), Colors.YELLOW);
        return r;
    }

    private TraceResult handleReport(TraceReportRequest req) {
        Integer taskId = req.getTaskId();

        logger.info("[trace/report] 查询报告: ID=%s", taskId != null ? String.valueOf(taskId) : "(最新)");

        if (taskId == null) {
            logger.warn("[trace/report] 未指定ID，尝试查找最新数据");
            outln("未指定ID，查找最新完成的追踪...", Colors.GRAY);
        }

        PerfTaskManager mgr = getTaskManager();

        if (taskId == null) {
            Integer latestId = findLatestId(mgr.getTraceDataMap());
            if (latestId == null) {
                logger.warn("[trace/report] ❌ 无任何已完成数据");
                outln("错误: 没有已完成的追踪数据", Colors.RED);
                outln("提示: 先用 'performance trace start' 开始追踪，再用 'performance trace stop <ID>' 停止", Colors.GRAY);
                return null;
            }
            taskId = latestId;
            logger.info("[trace/report] 自动选择最新ID: %d", taskId);
            out("使用最新ID: ", Colors.GRAY); outln(String.valueOf(taskId), Colors.YELLOW);
        }

        List<TraceData> data = mgr.getTraceData(taskId);

        if (data == null || data.isEmpty()) {
            Map<Integer, ?> availableIds = mgr.getTraceDataMap();
            logger.warn("[trace/report] ❌ 数据不存在或为空: ID=%d, 可用IDs=%s", taskId, availableIds.keySet());
            outln("错误: 数据不存在 (ID: " + taskId + ")", Colors.RED);
            if (!availableIds.isEmpty()) {
                outln("可用的报告ID: " + availableIds.keySet(), Colors.GRAY);
            } else {
                outln("提示: 没有任何已完成的追踪。请先执行 'performance trace stop <ID>'", Colors.GRAY);
            }
            return null;
        }

        long totalDuration = 0;
        for (TraceData td : data) {
            totalDuration += td.duration();
        }

        logger.info("[trace/report] ✅ 找到数据: ID=%d, 追踪数=%d, 总耗时=%s",
                taskId, data.size(), formatDurationNs(totalDuration));

        TraceResult r = new TraceResult();
        r.setTaskId(taskId);
        r.setTraceCount(data.size());

        outln("", Colors.DEFAULT);
        outln("=== Trace 追踪报告 ===", Colors.CYAN);
        out("任务ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out("追踪数: ", Colors.CYAN); outln(String.valueOf(data.size()), Colors.WHITE);
        out("总耗时: ", Colors.CYAN); outln(formatDurationNs(totalDuration), Colors.WHITE);
        outln("", Colors.DEFAULT);

        ArrayList<TraceResult.TraceEntry> entries = new ArrayList<>();
        int index = 0;
        for (TraceData d : data) {
            index++;
            outln(String.format(
                    Locale.getDefault(),
                    "[%4d]  %-40s %s (%s)", index, d.name(), d.getDurationString(), d.threadName()), Colors.GRAY);
            var te = new TraceResult.TraceEntry();
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

    private TraceResult handleExport(TraceExportRequest req) throws JSONException {
        int taskId = req.getTaskId();
        String filePath = req.getFilePath();

        logger.info("[trace/export] 导出数据: ID=%d, path=%s", taskId, filePath);

        PerfTaskManager mgr = getTaskManager();
        List<TraceData> td = mgr.getTraceData(taskId);
        if (td == null || td.isEmpty()) {
            logger.warn("[trace/export] ❌ 数据不存在或为空: ID=%d, 可用IDs=%s", taskId, mgr.getTraceDataMap().keySet());
            outln("错误: 数据不存在 (ID: " + taskId + ")", Colors.RED);
            outln("可用的报告ID: " + mgr.getTraceDataMap().keySet(), Colors.GRAY);
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
            outln("导出失败: 无法写入文件", Colors.RED);
            return null;
        }

        logger.info("[trace/export] ✅ 导出成功: %s, 追踪数=%d", filePath, td.size());

        TraceResult r = new TraceResult();
        r.setTaskId(taskId);
        r.setStatus("exported");
        r.setExportPath(filePath);
        outln("数据已导出", Colors.GREEN);
        out("路径: ", Colors.CYAN); outln(filePath, Colors.YELLOW);
        out("追踪数: ", Colors.CYAN); outln(String.valueOf(td.size()), Colors.WHITE);
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
