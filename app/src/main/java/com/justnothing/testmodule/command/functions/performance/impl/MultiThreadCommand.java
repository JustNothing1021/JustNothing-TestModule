package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.functions.performance.PerfTaskManager;
import com.justnothing.testmodule.command.functions.performance.request.*;
import com.justnothing.testmodule.command.functions.performance.response.MultiThreadResult;
import com.justnothing.testmodule.command.functions.performance.sampler.MultiThreadSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.MultiThreadSampler;
import com.justnothing.testmodule.command.output.Colors;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@SubCommandInfo(
    description = "多线程采样分析，分别统计各线程的方法调用情况",
    usage = "performance multithread <action> [args...]",
    examples = {
        "performance multithread start 1000",
        "performance multithread stop 1",
        "performance multithread report 1",
        "performance multithread export 1 /sdcard/mt.json"
    },
    optionsDesc = """
        Actions:
            start [rate] [--exclude pattern]   开始多线程采样
            stop <id>                          停止采样
            report [id]                        查看报告 (默认最新)
            export <id> <path>                 导出数据"""
)
public class MultiThreadCommand extends AbstractPerfCommand<PerformanceRequest, MultiThreadResult> {

    public MultiThreadCommand() {
        super("performance multithread", PerformanceRequest.class, MultiThreadResult.class);
    }

    @Override
    protected MultiThreadResult executePerfCommand(PerformanceRequest req) throws Exception {
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
            outln("未知请求类型", Colors.RED);
            return null;
        }
    }

    private MultiThreadResult handleStart(MultiThreadStartRequest req) {
        int rate = req.getRate();

        logger.info("[mt/start] 开始多线程采样: rate=%d Hz", rate);

        if (rate <= 0) {
            logger.warn("[mt/start] 频率无效: %d", rate);
            outln("错误: 频率必须 > 0", Colors.RED);
            return null;
        }
        if (rate > 10000) {
            logger.warn("[mt/start] 频率过高: %d Hz，可能影响性能", rate);
            outln("警告: 频率过高", Colors.YELLOW);
        }

        PerfTaskManager mgr = getTaskManager();
        int id = mgr.addMultiThreadSampler(new MultiThreadSampler(rate));
        mgr.getMultiThreadSampler(id).start();

        logger.info("[mt/start] ✅ 多线程采样器已启动: ID=%d, rate=%dHz", id, rate);

        MultiThreadResult r = new MultiThreadResult();
        r.setTaskId(id);
        r.setSampleRate(rate);
        r.setStatus("running");
        outln("多线程采样器已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("频率: ", Colors.CYAN);
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
            outln("错误: 多线程采样器不存在 (ID: " + taskId + ")", Colors.RED);
            outln("可用的IDs: " + mgr.getMultiThreadSamplers().keySet(), Colors.GRAY);
            outln("提示: 先用 'performance multithread start' 启动，再用 'performance multithread stop <ID>' 停止", Colors.GRAY);
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
        outln("采样器已停止", Colors.YELLOW);
        out("ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out("总采样次数: ", Colors.CYAN); outln(String.valueOf(totalSamples), Colors.YELLOW);
        out("持续时间: ", Colors.CYAN); outln(formatDurationNs(duration), Colors.YELLOW);
        out("检测线程数: ", Colors.CYAN); outln(String.valueOf(s.getThreadCount()), Colors.YELLOW);
        return r;
    }

    private MultiThreadResult handleReport(MultiThreadReportRequest req) {
        Integer taskId = req.getTaskId();

        logger.info("[mt/report] 查询报告: ID=%s", taskId != null ? String.valueOf(taskId) : "(最新)");

        if (taskId == null) {
            logger.warn("[mt/report] 未指定ID，尝试查找最新数据");
            outln("未指定ID，查找最新完成的采样...", Colors.GRAY);
        }

        PerfTaskManager mgr = getTaskManager();

        if (taskId == null) {
            Integer latestId = findLatestId(mgr.getMultiThreadSampleDataMap());
            if (latestId == null) {
                logger.warn("[mt/report] ❌ 无任何已完成数据");
                outln("错误: 没有已完成的采样数据", Colors.RED);
                outln("提示: 先用 'performance multithread start' 开始采样，再用 'performance multithread stop <ID>' 停止", Colors.GRAY);
                return null;
            }
            taskId = latestId;
            logger.info("[mt/report] 自动选择最新ID: %d", taskId);
            out("使用最新ID: ", Colors.GRAY); outln(String.valueOf(taskId), Colors.YELLOW);
        }

        MultiThreadSampleData d = mgr.getMultiThreadSampleData(taskId);

        if (d == null) {
            Map<Integer, ?> availableIds = mgr.getMultiThreadSampleDataMap();
            logger.warn("[mt/report] ❌ 数据不存在: ID=%d, 可用IDs=%s", taskId, availableIds.keySet());
            outln("错误: 数据不存在 (ID: " + taskId + ")", Colors.RED);
            if (!availableIds.isEmpty()) {
                outln("可用的报告ID: " + availableIds.keySet(), Colors.GRAY);
            } else {
                outln("提示: 没有任何已完成的采样。请先执行 'performance multithread stop <ID>'", Colors.GRAY);
            }
            return null;
        }

        if (d.threadMethodCounts().isEmpty()) {
            logger.warn("[mt/report] ⚠️ 数据为空: ID=%d (采样期间无方法调用被捕获)", taskId);
            outln("警告: 报告数据为空 (ID: " + taskId + ")", Colors.YELLOW);
            outln("该采样周期内没有捕获到任何方法调用", Colors.GRAY);
            out("采样率: ", Colors.CYAN); outln(d.sampleRate() + " Hz", Colors.WHITE);
            out("持续时间: ", Colors.CYAN); outln(formatDurationNs(d.getDuration()), Colors.WHITE);
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
        outln("=== 多线程采样报告 ===", Colors.CYAN);
        out("任务ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out("采样率: ", Colors.CYAN); outln(d.sampleRate() + " Hz", Colors.WHITE);
        out("总采样数: ", Colors.CYAN); outln(String.valueOf(d.totalSamples()), Colors.WHITE);
        out("持续时间: ", Colors.CYAN); outln(formatDurationNs(d.getDuration()), Colors.WHITE);
        out("检测线程数: ", Colors.CYAN); outln(String.valueOf(d.threadCount()), Colors.WHITE);
        outln("", Colors.DEFAULT);

        ArrayList<MultiThreadResult.ThreadEntry> threadEntries = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> te : d.threadMethodCounts().entrySet()) {
            out("线程: ", Colors.CYAN);
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
            outln("错误: 数据不存在 (ID: " + taskId + ")", Colors.RED);
            outln("可用的报告ID: " + mgr.getMultiThreadSampleDataMap().keySet(), Colors.GRAY);
            return null;
        }

        JSONObject json = new JSONObject();
        json.put("id", d.id()).put("sampleRate", d.sampleRate()).put("totalSamples", d.totalSamples())
                .put("threadCount", d.threadCount()).put("duration", d.getDuration());

        if (!writeToFile(filePath, json.toString(2))) {
            logger.error("[mt/export] ❌ 写入文件失败: %s", filePath);
            outln("导出失败: 无法写入文件", Colors.RED);
            return null;
        }

        logger.info("[mt/export] ✅ 导出成功: %s, 线程数=%d", filePath, d.threadCount());

        MultiThreadResult r = new MultiThreadResult();
        r.setTaskId(taskId);
        r.setStatus("exported");
        r.setExportPath(filePath);
        outln("数据已导出", Colors.GREEN);
        out("路径: ", Colors.CYAN); outln(filePath, Colors.YELLOW);
        out("线程数: ", Colors.CYAN); outln(String.valueOf(d.threadCount()), Colors.WHITE);
        out("总采样: ", Colors.CYAN); outln(String.valueOf(d.totalSamples()), Colors.WHITE);
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
