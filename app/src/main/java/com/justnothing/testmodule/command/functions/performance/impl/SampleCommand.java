package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.functions.performance.PerfTaskManager;
import com.justnothing.testmodule.command.functions.performance.request.SampleRequest;
import com.justnothing.testmodule.command.functions.performance.response.SampleResult;
import com.justnothing.testmodule.command.functions.performance.sampler.SimpleSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.SimpleSampler;
import com.justnothing.testmodule.command.output.Colors;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class SampleCommand extends AbstractPerfCommand<SampleRequest, SampleResult> {

    public SampleCommand() {
        super("performance sample", SampleRequest.class, SampleResult.class);
    }

    @Override
    protected SampleResult executePerfCommand(SampleRequest request) throws Exception {
        String action = request.getAction();
        if (action == null || action.isEmpty()) {
            outln("错误: 参数不足", Colors.RED);
            return null;
        }
        return switch (action) {
            case "start" -> handleStart(request);
            case "stop" -> handleStop(request);
            case "report" -> handleReport(request);
            case "export" -> handleExport(request);
            default -> {
                outln("未知: " + action, Colors.RED);
                yield null;
            }
        };
    }

    private SampleResult handleStart(SampleRequest req) {
        int rate = req.getSampleRate() != null ? req.getSampleRate() : 100;
        if (rate <= 0) {
            outln("错误: 频率必须 > 0", Colors.RED);
            return null;
        }
        if (rate > 10000)
            outln("警告: 频率过高", Colors.YELLOW);

        PerfTaskManager mgr = getTaskManager();
        int id = mgr.addSimpleSampler(new SimpleSampler(rate));
        mgr.getSimpleSampler(id).start();

        logger.info("采样器已启动 (ID: " + id + ", 频率: " + rate + " Hz)");
        SampleResult r = new SampleResult();
        r.setTaskId(id);
        r.setSampleRate(rate);
        r.setStatus("running");
        outln("采样器已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("频率: ", Colors.CYAN);
        outln(rate + " Hz", Colors.YELLOW);
        return r;
    }

    private SampleResult handleStop(SampleRequest req) {
        if (req.getTaskId() == null) {
            outln("错误: 需要 ID", Colors.RED);
            return null;
        }
        int id = req.getTaskId();
        PerfTaskManager mgr = getTaskManager();
        SimpleSampler s = mgr.getSimpleSampler(id);
        if (s == null) {
            outln("不存在 (ID: " + id + ")", Colors.RED);
            return null;
        }

        s.stop();
        SimpleSampleData data = new SimpleSampleData(id, s.getSampleRate(), s.getStartTime(), s.getStopTime(),
                s.getTotalSamples(), s.getReport());
        mgr.addSimpleSampleData(id, data);

        SampleResult r = new SampleResult();
        r.setTaskId(id);
        r.setSampleRate(s.getSampleRate());
        r.setStatus("stopped");
        r.setTotalSamples(s.getTotalSamples());
        r.setDuration(data.getDuration());
        r.setDurationStr(data.getDurationString());
        outln("已停止", Colors.YELLOW);
        out("ID/次数/持续: ", Colors.CYAN);
        outln(id + "/" + s.getTotalSamples() + "/" + data.getDurationString(), Colors.YELLOW);
        return r;
    }

    private SampleResult handleReport(SampleRequest req) {
        if (req.getTaskId() == null) {
            outln("错误: 需要 ID", Colors.RED);
            return null;
        }
        SimpleSampleData data = getTaskManager().getSimpleSampleData(req.getTaskId());
        if (data == null || data.methodCounts().isEmpty()) {
            outln("不存在或无数据", Colors.RED);
            return null;
        }

        SampleResult r = new SampleResult();
        r.setTaskId(req.getTaskId());
        r.setSampleRate(data.sampleRate());
        r.setTotalSamples(data.totalSamples());
        r.setDuration(data.getDuration());
        r.setDurationStr(data.getDurationString());

        outln("=== 采样报告 ===", Colors.CYAN);
        ArrayList<SampleResult.MethodEntry> entries = new ArrayList<>();
        data.methodCounts()
            .entrySet()
            .stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .forEach(e -> {
                double pct = e.getValue() * 100.0 / data.totalSamples();
                outln(String.format(Locale.getDefault(), "  %-60s %6d (%5.1f%%)", e.getKey(), e.getValue(), pct), Colors.GRAY);
                entries.add(new SampleResult.MethodEntry(e.getKey(), e.getValue(), pct));
            });
        r.setHotMethods(entries);
        return r;
    }

    private SampleResult handleExport(SampleRequest req) throws JSONException {
        if (req.getTaskId() == null || req.getFilePath() == null || req.getFilePath().isEmpty()) {
            outln("用法: performance sample export <id> <file>", Colors.GRAY);
            return null;
        }
        SimpleSampleData data = getTaskManager().getSimpleSampleData(req.getTaskId());
        if (data == null) {
            outln("不存在", Colors.RED);
            return null;
        }

        JSONObject json = new JSONObject();
        json.put("id", data.id()).put("sampleRate", data.sampleRate()).put("totalSamples", data.totalSamples())
                .put("duration", data.getDuration());
        JSONObject mc = new JSONObject();
        for (Map.Entry<String, Integer> e : data.methodCounts().entrySet()) {
            try {
                mc.put(e.getKey(), e.getValue());
            } catch (JSONException ignored) {
            }
        }
        json.put("methodCounts", mc);

        if (!writeToFile(req.getFilePath(), json.toString(2))) {
            outln("导出失败", Colors.RED);
            return null;
        }
        SampleResult r = new SampleResult();
        r.setTaskId(req.getTaskId());
        r.setStatus("exported");
        r.setExportPath(req.getFilePath());
        outln("已导出", Colors.GREEN);
        return r;
    }
}
