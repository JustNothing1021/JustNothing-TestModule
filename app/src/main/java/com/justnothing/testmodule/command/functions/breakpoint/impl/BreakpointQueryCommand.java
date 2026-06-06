package com.justnothing.testmodule.command.functions.breakpoint.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointListRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointHitsRequest;
import com.justnothing.testmodule.command.functions.breakpoint.response.BreakpointResult;
import com.justnothing.testmodule.command.functions.intercept.BreakpointInterceptTask;
import com.justnothing.testmodule.command.output.Colors;

import java.util.Date;
import java.util.List;

@SubCommandInfo(
    description = "断点查询命令（列表、命中统计）",
    usage = "breakpoint <list|hits>",
    examples = {
        "breakpoint list",
        "breakpoint hits"
    }
)
public class BreakpointQueryCommand extends AbstractBreakpointCommand<CommandRequest, BreakpointResult> {

    public BreakpointQueryCommand() {
        super("breakpoint query", CommandRequest.class, BreakpointResult.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BreakpointResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        CommandRequest request = context.getRequest();

        if (request instanceof BreakpointListRequest) {
            return handleList((BreakpointListRequest) request);
        } else if (request instanceof BreakpointHitsRequest) {
            return handleHits((BreakpointHitsRequest) request);
        }

        return createErrorResult("未知的断点查询请求类型");
    }

    private BreakpointResult handleList(BreakpointListRequest request) {
        List<BreakpointInterceptTask> breakpoints = manager.listTasks();
        
        if (breakpoints.isEmpty()) {
            out("没有设置任何断点", Colors.GRAY);
            return createSuccessResult("没有设置任何断点");
        }

        BreakpointResult result = new BreakpointResult();
        result.setSuccess(true);
        result.setSubCommand("list");

        out("=== 断点列表 ===", Colors.CYAN);
        out("", Colors.WHITE);

        for (BreakpointInterceptTask task : breakpoints) {
            BreakpointResult.BreakpointInfo info = new BreakpointResult.BreakpointInfo();
            info.setId(String.valueOf(task.getId()));
            info.setClassName(task.getClassName());
            info.setMethodName(task.getMethodName());
            info.setEnabled(task.isEnabled());
            info.setHitCount(task.getHitCount());
            result.addBreakpoint(info);

            out("ID: ", Colors.CYAN);
            out(String.valueOf(task.getId()), Colors.YELLOW);
            out("  类: ", Colors.CYAN);
            out(task.getClassName(), Colors.GREEN);
            out("  方法: ", Colors.CYAN);
            out(task.getMethodName(), Colors.GREEN);
            out("  签名: ", Colors.CYAN);
            out(task.getSignature() != null ? task.getSignature() : "所有重载", Colors.GRAY);
            out("  状态: ", Colors.CYAN);
            out(task.isEnabled() ? "启用" : "禁用", task.isEnabled() ? Colors.GREEN : Colors.RED);
            out("  命中次数: ", Colors.CYAN);
            out(String.valueOf(task.getHitCount()), Colors.YELLOW);
            if (task.getLastHitAt() > 0) {
                out("  最后命中: ", Colors.CYAN);
                out(String.valueOf(new Date(task.getLastHitAt())), Colors.GRAY);
            }
            out("", Colors.WHITE);
        }

        result.setTotalBreakpoints(breakpoints.size());
        long activeCount = breakpoints.stream().filter(BreakpointInterceptTask::isEnabled).count();
        result.setActiveBreakpoints((int) activeCount);

        return result;
    }

    private BreakpointResult handleHits(BreakpointHitsRequest request) {
        List<BreakpointInterceptTask> breakpoints = manager.listTasks();

        if (breakpoints.isEmpty()) {
            out("没有设置任何断点", Colors.GRAY);
            return createSuccessResult("没有设置任何断点");
        }

        BreakpointResult result = new BreakpointResult();
        result.setSuccess(true);
        result.setSubCommand("hits");

        out("=== 断点命中统计 ===", Colors.CYAN);
        out("", Colors.WHITE);

        int totalHits = 0;
        for (BreakpointInterceptTask task : breakpoints) {
            out("ID ", Colors.CYAN);
            out(String.valueOf(task.getId()), Colors.YELLOW);
            out(": ", Colors.WHITE);
            out(task.getClassName() + "." + task.getMethodName(), Colors.GREEN);
            out(" - 命中 ", Colors.WHITE);
            out(String.valueOf(task.getHitCount()), Colors.YELLOW);
            out(" 次", Colors.WHITE);
            totalHits += task.getHitCount();
        }

        out("", Colors.WHITE);
        out("总计: ", Colors.CYAN);
        out(String.valueOf(totalHits), Colors.YELLOW);
        out(" 次命中", Colors.WHITE);

        result.setOutput("总计 " + totalHits + " 次命中");
        return result;
    }
}
