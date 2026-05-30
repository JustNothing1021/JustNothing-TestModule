package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.functions.performance.PerfTaskManager;
import com.justnothing.testmodule.command.functions.performance.request.*;
import com.justnothing.testmodule.command.functions.performance.response.HierarchicalResult;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampler;
import com.justnothing.testmodule.command.output.Colors;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@SubCommandInfo(
    description = "分层采样分析，按调用层次展示方法耗时分布",
    usage = "performance hierarchical <action> [args...]",
    examples = {
        "performance hierarchical start 1000",
        "performance hierarchical stop 1",
        "performance hierarchical report 1",
        "performance hierarchical export 1 /sdcard/hier.json"
    },
    optionsDesc = """
        Actions:
            start [rate] [--exclude pattern]   开始分层采样
            stop <id>                          停止采样
            report [id]                        查看报告 (默认最新)
            export <id> <path>                 导出数据"""
)
public class HierarchicalCommand extends AbstractPerfCommand<PerformanceRequest, HierarchicalResult> {

    public HierarchicalCommand() {
        super("performance hierarchical", PerformanceRequest.class, HierarchicalResult.class);
    }

    @Override
    protected HierarchicalResult executePerfCommand(PerformanceRequest req) throws Exception {
        logger.debug("[hier] 收到请求: %s", req.getClass().getSimpleName());

        if (req instanceof HierarchicalStartRequest startReq) {
            return handleStart(startReq);
        } else if (req instanceof HierarchicalStopRequest stopReq) {
            return handleStop(stopReq);
        } else if (req instanceof HierarchicalReportRequest reportReq) {
            return handleReport(reportReq);
        } else if (req instanceof HierarchicalExportRequest exportReq) {
            return handleExport(exportReq);
        } else {
            logger.warn("[hier] 未知请求类型: %s", req.getClass().getName());
            outln("未知请求类型", Colors.RED);
            return null;
        }
    }

    private HierarchicalResult handleStart(HierarchicalStartRequest req) {
        int rate = req.getRate();

        logger.info("[hier/start] 开始分层采样: rate=%d Hz", rate);

        if (rate <= 0) {
            logger.warn("[hier/start] 频率无效: %d", rate);
            outln("错误: 频率必须 > 0", Colors.RED);
            return null;
        }
        if (rate > 10000) {
            logger.warn("[hier/start] 频率过高: %d Hz，可能影响性能", rate);
            outln("警告: 频率过高", Colors.YELLOW);
        }

        PerfTaskManager mgr = getTaskManager();
        int id = mgr.addHierarchicalSampler(new HierarchicalSampler(rate));
        mgr.getHierarchicalSampler(id).start();

        logger.info("[hier/start] ✅ 分层采样器已启动: ID=%d, rate=%dHz", id, rate);

        HierarchicalResult r = new HierarchicalResult();
        r.setTaskId(id);
        r.setSampleRate(rate);
        r.setStatus("running");
        outln("分层采样器已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("频率: ", Colors.CYAN);
        outln(rate + " Hz", Colors.YELLOW);
        return r;
    }

    private HierarchicalResult handleStop(HierarchicalStopRequest req) {
        int taskId = req.getTaskId();
        PerfTaskManager mgr = getTaskManager();

        logger.info("[hier/stop] 停止分层采样: ID=%d", taskId);

        HierarchicalSampler s = mgr.getHierarchicalSampler(taskId);
        if (s == null) {
            logger.warn("[hier/stop] ❌ 采样器不存在: ID=%d, 当前运行中的IDs=%s",
                    taskId, mgr.getHierarchicalSamplers().keySet());
            outln("错误: 分层采样器不存在 (ID: " + taskId + ")", Colors.RED);
            outln("可用的IDs: " + mgr.getHierarchicalSamplers().keySet(), Colors.GRAY);
            outln("提示: 先用 'performance hierarchical start' 启动，再用 'performance hierarchical stop <ID>' 停止", Colors.GRAY);
            return null;
        }

        s.stop();
        long totalSamples = s.getTotalSamples();
        long duration = System.nanoTime() - s.getStartTime();

        logger.info("[hier/stop] 采样器已停止: ID=%d, 总采样=%d, 持续=%s, 方法数=%d",
                taskId, totalSamples, formatDurationNs(duration), s.getMethodCount());

        HierarchicalSampleData d = new HierarchicalSampleData(taskId, s.getSampleRate(), s.getStartTime(),
                s.getStopTime(), s.getTotalSamples(), s.getReport(), s.getCallerCounts(), s.getMethodCount());
        mgr.addHierarchicalSampleData(taskId, d);

        logger.debug("[hier/stop] 数据已存储: ID=%d, 方法数=%d", taskId, d.methodCount());

        HierarchicalResult r = new HierarchicalResult();
        r.setTaskId(taskId);
        r.setSampleRate(s.getSampleRate());
        r.setStatus("stopped");
        r.setTotalSamples(s.getTotalSamples());
        r.setMethodCount(s.getMethodCount());
        outln("采样器已停止", Colors.YELLOW);
        out("ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out("总采样次数: ", Colors.CYAN); outln(String.valueOf(totalSamples), Colors.YELLOW);
        out("持续时间: ", Colors.CYAN); outln(formatDurationNs(duration), Colors.YELLOW);
        out("捕获方法数: ", Colors.CYAN); outln(String.valueOf(s.getMethodCount()), Colors.YELLOW);
        return r;
    }

    private HierarchicalResult handleReport(HierarchicalReportRequest req) {
        Integer taskId = req.getTaskId();

        logger.info("[hier/report] 查询报告: ID=%s", taskId != null ? String.valueOf(taskId) : "(最新)");

        if (taskId == null) {
            logger.warn("[hier/report] 未指定ID，尝试查找最新数据");
            outln("未指定ID，查找最新完成的采样...", Colors.GRAY);
        }

        PerfTaskManager mgr = getTaskManager();

        if (taskId == null) {
            Integer latestId = findLatestId(mgr.getHierarchicalSampleDataMap());
            if (latestId == null) {
                logger.warn("[hier/report] ❌ 无任何已完成数据");
                outln("错误: 没有已完成的采样数据", Colors.RED);
                outln("提示: 先用 'performance hierarchical start' 开始采样，再用 'performance hierarchical stop <ID>' 停止", Colors.GRAY);
                return null;
            }
            taskId = latestId;
            logger.info("[hier/report] 自动选择最新ID: %d", taskId);
            out("使用最新ID: ", Colors.GRAY); outln(String.valueOf(taskId), Colors.YELLOW);
        }

        HierarchicalSampleData d = mgr.getHierarchicalSampleData(taskId);

        if (d == null) {
            Map<Integer, ?> availableIds = mgr.getHierarchicalSampleDataMap();
            logger.warn("[hier/report] ❌ 数据不存在: ID=%d, 可用IDs=%s", taskId, availableIds.keySet());
            outln("错误: 数据不存在 (ID: " + taskId + ")", Colors.RED);
            if (!availableIds.isEmpty()) {
                outln("可用的报告ID: " + availableIds.keySet(), Colors.GRAY);
            } else {
                outln("提示: 没有任何已完成的采样。请先执行 'performance hierarchical stop <ID>'", Colors.GRAY);
            }
            return null;
        }

        if (d.methodCallInfos().isEmpty()) {
            logger.warn("[hier/report] ⚠️ 数据为空: ID=%d (采样期间无方法调用被捕获)", taskId);
            outln("警告: 报告数据为空 (ID: " + taskId + ")", Colors.YELLOW);
            outln("该采样周期内没有捕获到任何方法调用", Colors.GRAY);
            out("采样率: ", Colors.CYAN); outln(d.sampleRate() + " Hz", Colors.WHITE);
            out("持续时间: ", Colors.CYAN); outln(formatDurationNs(d.getDuration()), Colors.WHITE);
            return null;
        }

        logger.info("[hier/report] ✅ 找到数据: ID=%d, 方法数=%d, 总采样=%d",
                taskId, d.methodCallInfos().size(), d.totalSamples());

        HierarchicalResult r = new HierarchicalResult();
        r.setTaskId(taskId);
        r.setSampleRate(d.sampleRate());
        r.setTotalSamples(d.totalSamples());
        r.setMethodCount(d.methodCount());

        outln("", Colors.DEFAULT);
        outln("=== 分层采样报告 ===", Colors.CYAN);
        out("任务ID: ", Colors.CYAN); outln(String.valueOf(taskId), Colors.YELLOW);
        out("采样率: ", Colors.CYAN); outln(d.sampleRate() + " Hz", Colors.WHITE);
        out("总采样数: ", Colors.CYAN); outln(String.valueOf(d.totalSamples()), Colors.WHITE);
        out("持续时间: ", Colors.CYAN); outln(formatDurationNs(d.getDuration()), Colors.WHITE);
        out("热点方法 TOP-" + Math.min(d.methodCallInfos().size(), 20) + ":", Colors.CYAN);
        outln("", Colors.DEFAULT);

        ArrayList<HierarchicalResult.MethodCallEntry> entries = new ArrayList<>();
        long totalCountSum = 0;
        for (var info : d.methodCallInfos().values()) {
            totalCountSum += info.getSampleCount();
        }
        int index = 0;
        for (var entry : d.methodCallInfos().entrySet().stream()
                .sorted((a, b) -> b.getValue().getSampleCount() - a.getValue().getSampleCount()).limit(20)
                .collect(Collectors.toList())) {
                    var info = entry.getValue();
                    double pct = totalCountSum > 0 ? info.getSampleCount() * 100.0 / totalCountSum : 0;
                    byte color = heatColor(pct, index);
                    outln(String.format(Locale.getDefault(),
                            "  %-60s %6d (%5.1f%%) depth:%.1f",
                            info.methodKey, info.getSampleCount(), pct, info.getAverageDepth()), color);
                    var mce = new HierarchicalResult.MethodCallEntry();
                    mce.setMethodKey(info.methodKey);
                    mce.setSampleCount(info.getSampleCount());
                    mce.setPercentage(pct);
                    mce.setAvgDepth(info.getAverageDepth());
                    ArrayList<HierarchicalResult.MethodCallEntry.CallerEntry> callers = new ArrayList<>();
                    info.getCallers()
                            .entrySet()
                            .stream()
                            .sorted((a, b) -> b.getValue() - a.getValue())
                            .limit(5)
                            .collect(Collectors.toList())
                            .forEach(c -> {
                                outln(String.format(Locale.getDefault(),
                                    "      %-60s %6d (%5.1f%%)",
                                        c.getKey(), c.getValue(), c.getValue() * 100.0 / info.getSampleCount()),
                                        Colors.GRAY);
                                var ce = new HierarchicalResult.MethodCallEntry.CallerEntry();
                                ce.setCallerName(c.getKey());
                                ce.setCount(c.getValue());
                                ce.setPercentage(c.getValue() * 100.0 / info.getSampleCount());
                                callers.add(ce);
                            });
                    mce.setCallers(callers);
                    entries.add(mce);
                    index++;
                }
        r.setHotMethods(entries);

        logger.debug("[hier/report] 报告生成完毕: %d 条方法记录", entries.size());
        return r;
    }

    private HierarchicalResult handleExport(HierarchicalExportRequest req) throws JSONException {
        int taskId = req.getTaskId();
        String filePath = req.getFilePath();

        logger.info("[hier/export] 导出数据: ID=%d, path=%s", taskId, filePath);

        PerfTaskManager mgr = getTaskManager();
        HierarchicalSampleData d = mgr.getHierarchicalSampleData(taskId);
        if (d == null) {
            logger.warn("[hier/export] ❌ 数据不存在: ID=%d, 可用IDs=%s", taskId, mgr.getHierarchicalSampleDataMap().keySet());
            outln("错误: 数据不存在 (ID: " + taskId + ")", Colors.RED);
            outln("可用的报告ID: " + mgr.getHierarchicalSampleDataMap().keySet(), Colors.GRAY);
            return null;
        }

        JSONObject j = new JSONObject();
        j.put("id", d.id()).put("sampleRate", d.sampleRate())
         .put("totalSamples", d.totalSamples()).put("methodCount", d.methodCount())
         .put("duration", d.getDuration());

        if (!writeToFile(filePath, j.toString(2))) {
            logger.error("[hier/export] ❌ 写入文件失败: %s", filePath);
            outln("导出失败: 无法写入文件", Colors.RED);
            return null;
        }

        logger.info("[hier/export] ✅ 导出成功: %s, 方法数=%d", filePath, d.methodCount());

        HierarchicalResult r = new HierarchicalResult();
        r.setTaskId(taskId);
        r.setStatus("exported");
        r.setExportPath(filePath);
        outln("数据已导出", Colors.GREEN);
        out("路径: ", Colors.CYAN); outln(filePath, Colors.YELLOW);
        out("方法数: ", Colors.CYAN); outln(String.valueOf(d.methodCount()), Colors.WHITE);
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
