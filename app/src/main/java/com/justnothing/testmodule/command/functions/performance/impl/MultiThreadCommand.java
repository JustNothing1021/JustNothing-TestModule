package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.functions.performance.request.MultiThreadRequest;
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

public class MultiThreadCommand extends AbstractPerfCommand<MultiThreadRequest, MultiThreadResult> {

    public MultiThreadCommand() {
        super("performance multithread", MultiThreadRequest.class, MultiThreadResult.class);
    }

    @Override
    protected MultiThreadResult executePerfCommand(MultiThreadRequest req) throws Exception {
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

    private MultiThreadResult handleStart(MultiThreadRequest req) {
        int rate = req.getSampleRate() != null ? req.getSampleRate() : 100;
        if (rate <= 0) {
            outln("频率必须>0", Colors.RED);
            return null;
        }
        int id = getTaskManager().addMultiThreadSampler(new MultiThreadSampler(rate));
        getTaskManager().getMultiThreadSampler(id).start();
        logger.info("多线程采样器已启动 (ID: " + id + ")");
        MultiThreadResult r = new MultiThreadResult();
        r.setTaskId(id);
        r.setSampleRate(rate);
        r.setStatus("running");
        outln("已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        return r;
    }

    private MultiThreadResult handleStop(MultiThreadRequest req) {
        if (req.getTaskId() == null) {
            outln("需要ID", Colors.RED);
            return null;
        }
        MultiThreadSampler s = getTaskManager().getMultiThreadSampler(req.getTaskId());
        if (s == null) {
            outln("不存在", Colors.RED);
            return null;
        }
        s.stop();
        MultiThreadSampleData d = new MultiThreadSampleData(req.getTaskId(), s.getSampleRate(), s.getStartTime(),
                s.getStopTime(), s.getTotalSamples(), s.getReport(), s.getThreadSampleCounts(), s.getThreadCount());
        getTaskManager().addMultiThreadSampleData(req.getTaskId(), d);
        MultiThreadResult r = new MultiThreadResult();
        r.setTaskId(req.getTaskId());
        r.setSampleRate(s.getSampleRate());
        r.setStatus("stopped");
        r.setTotalSamples(s.getTotalSamples());
        r.setThreadCount(s.getThreadCount());
        outln("已停止", Colors.YELLOW);
        return r;
    }

    private MultiThreadResult handleReport(MultiThreadRequest req) {
        if (req.getTaskId() == null) {
            outln("需要ID", Colors.RED);
            return null;
        }
        MultiThreadSampleData d = getTaskManager().getMultiThreadSampleData(req.getTaskId());
        if (d == null) {
            outln("不存在", Colors.RED);
            return null;
        }
        MultiThreadResult r = new MultiThreadResult();
        r.setTaskId(req.getTaskId());
        r.setSampleRate(d.sampleRate());
        r.setTotalSamples(d.totalSamples());
        r.setThreadCount(d.threadCount());
        ArrayList<MultiThreadResult.ThreadEntry> threadEntries = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> te : d.threadMethodCounts().entrySet()) {
            out("线程: ", Colors.CYAN);
            outln(te.getKey(), Colors.GREEN);
            ArrayList<MultiThreadResult.ThreadEntry.MethodEntry> mes = new ArrayList<>();
            te.getValue()
                .entrySet()
                .stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10).forEach(e -> {
                double p = e.getValue() * 100.0 / Math.max(Objects.requireNonNullElse(d.threadSampleCounts().get(te.getKey()), 1), 1);
                outln(String.format(Locale.getDefault(), "    %-60s %6d (%5.1f%%)", e.getKey(), e.getValue(), p), Colors.GRAY);
                var me = new MultiThreadResult.ThreadEntry.MethodEntry();
                me.setMethodName(e.getKey());
                me.setCount(e.getValue());
                me.setPercentage(p);
                mes.add(me);
            });
            var te2 = new MultiThreadResult.ThreadEntry();
            te2.setThreadName(te.getKey());
            te2.setSampleCount(Objects.requireNonNullElse(d.threadSampleCounts().get(te.getKey()), 0));
            te2.setMethods(mes);
            threadEntries.add(te2);
        }
        r.setThreadData(threadEntries);
        return r;
    }

    private MultiThreadResult handleExport(MultiThreadRequest req) throws JSONException {
        if (req.getTaskId() == null || req.getFilePath() == null) {
            outln("参数不足", Colors.RED);
            return null;
        }
        MultiThreadSampleData d = getTaskManager().getMultiThreadSampleData(req.getTaskId());
        if (d == null) {
            outln("不存在", Colors.RED);
            return null;
        }
        JSONObject json = new JSONObject();
        json.put("id", d.id()).put("sampleRate", d.sampleRate()).put("totalSamples", d.totalSamples())
                .put("threadCount", d.threadCount());
        if (!writeToFile(req.getFilePath(), json.toString(2))) {
            outln("失败", Colors.RED);
            return null;
        }
        MultiThreadResult r = new MultiThreadResult();
        r.setTaskId(req.getTaskId());
        r.setStatus("exported");
        r.setExportPath(req.getFilePath());
        outln("已导出", Colors.GREEN);
        return r;
    }
}
