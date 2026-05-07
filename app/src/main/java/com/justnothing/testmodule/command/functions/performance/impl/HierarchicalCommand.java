package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.functions.performance.request.HierarchicalRequest;
import com.justnothing.testmodule.command.functions.performance.response.HierarchicalResult;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampler;
import com.justnothing.testmodule.command.output.Colors;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class HierarchicalCommand extends AbstractPerfCommand<HierarchicalRequest, HierarchicalResult> {

    public HierarchicalCommand() {
        super("performance hierarchical", HierarchicalRequest.class, HierarchicalResult.class);
    }

    @Override
    protected HierarchicalResult executePerfCommand(HierarchicalRequest req) throws Exception {
        String action = req.getAction();
        if (action == null || action.isEmpty()) {
            outln("参数不足", Colors.RED);
            return null;
        }
        return switch (action) {
            case "start" -> handleStart(req);
            case "stop" -> handleStop(req);
            case "report" -> handleReport(req);
            case "export" -> handleExport(req);
            default -> {
                outln("未知", Colors.RED);
                yield null;
            }
        };
    }

    private HierarchicalResult handleStart(HierarchicalRequest req) {
        int rate = req.getSampleRate() != null ? req.getSampleRate() : 100;
        if (rate <= 0) {
            outln("频率必须>0", Colors.RED);
            return null;
        }
        int id = getTaskManager().addHierarchicalSampler(new HierarchicalSampler(rate));
        getTaskManager().getHierarchicalSampler(id).start();
        HierarchicalResult r = new HierarchicalResult();
        r.setTaskId(id);
        r.setSampleRate(rate);
        r.setStatus("running");
        outln("分层采样器已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        return r;
    }

    private HierarchicalResult handleStop(HierarchicalRequest req) {
        if (req.getTaskId() == null) {
            outln("需要ID", Colors.RED);
            return null;
        }
        HierarchicalSampler s = getTaskManager().getHierarchicalSampler(req.getTaskId());
        if (s == null) {
            outln("不存在", Colors.RED);
            return null;
        }
        s.stop();
        HierarchicalSampleData d = new HierarchicalSampleData(req.getTaskId(), s.getSampleRate(), s.getStartTime(),
                s.getStopTime(), s.getTotalSamples(), s.getReport(), s.getCallerCounts(), s.getMethodCount());
        getTaskManager().addHierarchicalSampleData(req.getTaskId(), d);
        HierarchicalResult r = new HierarchicalResult();
        r.setTaskId(req.getTaskId());
        r.setSampleRate(s.getSampleRate());
        r.setStatus("stopped");
        r.setTotalSamples(s.getTotalSamples());
        r.setMethodCount(s.getMethodCount());
        outln("已停止", Colors.YELLOW);
        return r;
    }

    private HierarchicalResult handleReport(HierarchicalRequest req) {
        if (req.getTaskId() == null) {
            outln("需要ID", Colors.RED);
            return null;
        }
        HierarchicalSampleData d = getTaskManager().getHierarchicalSampleData(req.getTaskId());
        if (d == null) {
            outln("不存在", Colors.RED);
            return null;
        }
        HierarchicalResult r = new HierarchicalResult();
        r.setTaskId(req.getTaskId());
        r.setSampleRate(d.sampleRate());
        r.setTotalSamples(d.totalSamples());
        r.setMethodCount(d.methodCount());
        ArrayList<HierarchicalResult.MethodCallEntry> entries = new ArrayList<>();
        d.methodCallInfos().entrySet().stream()
                .sorted((a, b) -> b.getValue().getSampleCount() - a.getValue().getSampleCount()).limit(20)
                .forEach(entry -> {
                    var info = entry.getValue();
                    double pct = info.getSampleCount() * 100.0 / d.totalSamples();
                    outln(String.format(Locale.getDefault(),
                            "  %-60s %6d (%5.1f%%) depth:%.1f",
                            info.methodKey, info.getSampleCount(), pct, info.getAverageDepth()), Colors.GRAY);
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
                });
        r.setHotMethods(entries);
        return r;
    }

    private HierarchicalResult handleExport(HierarchicalRequest req) throws JSONException {
        if (req.getTaskId() == null || req.getFilePath() == null) {
            outln("参数不足", Colors.RED);
            return null;
        }
        String fp = req.getFilePath();
        JSONObject j = new JSONObject();
        j.put("id", req.getTaskId());
        if (!writeToFile(fp, j.toString(2))) {
            outln("失败", Colors.RED);
            return null;
        }
        HierarchicalResult r = new HierarchicalResult();
        r.setTaskId(req.getTaskId());
        r.setStatus("exported");
        r.setExportPath(fp);
        outln("已导出", Colors.GREEN);
        return r;
    }
}
