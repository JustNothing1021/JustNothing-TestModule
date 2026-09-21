package com.justnothing.testmodule.command.functions.threads.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.threads.ThreadsTexts;
import com.justnothing.testmodule.command.functions.threads.request.ThreadListRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadDetail;
import com.justnothing.testmodule.command.functions.threads.response.ThreadListResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SubCommandInfo(
    description = ThreadsTexts.SUB_THREADS_LIST_DESC,
    usage = "threads list [options]",
    examples = {
        "threads list",
        "threads list --id 1",
        "threads list --name main",
        "threads list --state BLOCKED"
    }
)
public class ThreadListCommand extends AbstractThreadsCommand<ThreadListRequest, ThreadListResult> {

    public ThreadListCommand() {
        super("threads list", ThreadListRequest.class, ThreadListResult.class);
    }

    @Override
    protected ThreadListResult executeThreadsCommand(CommandExecutor.CmdExecContext<ThreadListRequest> context) throws Exception {
        ThreadListRequest request = context.getCommandRequest();
        
        Long filterId = request.getFilterId();
        String filterName = request.getFilterName();
        String filterState = request.getFilterState();

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();

        context.println(Text.zhEn("=== 线程信息 ===", "=== Thread information ===").text(), Colors.CYAN);
        context.println("");
        context.print(ThreadsTexts.LABEL_THREAD_COUNT.text(), Colors.GRAY);
        context.println(String.valueOf(allStackTraces.size()), Colors.YELLOW);
        context.println("");

        int blockedCount = 0;
        int waitingCount = 0;
        int timedWaitingCount = 0;
        int runnableCount = 0;
        int terminatedCount = 0;
        int newStateCount = 0;

        for (Thread thread : allStackTraces.keySet()) {
            Thread.State state = thread.getState();
            switch (state) {
                case BLOCKED -> blockedCount++;
                case WAITING -> waitingCount++;
                case TIMED_WAITING -> timedWaitingCount++;
                case RUNNABLE -> runnableCount++;
                case TERMINATED -> terminatedCount++;
                case NEW -> newStateCount++;
            }
        }

        context.println(Text.zhEn("=== 线程状态统计 ===", "=== Thread state stats ===").text(), Colors.CYAN);
        context.println("");
        printStateCount(context, "BLOCKED", blockedCount, Colors.RED);
        printStateCount(context, "WAITING", waitingCount, Colors.YELLOW);
        printStateCount(context, "TIMED_WAITING", timedWaitingCount, Colors.MAGENTA);
        printStateCount(context, "RUNNABLE", runnableCount, Colors.LIGHT_GREEN);
        printStateCount(context, "TERMINATED", terminatedCount, Colors.GRAY);
        printStateCount(context, "NEW", newStateCount, Colors.CYAN);
        context.println("");

        context.println(Text.zhEn("=== 线程详情 ===", "=== Thread details ===").text(), Colors.CYAN);
        context.println("");

        boolean found = false;
        boolean includeStackTrace = !ThreadListRequest.LEVEL_BASIC.equals(request.getDetailLevel());
        List<ThreadDetail> details = new ArrayList<>();

        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();

            if (filterId != null && thread.getId() != filterId) continue;
            if (filterName != null && !thread.getName().equals(filterName)) continue;
            if (filterState != null && thread.getState() != Thread.State.valueOf(filterState)) continue;

            found = true;
            printThreadInfo(context, thread, stackTrace);
            details.add(ThreadDetail.of(thread, stackTrace, includeStackTrace));
        }

        if (!found && (filterId != null || filterName != null || filterState != null)) {
            context.println(Text.zhEn("未找到匹配的线程", "No matching thread found").text(), Colors.GRAY);
        }

        logger.info("线程信息查询完成");

        ThreadListResult result = new ThreadListResult();
        result.setTimestamp(System.currentTimeMillis());
        result.setThreadDetails(details);
        result.setTotalThreadCount(allStackTraces.size());
        result.setBlockedCount(blockedCount);
        result.setWaitingCount(waitingCount);
        result.setTimedWaitingCount(timedWaitingCount);
        result.setRunnableCount(runnableCount);
        result.setTerminatedCount(terminatedCount);
        result.setNewCount(newStateCount);

        return result;
    }

    private void printStateCount(CommandExecutor.CmdExecContext ctx, String stateName, int count, byte color) {
        ctx.print(stateName + ": ", Colors.GRAY);
        ctx.println(String.valueOf(count), color);
    }

    private void printThreadInfo(CommandExecutor.CmdExecContext ctx, Thread thread, StackTraceElement[] stackTrace) {
        byte stateColor = getStateColor(thread.getState());

        ctx.print(ThreadsTexts.LABEL_THREAD.text(), Colors.CYAN);
        ctx.println(thread.getName(), Colors.LIGHT_GREEN);
        ctx.print("  ID: ", Colors.GRAY);
        ctx.println(String.valueOf(thread.getId()), Colors.YELLOW);
        ctx.print(ThreadsTexts.LABEL_THREAD_STATE.text(), Colors.GRAY);
        ctx.println(thread.getState().toString(), stateColor);
        ctx.print(ThreadsTexts.LABEL_PRIORITY.text(), Colors.GRAY);
        ctx.println(String.valueOf(thread.getPriority()), Colors.LIGHT_GREEN);
        ctx.print(ThreadsTexts.LABEL_DAEMON.text(), Colors.GRAY);
        ctx.println(thread.isDaemon() ? ThreadsTexts.VALUE_YES.text() : ThreadsTexts.VALUE_NO.text(), thread.isDaemon() ? Colors.MAGENTA : Colors.LIGHT_GREEN);
        ctx.print(ThreadsTexts.LABEL_INTERRUPTED.text(), Colors.GRAY);
        ctx.println(thread.isInterrupted() ? ThreadsTexts.VALUE_YES.text() : ThreadsTexts.VALUE_NO.text(), thread.isInterrupted() ? Colors.RED : Colors.LIGHT_GREEN);
        ctx.print(Text.zhEn("  是否存活: ", "  Alive: ").text(), Colors.GRAY);
        ctx.println(thread.isAlive() ? ThreadsTexts.VALUE_YES.text() : ThreadsTexts.VALUE_NO.text(), thread.isAlive() ? Colors.LIGHT_GREEN : Colors.GRAY);

        if (stackTrace != null && stackTrace.length > 0) {
            ctx.print(Text.zhEn("  堆栈:", "  Stack trace:").text(), Colors.GRAY);
            ctx.println("");
            for (StackTraceElement element : stackTrace) {
                ctx.print("    ", Colors.DEFAULT);
                printStackTraceElement(ctx, element);
            }
        }
        ctx.println("");
    }

    private byte getStateColor(Thread.State state) {
        return switch (state) {
            case RUNNABLE -> Colors.LIGHT_GREEN;
            case BLOCKED -> Colors.RED;
            case WAITING -> Colors.YELLOW;
            case TIMED_WAITING -> Colors.MAGENTA;
            case TERMINATED -> Colors.GRAY;
            case NEW -> Colors.CYAN;
        };
    }

    private void printStackTraceElement(CommandExecutor.CmdExecContext ctx, StackTraceElement element) {
        String className = element.getClassName();
        String methodName = element.getMethodName();
        String fileName = element.getFileName();
        int lineNumber = element.getLineNumber();

        int lastDot = className.lastIndexOf('.');
        String packageName = lastDot > 0 ? className.substring(0, lastDot) : "";
        String simpleClassName = lastDot > 0 ? className.substring(lastDot + 1) : className;

        if (!packageName.isEmpty()) {
            ctx.print(packageName + ".", Colors.GRAY);
        }
        ctx.print(simpleClassName, Colors.GREEN);
        ctx.print(".", Colors.GRAY);
        ctx.print(methodName, Colors.LIGHT_BLUE);
        ctx.print("(", Colors.MAGENTA);

        if (fileName != null) {
            ctx.print(fileName, Colors.CYAN);
            if (lineNumber >= 0) {
                ctx.print(":", Colors.GRAY);
                ctx.print(String.valueOf(lineNumber), Colors.YELLOW);
            }
        } else {
            ctx.print("Unknown Source", Colors.GRAY);
        }
        ctx.println(")", Colors.MAGENTA);
    }
}
