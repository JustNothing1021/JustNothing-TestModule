package com.justnothing.testmodule.command.functions.trace.impl;

import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.intercept.TraceInterceptTask;
import com.justnothing.testmodule.command.functions.trace.TraceResult;
import com.justnothing.testmodule.command.functions.trace.request.TraceListRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceShowRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceExportRequest;
import com.justnothing.testmodule.command.output.Colors;

import java.util.List;

@SubCommandInfo(
    description = "Trace 查询操作 - 列表/显示/导出",
    examples = {
        "trace list                        列出所有任务",
        "trace show <id>                   显示调用树",
        "trace export <id> <file>          导出到文件"
    }
)
public class TraceQueryCommand extends AbstractTraceCommand<com.justnothing.testmodule.command.base.protocol.CommandRequest, TraceResult> {

    @Override
    protected TraceResult executeInternal(com.justnothing.testmodule.command.base.protocol.CommandRequest request) throws Exception {
        if (request instanceof TraceListRequest) return handleList();
        if (request instanceof TraceShowRequest r) return handleShow(r);
        if (request instanceof TraceExportRequest r) return handleExport(r);
        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    private TraceResult handleList() {
        TraceResult r = new TraceResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("list");

        List<TraceInterceptTask> tasks = manager.listTasks();

        if (tasks.isEmpty()) {
            outln("没有活跃的trace任务", Colors.GRAY);
            r.setSuccess(true);
            r.setEntryCount(0L);
            return r;
        }

        outln("活跃的trace任务:", Colors.CYAN);
        outln("ID\t类名\t方法名\t签名\t状态\t调用次数", Colors.GRAY);
        outln("--------------------------------------------------", Colors.GRAY);

        for (TraceInterceptTask task : tasks) {
            out(String.valueOf(task.getId()), Colors.YELLOW);
            out("\t", Colors.WHITE);
            out(task.getClassName(), Colors.GREEN);
            out("\t", Colors.WHITE);
            out(task.getMethodName(), Colors.GREEN);
            out("\t", Colors.WHITE);
            out(task.getSignature() != null ? task.getSignature() : "所有", Colors.GRAY);
            out("\t", Colors.WHITE);
            out(task.isRunning() ? "运行中" : "已停止", task.isRunning() ? Colors.GREEN : Colors.RED);
            out("\t", Colors.WHITE);
            outln(String.valueOf(task.getCallCount()), Colors.YELLOW);
        }

        r.setSuccess(true);
        r.setEntryCount((long) tasks.size());
        r.setOutput(tasks.size() + " 个活跃任务");
        return r;
    }

    private TraceResult handleShow(TraceShowRequest request) throws Exception {
        TraceResult r = new TraceResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("show");

        logger.info("显示 trace 调用树: id=%d", request.getTraceId());

        TraceInterceptTask task = manager.getTask(request.getTraceId());
        if (task == null) {
            throw new IllegalCommandLineArgumentException("未找到trace任务 (ID: " + request.getTraceId() + ")");
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
            throw new IllegalCommandLineArgumentException("未找到trace任务 (ID: " + request.getTraceId() + ")");
        }

        boolean success = task.exportToFile(request.getFilePath());
        if (success) {
            outln("导出trace任务成功", Colors.GREEN);
            out("文件路径: ", Colors.CYAN);
            outln(request.getFilePath(), Colors.GRAY);

            r.setSuccess(true);
            r.setOutput(request.getFilePath());
            r.setTargetClass(task.getClassName());
            r.setTargetMethod(task.getMethodName());
        } else {
            throw new RuntimeException("导出trace任务失败");
        }
        return r;
    }
}
