package com.justnothing.testmodule.command.functions.threads.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.threads.AbstractThreadsCommand;
import com.justnothing.testmodule.command.functions.threads.request.ThreadListRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadListResult;

import java.util.Arrays;
import java.util.Map;

@SubCommandInfo(
    description = "列出所有线程及其状态",
    usage = "threads list [options]",
    examples = {
        "threads list",
        "threads list --id 1",
        "threads list --name main",
        "threads list --state BLOCKED"
    }
)
public class ListCommand extends AbstractThreadsCommand<ThreadListRequest, ThreadListResult> {

    public ListCommand() {
        super("threads list", ThreadListRequest.class, ThreadListResult.class);
    }

    @Override
    protected ThreadListResult executeThreadsCommand(CommandExecutor.CmdExecContext<ThreadListRequest> context) throws Exception {
        ThreadListRequest request = context.getCommandRequest();
        
        Long filterId = request.getFilterId();
        String filterName = request.getFilterName();
        String filterState = request.getFilterState();

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();

        context.println("=== 线程信息 ===", Colors.CYAN);
        context.println("");
        context.print("线程总数: ", Colors.GRAY);
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

        context.println("=== 线程状态统计 ===", Colors.CYAN);
        context.println("");
        printStateCount(context, "BLOCKED", blockedCount, Colors.RED);
        printStateCount(context, "WAITING", waitingCount, Colors.YELLOW);
        printStateCount(context, "TIMED_WAITING", timedWaitingCount, Colors.MAGENTA);
        printStateCount(context, "RUNNABLE", runnableCount, Colors.LIGHT_GREEN);
        printStateCount(context, "TERMINATED", terminatedCount, Colors.GRAY);
        printStateCount(context, "NEW", newStateCount, Colors.CYAN);
        context.println("");

        context.println("=== 线程详情 ===", Colors.CYAN);
        context.println("");

        boolean found = false;
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();

            if (filterId != null && thread.getId() != filterId) continue;
            if (filterName != null && !thread.getName().equals(filterName)) continue;
            if (filterState != null && thread.getState() != Thread.State.valueOf(filterState)) continue;

            found = true;
            printThreadInfo(context, thread, stackTrace);
        }

        if (!found && (filterId != null || filterName != null || filterState != null)) {
            context.println("未找到匹配的线程", Colors.GRAY);
        }

        logger.info("线程信息查询完成");

        ThreadListResult result = new ThreadListResult();
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

        ctx.print("线程: ", Colors.CYAN);
        ctx.println(thread.getName(), Colors.LIGHT_GREEN);
        ctx.print("  ID: ", Colors.GRAY);
        ctx.println(String.valueOf(thread.getId()), Colors.YELLOW);
        ctx.print("  状态: ", Colors.GRAY);
        ctx.println(thread.getState().toString(), stateColor);
        ctx.print("  优先级: ", Colors.GRAY);
        ctx.println(String.valueOf(thread.getPriority()), Colors.LIGHT_GREEN);
        ctx.print("  守护: ", Colors.GRAY);
        ctx.println(thread.isDaemon() ? "是" : "否", thread.isDaemon() ? Colors.MAGENTA : Colors.LIGHT_GREEN);
        ctx.print("  中断: ", Colors.GRAY);
        ctx.println(thread.isInterrupted() ? "是" : "否", thread.isInterrupted() ? Colors.RED : Colors.LIGHT_GREEN);
        ctx.print("  是否存活: ", Colors.GRAY);
        ctx.println(thread.isAlive() ? "是" : "否", thread.isAlive() ? Colors.LIGHT_GREEN : Colors.GRAY);

        if (stackTrace != null && stackTrace.length > 0) {
            ctx.print("  堆栈:", Colors.GRAY);
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
