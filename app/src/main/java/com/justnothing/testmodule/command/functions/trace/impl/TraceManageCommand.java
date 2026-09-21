package com.justnothing.testmodule.command.functions.trace.impl;

import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.trace.response.TraceResult;
import com.justnothing.testmodule.command.functions.trace.request.TraceAddRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceStopRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceClearRequest;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.trace.TraceTexts;

@SubCommandInfo(
    description = TraceTexts.SUB_TRACE_MANAGE_DESC,
    examples = {
        "trace add <class> <method> [sig]     添加跟踪任务",
        "trace stop <id>                    停止指定任务",
        "trace clear                        清除所有任务"
    }
)
public class TraceManageCommand extends AbstractTraceCommand<CommandRequest<?>, TraceResult> {

    @SuppressWarnings("unchecked")
    public TraceManageCommand() {
        super("trace manage", (Class) CommandRequest.class, TraceResult.class);
    }

    @Override
    protected TraceResult executeRequest(CommandRequest<?> request) throws Exception {
        if (request instanceof TraceAddRequest r) return handleAdd(r);
        if (request instanceof TraceStopRequest r) return handleStop(r);
        if (request instanceof TraceClearRequest r) return handleClear(r);
        throw new IllegalArgumentException(TraceTexts.UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
    }

    private TraceResult handleAdd(TraceAddRequest request) {
        TraceResult r = new TraceResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("add");

        logger.info("添加 trace 任务: class=%s, method=%s, sig=%s",
                request.getClassName(), request.getMethodName(),
                request.getSignature() != null ? request.getSignature() : "所有");

        int id = manager.addTraceTask(
                request.getClassName(), request.getMethodName(),
                request.getSignature(), context.classLoader());

        outln(Text.zhEn("添加trace任务成功", "Trace task added successfully").text(), Colors.GREEN);
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
            outln(Text.zhEn("停止trace任务成功", "Trace task stopped successfully").text(), Colors.GREEN);
            r.setSuccess(true);
            r.setActive(false);
            r.setOutput("stopped id=" + request.getTraceId());
        } else {
            out(CliMessages.ERROR_PREFIX.text()
                    + Text.zhEn("未找到trace任务 (ID: ", "Trace task not found (ID: ").text(), Colors.RED);
            outln(String.valueOf(request.getTraceId()), Colors.YELLOW);
            r.setSuccess(false);
            r.setOutput(Text.zhEn("未找到 ID: %s", "Not found: ID %s").format(request.getTraceId()));
        }
        return r;
    }

    private TraceResult handleClear(TraceClearRequest request) {
        TraceResult r = okResult("clear");
        logger.warn("清除所有 trace 任务");
        manager.clearAll();
        outln(Text.zhEn("清除所有trace任务成功", "All trace tasks cleared").text(), Colors.GREEN);
        r.setActive(false);
        return r;
    }
}
