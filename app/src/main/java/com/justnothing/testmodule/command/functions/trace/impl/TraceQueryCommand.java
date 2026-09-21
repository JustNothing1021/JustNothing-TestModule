package com.justnothing.testmodule.command.functions.trace.impl;

import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.intercept.TraceInterceptTask;
import com.justnothing.testmodule.command.functions.trace.response.TraceResult;
import com.justnothing.testmodule.command.functions.trace.request.TraceListRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceShowRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceExportRequest;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.trace.TraceTexts;

import java.util.List;

@SubCommandInfo(
    description = TraceTexts.SUB_TRACE_QUERY_DESC,
    examples = {
        "trace list                        列出所有任务",
        "trace show <id>                   显示调用树",
        "trace export <id> <file>          导出到文件"
    }
)
public class TraceQueryCommand extends AbstractTraceCommand<CommandRequest<?>, TraceResult> {

    @SuppressWarnings("unchecked")
    public TraceQueryCommand() {
        super("trace query", (Class) CommandRequest.class, TraceResult.class);
    }

    @Override
    protected TraceResult executeRequest(CommandRequest<?> request) throws Exception {
        if (request instanceof TraceListRequest) return handleList();
        if (request instanceof TraceShowRequest r) return handleShow(r);
        if (request instanceof TraceExportRequest r) return handleExport(r);
        throw new IllegalArgumentException(TraceTexts.UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
    }

    private TraceResult handleList() {
        TraceResult r = new TraceResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("list");

        List<TraceInterceptTask> tasks = manager.listTasks();

        if (tasks.isEmpty()) {
            outln(Text.zhEn("没有活跃的trace任务", "No active trace tasks").text(), Colors.GRAY);
            r.setSuccess(true);
            r.setEntryCount(0L);
            return r;
        }

        outln(Text.zhEn("活跃的trace任务:", "Active trace tasks:").text(), Colors.CYAN);
        outln(Text.zhEn("ID\t类名\t方法名\t签名\t状态\t调用次数",
                "ID\tClass\tMethod\tSignature\tStatus\tCall count").text(), Colors.GRAY);
        outln("--------------------------------------------------", Colors.GRAY);

        for (TraceInterceptTask task : tasks) {
            out(String.valueOf(task.getId()), Colors.YELLOW);
            out("\t", Colors.WHITE);
            out(task.getClassName(), Colors.GREEN);
            out("\t", Colors.WHITE);
            out(task.getMethodName(), Colors.GREEN);
            out("\t", Colors.WHITE);
            out(task.getSignature() != null ? task.getSignature() : Text.zhEn("所有", "all").text(), Colors.GRAY);
            out("\t", Colors.WHITE);
            out(task.isRunning() ? Text.zhEn("运行中", "Running").text() : Text.zhEn("已停止", "Stopped").text(),
                    task.isRunning() ? Colors.GREEN : Colors.RED);
            out("\t", Colors.WHITE);
            outln(String.valueOf(task.getCallCount()), Colors.YELLOW);
        }

        r.setSuccess(true);
        r.setEntryCount((long) tasks.size());
        r.setOutput(Text.zhEn("%s 个活跃任务", "%s active tasks").format(tasks.size()));
        return r;
    }

    private TraceResult handleShow(TraceShowRequest request) throws Exception {
        TraceResult r = new TraceResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("show");

        logger.info("显示 trace 调用树: id=%d", request.getTraceId());

        TraceInterceptTask task = manager.getTask(request.getTraceId());
        if (task == null) {
            throw new IllegalCommandLineArgumentException(TraceTexts.TRACE_TASK_NOT_FOUND.format(request.getTraceId()));
        }

        String result = task.getCallTree();
        outln(result, Colors.WHITE);

        r.setSuccess(true);
        r.setTargetClass(task.getClassName());
        r.setTargetMethod(task.getMethodName());
        r.setOutput(result);
        r.setActive(task.isRunning());
        r.setEntryCount((long) task.getCallCount());
        return r;
    }

    private TraceResult handleExport(TraceExportRequest request) throws Exception {
        TraceResult r = new TraceResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("export");

        logger.info("导出 trace 结果: id=%d, file=%s", request.getTraceId(), request.getFilePath());

        TraceInterceptTask task = manager.getTask(request.getTraceId());
        if (task == null) {
            throw new IllegalCommandLineArgumentException(TraceTexts.TRACE_TASK_NOT_FOUND.format(request.getTraceId()));
        }

        boolean success = task.exportToFile(request.getFilePath());
        if (success) {
            outln(Text.zhEn("导出trace任务成功", "Trace task exported successfully").text(), Colors.GREEN);
            out(Text.zhEn("文件路径: ", "File path: ").text(), Colors.CYAN);
            outln(request.getFilePath(), Colors.GRAY);

            r.setSuccess(true);
            r.setOutput(request.getFilePath());
            r.setTargetClass(task.getClassName());
            r.setTargetMethod(task.getMethodName());
        } else {
            throw new RuntimeException(Text.zhEn("导出trace任务失败", "Failed to export trace task").text());
        }
        return r;
    }
}
