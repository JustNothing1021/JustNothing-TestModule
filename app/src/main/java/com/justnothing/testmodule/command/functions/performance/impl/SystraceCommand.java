package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.functions.performance.request.SystraceRequest;
import com.justnothing.testmodule.command.functions.performance.response.SystraceResult;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceData;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceParser;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceRunner;
import com.justnothing.testmodule.command.output.Colors;

public class SystraceCommand extends AbstractPerfCommand<SystraceRequest, SystraceResult> {

    public SystraceCommand() {
        super("performance systrace", SystraceRequest.class, SystraceResult.class);
    }

    @Override
    protected SystraceResult executePerfCommand(SystraceRequest req) {
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

    private SystraceResult handleStart(SystraceRequest req) {
        int dur = req.getDuration() != null ? req.getDuration() : 10;
        if (dur <= 0) {
            outln("持续时间必须>0", Colors.RED);
            return null;
        }
        SystraceRunner runner = new SystraceRunner("/data/local/tmp/systrace");
        runner.start(dur, null);
        int id = getTaskManager().addSystraceRunner(runner);
        SystraceResult r = new SystraceResult();
        r.setTaskId(id);
        r.setStatus("running");
        r.setDuration(dur);
        outln("Systrace 已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        return r;
    }

    private SystraceResult handleStop(SystraceRequest req) {
        if (req.getTaskId() == null) {
            outln("需要ID", Colors.RED);
            return null;
        }
        SystraceRunner sr = getTaskManager().getSystraceRunner(req.getTaskId());
        if (sr == null) {
            outln("不存在", Colors.RED);
            return null;
        }
        sr.stop();
        SystraceResult r = new SystraceResult();
        r.setTaskId(req.getTaskId());
        r.setStatus("stopped");
        r.setDuration((int) sr.getDuration());
        String f = sr.getOutputFile();
        if (f != null && !f.isEmpty()) {
            try {
                SystraceData d = SystraceParser.parse(f);
                getTaskManager().addSystraceData(req.getTaskId(), d);
                r.setOutputFile(f);
                outln("已停止", Colors.YELLOW);
            } catch (Exception e) {
                outln("停止但解析失败", Colors.YELLOW);
            }
        } else
            outln("已停止", Colors.YELLOW);
        return r;
    }

    private SystraceResult handleReport(SystraceRequest req) {
        if (req.getTaskId() == null) {
            outln("需要ID", Colors.RED);
            return null;
        }
        SystraceData d = getTaskManager().getSystraceData(req.getTaskId());
        if (d == null) {
            outln("不存在", Colors.RED);
            return null;
        }
        String report = SystraceParser.generateReport(d);
        SystraceResult r = new SystraceResult();
        r.setTaskId(req.getTaskId());
        r.setOutputFile(d.file());
        r.setReport(report);
        out(SystraceParser.generateReport(d), Colors.WHITE);
        return r;
    }

    private SystraceResult handleExport(SystraceRequest req) {
        if (req.getTaskId() == null || req.getFilePath() == null) {
            outln("参数不足", Colors.RED);
            return null;
        }
        SystraceData d = getTaskManager().getSystraceData(req.getTaskId());
        if (d == null) {
            outln("不存在", Colors.RED);
            return null;
        }
        String rpt = SystraceParser.generateReport(d);
        if (!writeToFile(req.getFilePath(), rpt)) {
            outln("失败", Colors.RED);
            return null;
        }
        SystraceResult r = new SystraceResult();
        r.setTaskId(req.getTaskId());
        r.setOutputFile(d.file());
        r.setExportPath(req.getFilePath());
        outln("已导出", Colors.GREEN);
        return r;
    }
}
