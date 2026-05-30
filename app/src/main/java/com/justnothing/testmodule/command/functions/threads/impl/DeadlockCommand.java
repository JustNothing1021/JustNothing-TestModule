package com.justnothing.testmodule.command.functions.threads.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.threads.AbstractThreadsCommand;
import com.justnothing.testmodule.command.functions.threads.request.ThreadDeadlockRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadDeadlockResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

@SubCommandInfo(
    description = "检测Java应用程序中的死锁",
    usage = "threads deadlock",
    examples = {"threads deadlock"}
)
public class DeadlockCommand extends AbstractThreadsCommand<ThreadDeadlockRequest, ThreadDeadlockResult> {

    public DeadlockCommand() {
        super("threads deadlock", ThreadDeadlockRequest.class, ThreadDeadlockResult.class);
    }

    @Override
    protected ThreadDeadlockResult executeThreadsCommand(CommandExecutor.CmdExecContext<ThreadDeadlockRequest> context) throws Exception {
        context.println("===== 线程状态分析 =====", Colors.CYAN);
        context.print("时间: ", Colors.GRAY);
        context.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()), Colors.YELLOW);
        context.println("");

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();

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

        context.println("===== 线程状态统计 =====", Colors.CYAN);
        context.println("");
        context.print("线程总数: ", Colors.GRAY);
        context.println(String.valueOf(allStackTraces.size()), Colors.YELLOW);
        printStateCount(context, " BLOCKED", blockedCount, Colors.RED);
        printStateCount(context, " WAITING", waitingCount, Colors.YELLOW);
        printStateCount(context, " TIMED_WAITING", timedWaitingCount, Colors.MAGENTA);
        printStateCount(context, " RUNNABLE", runnableCount, Colors.LIGHT_GREEN);
        printStateCount(context, " TERMINATED", terminatedCount, Colors.GRAY);
        printStateCount(context, " NEW", newStateCount, Colors.CYAN);
        context.println("");

        if (blockedCount > 0) {
            context.println("===== 可能的死锁线程 =====", Colors.CYAN);
            context.println("");
            context.println("Tip: Android不提供完整的死锁检测API, 此命令只基于基本的线程状态分析", Colors.GRAY);
            context.println("以下BLOCKED状态的线程可能存在死锁:", Colors.GRAY);
            context.println("");

            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                Thread thread = entry.getKey();
                if (thread.getState() == Thread.State.BLOCKED) {
                    StackTraceElement[] stackTrace = entry.getValue();

                    context.print("线程: ", Colors.CYAN);
                    context.println(thread.getName(), Colors.LIGHT_GREEN);
                    context.print("  ID: ", Colors.GRAY);
                    context.println(String.valueOf(thread.getId()), Colors.YELLOW);
                    context.print("  状态: ", Colors.GRAY);
                    context.println(thread.getState().toString(), Colors.RED);
                    context.print("  优先级: ", Colors.GRAY);
                    context.println(String.valueOf(thread.getPriority()), Colors.LIGHT_GREEN);
                    context.print("  守护: ", Colors.GRAY);
                    context.println(String.valueOf(thread.isDaemon()), thread.isDaemon() ? Colors.MAGENTA : Colors.LIGHT_GREEN);
                    context.print("  中断: ", Colors.GRAY);
                    context.println(String.valueOf(thread.isInterrupted()), thread.isInterrupted() ? Colors.RED : Colors.LIGHT_GREEN);
                    context.println("");

                    if (stackTrace != null && stackTrace.length > 0) {
                        context.print("  堆栈跟踪:", Colors.GRAY);
                        context.println("");
                        for (StackTraceElement element : stackTrace) {
                            context.print("    ", Colors.DEFAULT);
                        }
                    }
                    context.println("");
                }
            }
        } else {
            context.println("未检测到BLOCKED状态的线程, 应该是没有死锁的", Colors.LIGHT_GREEN);
            context.println("");
        }

        context.println("===== 检测结果 =====", Colors.CYAN);
        context.println("");
        context.print("检测到 ", Colors.GRAY);
        context.print(String.valueOf(blockedCount), blockedCount > 0 ? Colors.RED : Colors.LIGHT_GREEN);
        context.println(" 个BLOCKED状态的线程", Colors.GRAY);

        if (blockedCount > 0) {
            context.println("建议采取以下措施:", Colors.YELLOW);
            context.println("  1. 检查BLOCKED线程的堆栈跟踪, 找出阻塞发生的位置", Colors.GRAY);
            context.println("  2. 检查锁的获取顺序, 确保所有线程以相同的顺序获取锁", Colors.GRAY);
            context.println("  3. 使用 tryLock() 替代 lock(), 避免无限等待", Colors.GRAY);
        } else {
            context.println("当前没有检测到明显的死锁迹象", Colors.LIGHT_GREEN);
        }

        logger.info("线程状态分析完成, 发现 " + blockedCount + " 个BLOCKED线程");

        ThreadDeadlockResult result = new ThreadDeadlockResult();
        result.setBlockedThreadCount(blockedCount);
        result.setHasDeadlock(blockedCount > 0);

        return result;
    }

    private void printStateCount(CommandExecutor.CmdExecContext ctx, String stateName, int count, byte color) {
        ctx.print(stateName + ": ", Colors.GRAY);
        ctx.println(String.valueOf(count), color);
    }
}
