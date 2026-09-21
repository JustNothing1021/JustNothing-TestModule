package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.functions.performance.request.PerformanceRequest;
import com.justnothing.testmodule.command.functions.performance.util.PerfTaskManager;
import com.justnothing.testmodule.command.functions.performance.request.*;
import com.justnothing.testmodule.command.functions.performance.response.SampleResult;
import com.justnothing.testmodule.command.functions.performance.sampler.SimpleSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.SimpleSampler;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@SubCommandInfo(
    description = PerformanceTexts.SUB_PERFORMANCE_SAMPLE_DESC,
    usage = "performance sample <action> [args...]",
    examples = {
        "performance sample start 1000",
        "performance sample start 500 --exclude android.*",
        "performance sample stop 1",
        "performance sample report 1",
        "performance sample report",
        "performance sample export 1 /sdcard/sample.json"
    },
    optionsDesc = PerformanceTexts.SUB_PERFORMANCE_SAMPLE_OPTIONS
)
public class SampleCommand extends AbstractPerfCommand<PerformanceRequest<?>, SampleResult> {

    @SuppressWarnings("unchecked")
    public SampleCommand() {
        super("performance sample", (Class) PerformanceRequest.class, SampleResult.class);
    }

    @Override
    protected SampleResult executePerfCommand(PerformanceRequest<?> request) throws Exception {
        logger.debug("[sample] 收到请求: %s", request.getClass().getSimpleName());

        if (request instanceof SampleStartRequest startReq) {
            return handleStart(startReq);
        } else if (request instanceof SampleStopRequest stopReq) {
            return handleStop(stopReq);
        } else if (request instanceof SampleReportRequest reportReq) {
            return handleReport(reportReq);
        } else if (request instanceof SampleExportRequest exportReq) {
            return handleExport(exportReq);
        } else {
            logger.warn("[sample] 未知请求类型: %s", request.getClass().getName());
            outln(PerformanceTexts.UNKNOWN_REQUEST_TYPE.text(), Colors.RED);
            return null;
        }
    }

    private SampleResult handleStart(SampleStartRequest req) {
        int rate = req.getRate();
        String exclude = req.getExclude();

        logger.info("[sample/start] 开始采样: rate=%d Hz, exclude=%s", rate, exclude);

        if (rate <= 0) {
            logger.warn("[sample/start] 频率无效: %d", rate);
            outln(PerformanceTexts.ERR_RATE_MUST_BE_POSITIVE.text(), Colors.RED);
            return null;
        }
        if (rate > 10000) {
            logger.warn("[sample/start] 频率过高: %d Hz，可能影响性能", rate);
            outln(PerformanceTexts.WARN_RATE_TOO_HIGH.text(), Colors.YELLOW);
        }

        PerfTaskManager mgr = getTaskManager();
        int id = mgr.addSimpleSampler(new SimpleSampler(rate));
        mgr.getSimpleSampler(id).start();

        logger.info("[sample/start] ✅ 采样器已启动: ID=%d, rate=%dHz", id, rate);

        SampleResult r = new SampleResult();
        r.setTaskId(id);
        r.setSampleRate(rate);
        r.setStatus("running");
        outln(Text.zhEn("采样器已启动", "Sampler started").text(), Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out(PerformanceTexts.LABEL_RATE.text(), Colors.CYAN);
        outln(rate + " Hz", Colors.YELLOW);
        if (exclude != null && !exclude.isEmpty()) {
            out(Text.zhEn("排除模式: ", "Exclude pattern: ").text(), Colors.CYAN);
            outln(exclude, Colors.YELLOW);
        }
        return r;
    }

    private SampleResult handleStop(SampleStopRequest req) {
        int taskId = req.getTaskId();
        PerfTaskManager mgr = getTaskManager();

        logger.info("[sample/stop] 停止采样: ID=%d", taskId);

        SimpleSampler s = mgr.getSimpleSampler(taskId);
        if (s == null) {
            logger.warn("[sample/stop] ❌ 采样器不存在: ID=%d, 当前运行中的IDs=%s",
                    taskId, mgr.getSimpleSamplers().keySet());
            outln(Text.zhEn("错误: 采样器不存在 (ID: %d)", "Error: sampler not found (ID: %d)").format(taskId), Colors.RED);
            outln(PerformanceTexts.HINT_VIEW_TASKS.text(), Colors.GRAY);
            return null;
        }

        if (!s.isRunning()) {
            logger.warn("[sample/stop] 采样器已停止: ID=%d", taskId);
            outln(Text.zhEn("警告: 该采样器已经停止过", "Warning: this sampler has already been stopped").text(), Colors.YELLOW);
        }

        s.stop();
        long totalSamples = s.getTotalSamples();
        long duration = System.nanoTime() - s.getStartTime();

        logger.info("[sample/stop] 采样器已停止: ID=%d, 总采样=%d, 持续=%s",
                taskId, totalSamples, formatDurationNs(duration));

        SimpleSampleData data = new SimpleSampleData(taskId, s.getSampleRate(), s.getStartTime(), s.getStopTime(),
                (int) totalSamples, s.getReport());
        mgr.addSimpleSampleData(taskId, data);

        logger.debug("[sample/stop] 数据已存储: ID=%d, 方法数=%d", taskId, data.methodCounts().size());

        SampleResult r = new SampleResult();
        r.setTaskId(taskId);
        r.setSampleRate(s.getSampleRate());
        r.setStatus("stopped");
        r.setTotalSamples((int) totalSamples);
        r.setDuration(data.getDuration());
        r.setDurationStr(data.getDurationString());

        outln(PerformanceTexts.SAMPLER_STOPPED.text(), Colors.YELLOW);
        out("ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out(PerformanceTexts.LABEL_TOTAL_SAMPLE_COUNT.text(), Colors.CYAN); outln(String.valueOf(totalSamples), Colors.YELLOW);
        out(PerformanceTexts.LABEL_DURATION.text(), Colors.CYAN); outln(data.getDurationString(), Colors.YELLOW);
        out(PerformanceTexts.LABEL_CAPTURED_METHODS.text(), Colors.CYAN); outln(String.valueOf(data.methodCounts().size()), Colors.YELLOW);
        return r;
    }

    private SampleResult handleReport(SampleReportRequest req) {
        Integer taskId = req.getTaskId();

        logger.info("[sample/report] 查询报告: ID=%s", taskId != null ? String.valueOf(taskId) : "(最新)");

        if (taskId == null) {
            logger.warn("[sample/report] 未指定ID，尝试查找最新数据");
            outln(PerformanceTexts.NO_ID_SEARCH_LATEST_SAMPLE.text(), Colors.GRAY);
        }

        PerfTaskManager mgr = getTaskManager();

        if (taskId == null) {
            Integer latestId = findLatestId(mgr.getSimpleSampleDataMap());
            if (latestId == null) {
                logger.warn("[sample/report] ❌ 无任何已完成数据");
                outln(PerformanceTexts.ERR_NO_COMPLETED_SAMPLE_DATA.text(), Colors.RED);
                outln(Text.zhEn("提示: 先用 'performance sample start' 开始采样，再用 'performance sample stop <ID>' 停止",
                        "Hint: run 'performance sample start' to start sampling, then 'performance sample stop <ID>' to stop").text(), Colors.GRAY);
                return null;
            }
            taskId = latestId;
            logger.info("[sample/report] 自动选择最新ID: %d", taskId);
            out(PerformanceTexts.USING_LATEST_ID.text(), Colors.GRAY); outln(String.valueOf(taskId), Colors.YELLOW);
        }

        SimpleSampleData data = mgr.getSimpleSampleData(taskId);

        if (data == null) {
            Map<Integer, ?> availableIds = mgr.getSimpleSampleDataMap();
            logger.warn("[sample/report] ❌ 数据不存在: ID=%d, 可用IDs=%s", taskId, availableIds.keySet());
            outln(PerformanceTexts.ERR_DATA_NOT_FOUND.format(taskId), Colors.RED);
            if (!availableIds.isEmpty()) {
                outln(PerformanceTexts.AVAILABLE_REPORT_IDS.format(availableIds.keySet()), Colors.GRAY);
            } else {
                outln(Text.zhEn("提示: 没有任何已完成的采样。请先执行 'performance sample stop <ID>'",
                        "Hint: there is no completed sample data. Run 'performance sample stop <ID>' first").text(), Colors.GRAY);
            }
            return null;
        }

        if (data.methodCounts().isEmpty()) {
            logger.warn("[sample/report] ⚠️ 数据为空: ID=%d (采样期间无方法调用被捕获)", taskId);
            outln(PerformanceTexts.WARN_REPORT_DATA_EMPTY.format(taskId), Colors.YELLOW);
            outln(PerformanceTexts.NO_METHOD_CALLS_CAPTURED.text(), Colors.GRAY);
            out(PerformanceTexts.LABEL_SAMPLE_RATE.text(), Colors.CYAN); outln(data.sampleRate() + " Hz", Colors.WHITE);
            out(PerformanceTexts.LABEL_DURATION.text(), Colors.CYAN); outln(data.getDurationString(), Colors.WHITE);
            return null;
        }

        logger.info("[sample/report] ✅ 找到数据: ID=%d, 方法数=%d, 总采样=%d",
                taskId, data.methodCounts().size(), data.totalSamples());

        SampleResult r = new SampleResult();
        r.setTaskId(taskId);
        r.setSampleRate(data.sampleRate());
        r.setTotalSamples(data.totalSamples());
        r.setDuration(data.getDuration());
        r.setDurationStr(data.getDurationString());

        outln("", Colors.DEFAULT);
        outln(Text.zhEn("=== 单线程采样报告 ===", "=== Single-threaded sampling report ===").text(), Colors.CYAN);
        out(PerformanceTexts.LABEL_TASK_ID.text(), Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out(PerformanceTexts.LABEL_SAMPLE_RATE.text(), Colors.CYAN); outln(data.sampleRate() + " Hz", Colors.WHITE);
        out(PerformanceTexts.LABEL_TOTAL_SAMPLES.text(), Colors.CYAN); outln(String.valueOf(data.totalSamples()), Colors.WHITE);
        out(PerformanceTexts.LABEL_DURATION.text(), Colors.CYAN); outln(data.getDurationString(), Colors.WHITE);
        out(PerformanceTexts.HOT_METHODS_TOP.format(Math.min(data.methodCounts().size(), 20)), Colors.CYAN);
        outln("", Colors.DEFAULT);

        ArrayList<SampleResult.MethodEntry> entries = new ArrayList<>();
        long totalCountSum = 0;
        for (Integer count : data.methodCounts().values()) {
            totalCountSum += count;
        }
        int index = 0;
        for (Map.Entry<String, Integer> e : data.methodCounts()
                .entrySet()
                .stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .collect(Collectors.toList())) {
            index++;
            double pct = totalCountSum > 0 ? e.getValue() * 100.0 / totalCountSum : 0;
            byte color = heatColor(pct, index);
            outln(String.format(
                    Locale.getDefault(), "[%4d]  %-80s %6d (%5.1f%%)",
                    index, e.getKey(), e.getValue(), pct), color);
            entries.add(new SampleResult.MethodEntry(e.getKey(), e.getValue(), pct));
        }
        r.setHotMethods(entries);

        logger.debug("[sample/report] 报告生成完毕: %d 条方法记录", entries.size());
        return r;
    }

    private SampleResult handleExport(SampleExportRequest req) throws JSONException {
        int taskId = req.getTaskId();
        String filePath = req.getFilePath();

        logger.info("[sample/export] 导出数据: ID=%d, path=%s", taskId, filePath);

        PerfTaskManager mgr = getTaskManager();
        SimpleSampleData data = mgr.getSimpleSampleData(taskId);
        if (data == null) {
            logger.warn("[sample/export] ❌ 数据不存在: ID=%d", taskId);
            outln(PerformanceTexts.ERR_DATA_NOT_FOUND.format(taskId), Colors.RED);
            return null;
        }

        JSONObject json = new JSONObject();
        json.put("id", data.id()).put("sampleRate", data.sampleRate())
           .put("totalSamples", data.totalSamples()).put("duration", data.getDuration());
        JSONObject mc = new JSONObject();
        for (Map.Entry<String, Integer> e : data.methodCounts().entrySet()) {
            mc.put(e.getKey(), e.getValue());
        }
        json.put("methodCounts", mc);

        if (!writeToFile(filePath, json.toString(2))) {
            logger.error("[sample/export] ❌ 写入文件失败: %s", filePath);
            outln(PerformanceTexts.ERR_EXPORT_WRITE_FAILED.text(), Colors.RED);
            return null;
        }

        logger.info("[sample/export] ✅ 导出成功: %s (%d bytes approx)", filePath,
                json.toString().length());
        SampleResult r = new SampleResult();
        r.setTaskId(taskId);
        r.setStatus("exported");
        r.setExportPath(filePath);
        outln(PerformanceTexts.DATA_EXPORTED.text(), Colors.GREEN);
        out(PerformanceTexts.LABEL_PATH.text(), Colors.CYAN); outln(filePath, Colors.YELLOW);
        out(PerformanceTexts.LABEL_METHOD_COUNT.text(), Colors.CYAN); outln(String.valueOf(data.methodCounts().size()), Colors.WHITE);
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
