package com.justnothing.testmodule.command.functions.trace.impl;

import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.trace.TraceResult;
import com.justnothing.testmodule.command.functions.trace.request.TraceAddRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceStopRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceClearRequest;
import com.justnothing.testmodule.command.output.Colors;

@SubCommandInfo(
    description = "Trace 管理操作 - 添加/停止/清除",
    examples = {
        "trace add <class> <method> [sig]     添加跟踪任务",
        "trace stop <id>                    停止指定任务",
        "trace clear                        清除所有任务"
    }
)
public class TraceManageCommand extends AbstractTraceCommand<com.justnothing.testmodule.command.base.protocol.CommandRequest, TraceResult> {

    @Override
    protected TraceResult executeInternal(com.justnothing.testmodule.command.base.protocol.CommandRequest request) throws Exception {
        if (request instanceof TraceAddRequest r) return handleAdd(r);
        if (request instanceof TraceStopRequest r) return handleStop(r);
        if (request instanceof TraceClearRequest r) return handleClear(r);
        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    private TraceResult handleAdd(TraceAddRequest request) throws Exception {
        TraceResult r = new TraceResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("add");

        logger.info("添加 trace 任务: class=%s, method=%s, sig=%s",
                request.getClassName(), request.getMethodName(),
                request.getSignature() != null ? request.getSignature() : "所有");

        int id = manager.addTraceTask(
                request.getClassName(), request.getMethodName(),
                request.getSignature(), context.classLoader());

        outln("添加trace任务成功", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);

        r.setSuccess(true);
        r.setTargetClass(request.getClassName());
        r.setTargetMethod(request.getMethodName());
        r.setOutput("ID=" + id);
        r.setActive(true);
        return r;
    }

    private TraceResult handleStop(TraceStopRequest request) {
        TraceResult r = new TraceResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("stop");

        logger.info("停止 trace 任务: id=%d", request.getTraceId());

        boolean success = manager.removeTask(request.getTraceId());
        if (success) {
            outln("停止trace任务成功", Colors.GREEN);
            r.setSuccess(true);
            r.setActive(false);
            r.setOutput("stopped id=" + request.getTraceId());
        } else {
            out("错误: 未找到trace任务 (ID: ", Colors.RED);
            outln(String.valueOf(request.getTraceId()), Colors.YELLOW);
            r.setSuccess(false);
            r.setOutput("未找到 ID: " + request.getTraceId());
        }
        return r;
    }

    private TraceResult handleClear(TraceClearRequest request) {
        TraceResult r = okResult("clear");
        logger.warn("清除所有 trace 任务");
        manager.clearAll();
        outln("清除所有trace任务成功", Colors.GREEN);
        r.setActive(false);
        return r;
    }
}
