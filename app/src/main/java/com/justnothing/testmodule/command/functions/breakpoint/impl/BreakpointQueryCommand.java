package com.justnothing.testmodule.command.functions.breakpoint.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.breakpoint.BreakpointTexts;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointListRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointHitsRequest;
import com.justnothing.testmodule.command.functions.breakpoint.response.BreakpointResult;
import com.justnothing.testmodule.command.functions.intercept.base.BreakpointInterceptTask;
import com.justnothing.testmodule.command.framework.output.Colors;

import java.util.Date;
import java.util.List;

@SubCommandInfo(
    description = BreakpointTexts.SUB_BREAKPOINT_QUERY_DESC,
    usage = "breakpoint <list|hits>",
    examples = {
        "breakpoint list",
        "breakpoint hits"
    }
)
public class BreakpointQueryCommand extends AbstractBreakpointCommand<CommandRequest<?>, BreakpointResult> {

    @SuppressWarnings("unchecked")
    public BreakpointQueryCommand() {
        super("breakpoint query", (Class) CommandRequest.class, BreakpointResult.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BreakpointResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
        CommandRequest request = context.getRequest();

        if (request instanceof BreakpointListRequest) {
            return handleList((BreakpointListRequest) request);
        } else if (request instanceof BreakpointHitsRequest) {
            return handleHits((BreakpointHitsRequest) request);
        }

        return createErrorResult(Text.zhEn("未知的断点查询请求类型", "Unknown breakpoint query request type").text());
    }

    private BreakpointResult handleList(BreakpointListRequest request) {
        List<BreakpointInterceptTask> breakpoints = manager.listTasks();
        
        if (breakpoints.isEmpty()) {
            out(BreakpointTexts.NO_BREAKPOINTS.text(), Colors.GRAY);
            return createSuccessResult(BreakpointTexts.NO_BREAKPOINTS.text());
        }

        BreakpointResult result = new BreakpointResult();
        result.setSuccess(true);
        result.setSubCommand("list");

        out(Text.zhEn("=== 断点列表 ===", "=== Breakpoints ===").text(), Colors.CYAN);
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
            out(Text.zhEn("  类: ", "  Class: ").text(), Colors.CYAN);
            out(task.getClassName(), Colors.GREEN);
            out(Text.zhEn("  方法: ", "  Method: ").text(), Colors.CYAN);
            out(task.getMethodName(), Colors.GREEN);
            out(Text.zhEn("  签名: ", "  Signature: ").text(), Colors.CYAN);
            out(task.getSignature() != null ? task.getSignature() : BreakpointTexts.VALUE_ALL_OVERLOADS.text(), Colors.GRAY);
            out(Text.zhEn("  状态: ", "  Status: ").text(), Colors.CYAN);
            out(task.isEnabled() ? BreakpointTexts.VALUE_ENABLED.text() : BreakpointTexts.VALUE_DISABLED.text(),
                    task.isEnabled() ? Colors.GREEN : Colors.RED);
            out(Text.zhEn("  命中次数: ", "  Hits: ").text(), Colors.CYAN);
            out(String.valueOf(task.getHitCount()), Colors.YELLOW);
            if (task.getLastHitAt() > 0) {
                out(Text.zhEn("  最后命中: ", "  Last hit: ").text(), Colors.CYAN);
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
            out(BreakpointTexts.NO_BREAKPOINTS.text(), Colors.GRAY);
            return createSuccessResult(BreakpointTexts.NO_BREAKPOINTS.text());
        }

        BreakpointResult result = new BreakpointResult();
        result.setSuccess(true);
        result.setSubCommand("hits");

        out(Text.zhEn("=== 断点命中统计 ===", "=== Breakpoint hit statistics ===").text(), Colors.CYAN);
        out("", Colors.WHITE);

        int totalHits = 0;
        for (BreakpointInterceptTask task : breakpoints) {
            out("ID ", Colors.CYAN);
            out(String.valueOf(task.getId()), Colors.YELLOW);
            out(": ", Colors.WHITE);
            out(task.getClassName() + "." + task.getMethodName(), Colors.GREEN);
            out(Text.zhEn(" - 命中 ", " - hit ").text(), Colors.WHITE);
            out(String.valueOf(task.getHitCount()), Colors.YELLOW);
            out(Text.zhEn(" 次", " times").text(), Colors.WHITE);
            totalHits += task.getHitCount();
        }

        out("", Colors.WHITE);
        out(Text.zhEn("总计: ", "Total: ").text(), Colors.CYAN);
        out(String.valueOf(totalHits), Colors.YELLOW);
        out(Text.zhEn(" 次命中", " hits").text(), Colors.WHITE);

        result.setOutput(Text.zhEn("总计 %d 次命中", "Total: %d hits").format(totalHits));
        return result;
    }
}
