package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.functions.performance.request.TraceRequest;
import com.justnothing.testmodule.command.functions.performance.response.TraceResult;
import com.justnothing.testmodule.command.functions.performance.trace.TraceData;
import com.justnothing.testmodule.command.functions.performance.trace.Tracer;
import com.justnothing.testmodule.command.output.Colors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TraceCommand extends AbstractPerfCommand<TraceRequest, TraceResult> {

    public TraceCommand() {
        super("performance trace", TraceRequest.class, TraceResult.class);
    }

    @Override
    protected TraceResult executePerfCommand(TraceRequest req) throws Exception {
        String action = req.getAction();
        if (action == null || action.isEmpty()) {
            outln("参数不足", Colors.RED);
            return null;
        }
        return switch (action) {
            case "start" -> handleStart();
            case "stop" -> handleStop(req);
            case "report" -> handleReport(req);
            case "export" -> handleExport(req);
            default -> {
                outln("未知", Colors.RED);
                yield null;
            }
        };
    }

    private TraceResult handleStart() {
        int id = getTaskManager().addTracer(new Tracer());
        getTaskManager().getTracer(id).start();
        TraceResult r = new TraceResult();
        r.setTaskId(id);
        r.setStatus("running");
        outln("Tracer 已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        return r;
    }

    private TraceResult handleStop(TraceRequest req) {
        if (req.getTaskId() == null) {
            outln("需要ID", Colors.RED);
            return null;
        }
        Tracer t = getTaskManager().getTracer(req.getTaskId());
        if (t == null) {
            outln("不存在", Colors.RED);
            return null;
        }
        t.stop();
        getTaskManager().addTraceData(req.getTaskId(), t.getTraceData());
        TraceResult r = new TraceResult();
        r.setTaskId(req.getTaskId());
        r.setStatus("stopped");
        r.setTraceCount(t.getTraceData().size());
        outln("已停止", Colors.YELLOW);
        return r;
    }

    private TraceResult handleReport(TraceRequest req) {
        if (req.getTaskId() == null) {
            outln("需要ID", Colors.RED);
            return null;
        }
        List<TraceData> data = getTaskManager().getTraceData(req.getTaskId());
        if (data == null || data.isEmpty()) {
            outln("不存在", Colors.RED);
            return null;
        }
        TraceResult r = new TraceResult();
        r.setTaskId(req.getTaskId());
        r.setTraceCount(data.size());
        ArrayList<TraceResult.TraceEntry> entries = new ArrayList<>();
        for (TraceData d : data) {
            outln(String.format("  %-40s %s (%s)", d.name(), d.getDurationString(), d.threadName()), Colors.GRAY);
            var te = new TraceResult.TraceEntry();
            te.setName(d.name());
            te.setStartTime(d.startTime());
            te.setDuration(d.duration());
            te.setThreadId(d.threadId());
            te.setThreadName(d.threadName());
            entries.add(te);
        }
        r.setTraces(entries);
        return r;
    }

    private TraceResult handleExport(TraceRequest req) throws JSONException {
        if (req.getTaskId() == null || req.getFilePath() == null) {
            outln("参数不足", Colors.RED);
            return null;
        }
        List<TraceData> td = getTaskManager().getTraceData(req.getTaskId());
        if (td == null || td.isEmpty()) {
            outln("不存在", Colors.RED);
            return null;
        }
        JSONObject j = new JSONObject();
        j.put("id", req.getTaskId()).put("traceCount", td.size());
        JSONArray a = new JSONArray();
        for (TraceData d : td) {
            JSONObject o = new JSONObject();
            o.put("name", d.name()).put("duration", d.duration()).put("threadName", d.threadName());
            a.put(o);
        }
        j.put("traces", a);
        if (!writeToFile(req.getFilePath(), j.toString(2))) {
            outln("失败", Colors.RED);
            return null;
        }
        TraceResult r = new TraceResult();
        r.setTaskId(req.getTaskId());
        r.setStatus("exported");
        r.setExportPath(req.getFilePath());
        outln("已导出", Colors.GREEN);
        return r;
    }
}
