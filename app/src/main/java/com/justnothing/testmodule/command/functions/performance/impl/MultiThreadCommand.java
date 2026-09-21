package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.functions.performance.request.PerformanceRequest;
import com.justnothing.testmodule.command.functions.performance.util.PerfTaskManager;
import com.justnothing.testmodule.command.functions.performance.request.*;
import com.justnothing.testmodule.command.functions.performance.response.MultiThreadResult;
import com.justnothing.testmodule.command.functions.performance.sampler.MultiThreadSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.MultiThreadSampler;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@SubCommandInfo(
    description = PerformanceTexts.SUB_PERFORMANCE_MULTITHREAD_DESC,
    usage = "performance multithread <action> [args...]",
    examples = {
        "performance multithread start 1000",
        "performance multithread stop 1",
        "performance multithread report 1",
        "performance multithread export 1 /sdcard/mt.json"
    },
    optionsDesc = PerformanceTexts.SUB_PERFORMANCE_MULTITHREAD_OPTIONS
)
public class MultiThreadCommand extends AbstractPerfCommand<PerformanceRequest<?>, MultiThreadResult> {

    @SuppressWarnings("unchecked")
    public MultiThreadCommand() {
        super("performance multithread", (Class) PerformanceRequest.class, MultiThreadResult.class);
    }

    @Override
    protected MultiThreadResult executePerfCommand(PerformanceRequest<?> req) throws Exception {
        logger.debug("[mt] 收到请求: %s", req.getClass().getSimpleName());

        if (req instanceof MultiThreadStartRequest startReq) {
            return handleStart(startReq);
        } else if (req instanceof MultiThreadStopRequest stopReq) {
            return handleStop(stopReq);
        } else if (req instanceof MultiThreadReportRequest reportReq) {
            return handleReport(reportReq);
        } else if (req instanceof MultiThreadExportRequest exportReq) {
            return handleExport(exportReq);
        } else {
            logger.warn("[mt] 未知请求类型: %s", req.getClass().getName());
            outln(PerformanceTexts.UNKNOWN_REQUEST_TYPE.text(), Colors.RED);
            return null;
        }
    }

    private MultiThreadResult handleStart(MultiThreadStartRequest req) {
        int rate = req.getRate();

        logger.info("[mt/start] 开始多线程采样: rate=%d Hz", rate);

        if (rate <= 0) {
            logger.warn("[mt/start] 频率无效: %d", rate);
            outln(PerformanceTexts.ERR_RATE_MUST_BE_POSITIVE.text(), Colors.RED);
            return null;
        }
        if (rate > 10000) {
            logger.warn("[mt/start] 频率过高: %d Hz，可能影响性能", rate);
            outln(PerformanceTexts.WARN_RATE_TOO_HIGH.text(), Colors.YELLOW);
        }

        PerfTaskManager mgr = getTaskManager();
        int id = mgr.addMultiThreadSampler(new MultiThreadSampler(rate));
        mgr.getMultiThreadSampler(id).start();

        logger.info("[mt/start] ✅ 多线程采样器已启动: ID=%d, rate=%dHz", id, rate);

        MultiThreadResult r = new MultiThreadResult();
        r.setTaskId(id);
        r.setSampleRate(rate);
        r.setStatus("running");
        outln(Text.zhEn("多线程采样器已启动", "Multi-threaded sampler started").text(), Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out(PerformanceTexts.LABEL_RATE.text(), Colors.CYAN);
        outln(rate + " Hz", Colors.YELLOW);
        return r;
    }

    private MultiThreadResult handleStop(MultiThreadStopRequest req) {
        int taskId = req.getTaskId();
        PerfTaskManager mgr = getTaskManager();

        logger.info("[mt/stop] 停止多线程采样: ID=%d", taskId);

        MultiThreadSampler s = mgr.getMultiThreadSampler(taskId);
        if (s == null) {
            logger.warn("[mt/stop] ❌ 采样器不存在: ID=%d, 当前运行中的IDs=%s",
                    taskId, mgr.getMultiThreadSamplers().keySet());
            outln(Text.zhEn("错误: 多线程采样器不存在 (ID: %d)", "Error: multi-threaded sampler not found (ID: %d)").format(taskId), Colors.RED);
            outln(PerformanceTexts.AVAILABLE_IDS.format(mgr.getMultiThreadSamplers().keySet()), Colors.GRAY);
            outln(Text.zhEn("提示: 先用 'performance multithread start' 启动，再用 'performance multithread stop <ID>' 停止",
                    "Hint: run 'performance multithread start' first, then 'performance multithread stop <ID>' to stop").text(), Colors.GRAY);
            return null;
        }

        s.stop();
        long totalSamples = s.getTotalSamples();
        long duration = System.nanoTime() - s.getStartTime();

        logger.info("[mt/stop] 采样器已停止: ID=%d, 总采样=%d, 持续=%s, 线程数=%d",
                taskId, totalSamples, formatDurationNs(duration), s.getThreadCount());

        MultiThreadSampleData d = new MultiThreadSampleData(taskId, s.getSampleRate(), s.getStartTime(),
                s.getStopTime(), s.getTotalSamples(), s.getReport(), s.getThreadSampleCounts(), s.getThreadCount());
        mgr.addMultiThreadSampleData(taskId, d);

        logger.debug("[mt/stop] 数据已存储: ID=%d, 线程数=%d", taskId, d.threadCount());

        MultiThreadResult r = new MultiThreadResult();
        r.setTaskId(taskId);
        r.setSampleRate(s.getSampleRate());
        r.setStatus("stopped");
        r.setTotalSamples(s.getTotalSamples());
        r.setThreadCount(s.getThreadCount());
        outln(PerformanceTexts.SAMPLER_STOPPED.text(), Colors.YELLOW);
        out("ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out(PerformanceTexts.LABEL_TOTAL_SAMPLE_COUNT.text(), Colors.CYAN); outln(String.valueOf(totalSamples), Colors.YELLOW);
        out(PerformanceTexts.LABEL_DURATION.text(), Colors.CYAN); outln(formatDurationNs(duration), Colors.YELLOW);
        out(PerformanceTexts.LABEL_DETECTED_THREADS.text(), Colors.CYAN); outln(String.valueOf(s.getThreadCount()), Colors.YELLOW);
        return r;
    }

    private MultiThreadResult handleReport(MultiThreadReportRequest req) {
        Integer taskId = req.getTaskId();

        logger.info("[mt/report] 查询报告: ID=%s", taskId != null ? String.valueOf(taskId) : "(最新)");

        if (taskId == null) {
            logger.warn("[mt/report] 未指定ID，尝试查找最新数据");
            outln(PerformanceTexts.NO_ID_SEARCH_LATEST_SAMPLE.text(), Colors.GRAY);
        }

        PerfTaskManager mgr = getTaskManager();

        if (taskId == null) {
            Integer latestId = findLatestId(mgr.getMultiThreadSampleDataMap());
            if (latestId == null) {
                logger.warn("[mt/report] ❌ 无任何已完成数据");
                outln(PerformanceTexts.ERR_NO_COMPLETED_SAMPLE_DATA.text(), Colors.RED);
                outln(Text.zhEn("提示: 先用 'performance multithread start' 开始采样，再用 'performance multithread stop <ID>' 停止",
                        "Hint: run 'performance multithread start' to start sampling, then 'performance multithread stop <ID>' to stop").text(), Colors.GRAY);
                return null;
            }
            taskId = latestId;
            logger.info("[mt/report] 自动选择最新ID: %d", taskId);
            out(PerformanceTexts.USING_LATEST_ID.text(), Colors.GRAY); outln(String.valueOf(taskId), Colors.YELLOW);
        }

        MultiThreadSampleData d = mgr.getMultiThreadSampleData(taskId);

        if (d == null) {
            Map<Integer, ?> availableIds = mgr.getMultiThreadSampleDataMap();
            logger.warn("[mt/report] ❌ 数据不存在: ID=%d, 可用IDs=%s", taskId, availableIds.keySet());
            outln(PerformanceTexts.ERR_DATA_NOT_FOUND.format(taskId), Colors.RED);
            if (!availableIds.isEmpty()) {
                outln(PerformanceTexts.AVAILABLE_REPORT_IDS.format(availableIds.keySet()), Colors.GRAY);
            } else {
                outln(Text.zhEn("提示: 没有任何已完成的采样。请先执行 'performance multithread stop <ID>'",
                        "Hint: there is no completed sample data. Run 'performance multithread stop <ID>' first").text(), Colors.GRAY);
            }
            return null;
        }

        if (d.threadMethodCounts().isEmpty()) {
            logger.warn("[mt/report] ⚠️ 数据为空: ID=%d (采样期间无方法调用被捕获)", taskId);
            outln(PerformanceTexts.WARN_REPORT_DATA_EMPTY.format(taskId), Colors.YELLOW);
            outln(PerformanceTexts.NO_METHOD_CALLS_CAPTURED.text(), Colors.GRAY);
            out(PerformanceTexts.LABEL_SAMPLE_RATE.text(), Colors.CYAN); outln(d.sampleRate() + " Hz", Colors.WHITE);
            out(PerformanceTexts.LABEL_DURATION.text(), Colors.CYAN); outln(formatDurationNs(d.getDuration()), Colors.WHITE);
            return null;
        }

        logger.info("[mt/report] ✅ 找到数据: ID=%d, 线程数=%d, 总采样=%d",
                taskId, d.threadMethodCounts().size(), d.totalSamples());

        MultiThreadResult r = new MultiThreadResult();
        r.setTaskId(taskId);
        r.setSampleRate(d.sampleRate());
        r.setTotalSamples(d.totalSamples());
        r.setThreadCount(d.threadCount());

        outln("", Colors.DEFAULT);
        outln(Text.zhEn("=== 多线程采样报告 ===", "=== Multi-threaded sampling report ===").text(), Colors.CYAN);
        out(PerformanceTexts.LABEL_TASK_ID.text(), Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out(PerformanceTexts.LABEL_SAMPLE_RATE.text(), Colors.CYAN); outln(d.sampleRate() + " Hz", Colors.WHITE);
        out(PerformanceTexts.LABEL_TOTAL_SAMPLES.text(), Colors.CYAN); outln(String.valueOf(d.totalSamples()), Colors.WHITE);
        out(PerformanceTexts.LABEL_DURATION.text(), Colors.CYAN); outln(formatDurationNs(d.getDuration()), Colors.WHITE);
        out(PerformanceTexts.LABEL_DETECTED_THREADS.text(), Colors.CYAN); outln(String.valueOf(d.threadCount()), Colors.WHITE);
        outln("", Colors.DEFAULT);

        ArrayList<MultiThreadResult.ThreadEntry> threadEntries = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> te : d.threadMethodCounts().entrySet()) {
            out(Text.zhEn("线程: ", "Thread: ").text(), Colors.CYAN);
            outln(te.getKey(), Colors.GREEN);
            ArrayList<MultiThreadResult.ThreadEntry.MethodEntry> mes = new ArrayList<>();
            long threadCountSum = 0;
            for (Integer c : te.getValue().values()) threadCountSum += c;
            int mIndex = 0;
            for (var e : te.getValue()
                .entrySet()
                .stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .collect(Collectors.toList())) {
                mIndex++;
                double p = threadCountSum > 0 ? e.getValue() * 100.0 / threadCountSum : 0;
                byte color = heatColor(p, mIndex);
                outln(String.format(Locale.getDefault(),
                        "[%4d]    %-60s %6d (%5.1f%%)",
                        mIndex, e.getKey(), e.getValue(), p), color);
                var me = new MultiThreadResult.ThreadEntry.MethodEntry();
                me.setMethodName(e.getKey());
                me.setCount(e.getValue());
                me.setPercentage(p);
                mes.add(me);
            }
            var te2 = new MultiThreadResult.ThreadEntry();
            te2.setThreadName(te.getKey());
            te2.setSampleCount(Objects.requireNonNullElse(d.threadSampleCounts().get(te.getKey()), 0));
            te2.setMethods(mes);
            threadEntries.add(te2);
        }
        r.setThreadData(threadEntries);

        logger.debug("[mt/report] 报告生成完毕: %d 个线程记录", threadEntries.size());
        return r;
    }

    private MultiThreadResult handleExport(MultiThreadExportRequest req) throws JSONException {
        int taskId = req.getTaskId();
        String filePath = req.getFilePath();

        logger.info("[mt/export] 导出数据: ID=%d, path=%s", taskId, filePath);

        PerfTaskManager mgr = getTaskManager();
        MultiThreadSampleData d = mgr.getMultiThreadSampleData(taskId);
        if (d == null) {
            logger.warn("[mt/export] ❌ 数据不存在: ID=%d, 可用IDs=%s", taskId, mgr.getMultiThreadSampleDataMap().keySet());
            outln(PerformanceTexts.ERR_DATA_NOT_FOUND.format(taskId), Colors.RED);
            outln(PerformanceTexts.AVAILABLE_REPORT_IDS.format(mgr.getMultiThreadSampleDataMap().keySet()), Colors.GRAY);
            return null;
        }

        JSONObject json = new JSONObject();
        json.put("id", d.id()).put("sampleRate", d.sampleRate()).put("totalSamples", d.totalSamples())
                .put("threadCount", d.threadCount()).put("duration", d.getDuration());

        if (!writeToFile(filePath, json.toString(2))) {
            logger.error("[mt/export] ❌ 写入文件失败: %s", filePath);
            outln(PerformanceTexts.ERR_EXPORT_WRITE_FAILED.text(), Colors.RED);
            return null;
        }

        logger.info("[mt/export] ✅ 导出成功: %s, 线程数=%d", filePath, d.threadCount());

        MultiThreadResult r = new MultiThreadResult();
        r.setTaskId(taskId);
        r.setStatus("exported");
        r.setExportPath(filePath);
        outln(PerformanceTexts.DATA_EXPORTED.text(), Colors.GREEN);
        out(PerformanceTexts.LABEL_PATH.text(), Colors.CYAN); outln(filePath, Colors.YELLOW);
        out(PerformanceTexts.LABEL_THREAD_COUNT.text(), Colors.CYAN); outln(String.valueOf(d.threadCount()), Colors.WHITE);
        out(PerformanceTexts.LABEL_TOTAL_SAMPLES_SHORT.text(), Colors.CYAN); outln(String.valueOf(d.totalSamples()), Colors.WHITE);
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
