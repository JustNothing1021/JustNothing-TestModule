package com.justnothing.testmodule.command.functions.threads.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.threads.ThreadsTexts;
import com.justnothing.testmodule.command.functions.threads.request.ThreadDeadlockRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadDeadlockResult;
import com.justnothing.testmodule.command.functions.threads.response.ThreadDetail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SubCommandInfo(
    description = ThreadsTexts.SUB_THREADS_DEADLOCK_DESC,
    usage = "threads deadlock",
    examples = {"threads deadlock"}
)
public class ThreadDeadlockCommand extends AbstractThreadsCommand<ThreadDeadlockRequest, ThreadDeadlockResult> {

    public ThreadDeadlockCommand() {
        super("threads deadlock", ThreadDeadlockRequest.class, ThreadDeadlockResult.class);
    }

    @Override
    protected ThreadDeadlockResult executeThreadsCommand(CommandExecutor.CmdExecContext<ThreadDeadlockRequest> context) throws Exception {
        context.println(Text.zhEn("===== 线程状态分析 =====", "===== Thread state analysis =====").text(), Colors.CYAN);
        context.print(ThreadsTexts.LABEL_TIME.text(), Colors.GRAY);
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

        context.println(Text.zhEn("===== 线程状态统计 =====", "===== Thread state stats =====").text(), Colors.CYAN);
        context.println("");
        context.print(ThreadsTexts.LABEL_THREAD_COUNT.text(), Colors.GRAY);
        context.println(String.valueOf(allStackTraces.size()), Colors.YELLOW);
        printStateCount(context, " BLOCKED", blockedCount, Colors.RED);
        printStateCount(context, " WAITING", waitingCount, Colors.YELLOW);
        printStateCount(context, " TIMED_WAITING", timedWaitingCount, Colors.MAGENTA);
        printStateCount(context, " RUNNABLE", runnableCount, Colors.LIGHT_GREEN);
        printStateCount(context, " TERMINATED", terminatedCount, Colors.GRAY);
        printStateCount(context, " NEW", newStateCount, Colors.CYAN);
        context.println("");

        List<ThreadDetail> blockedThreads = new ArrayList<>();

        if (blockedCount > 0) {
            context.println(Text.zhEn("===== 可能的死锁线程 =====", "===== Possible deadlock threads =====").text(), Colors.CYAN);
            context.println("");
            context.println(Text.zhEn("Tip: Android不提供完整的死锁检测API, 此命令只基于基本的线程状态分析",
                    "Tip: Android offers no complete deadlock detection API, so this command only analyzes basic thread states").text(), Colors.GRAY);
            context.println(Text.zhEn("以下BLOCKED状态的线程可能存在死锁:", "The following BLOCKED threads may be deadlocked:").text(), Colors.GRAY);
            context.println("");

            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                Thread thread = entry.getKey();
                if (thread.getState() == Thread.State.BLOCKED) {
                    StackTraceElement[] stackTrace = entry.getValue();

                    context.print(ThreadsTexts.LABEL_THREAD.text(), Colors.CYAN);
                    context.println(thread.getName(), Colors.LIGHT_GREEN);
                    context.print("  ID: ", Colors.GRAY);
                    context.println(String.valueOf(thread.getId()), Colors.YELLOW);
                    context.print(ThreadsTexts.LABEL_THREAD_STATE.text(), Colors.GRAY);
                    context.println(thread.getState().toString(), Colors.RED);
                    context.print(ThreadsTexts.LABEL_PRIORITY.text(), Colors.GRAY);
                    context.println(String.valueOf(thread.getPriority()), Colors.LIGHT_GREEN);
                    context.print(ThreadsTexts.LABEL_DAEMON.text(), Colors.GRAY);
                    context.println(String.valueOf(thread.isDaemon()), thread.isDaemon() ? Colors.MAGENTA : Colors.LIGHT_GREEN);
                    context.print(ThreadsTexts.LABEL_INTERRUPTED.text(), Colors.GRAY);
                    context.println(String.valueOf(thread.isInterrupted()), thread.isInterrupted() ? Colors.RED : Colors.LIGHT_GREEN);
                    context.println("");

                    if (stackTrace != null && stackTrace.length > 0) {
                        context.print(Text.zhEn("  堆栈跟踪:", "  Stack trace:").text(), Colors.GRAY);
                        context.println("");
                        for (StackTraceElement element : stackTrace) {
                            context.print("    ", Colors.DEFAULT);
                            context.println(element.toString(), Colors.DEFAULT);
                        }
                    }
                    context.println("");

                    blockedThreads.add(ThreadDetail.of(thread, stackTrace, true));
                }
            }
        } else {
            context.println(Text.zhEn("未检测到BLOCKED状态的线程, 应该是没有死锁的",
                    "No BLOCKED thread detected, so there is probably no deadlock").text(), Colors.LIGHT_GREEN);
            context.println("");
        }

        context.println(Text.zhEn("===== 检测结果 =====", "===== Detection result =====").text(), Colors.CYAN);
        context.println("");
        context.print(Text.zhEn("检测到 ", "Detected ").text(), Colors.GRAY);
        context.print(String.valueOf(blockedCount), blockedCount > 0 ? Colors.RED : Colors.LIGHT_GREEN);
        context.println(Text.zhEn(" 个BLOCKED状态的线程", " BLOCKED threads").text(), Colors.GRAY);

        if (blockedCount > 0) {
            context.println(Text.zhEn("建议采取以下措施:", "Suggested actions:").text(), Colors.YELLOW);
            context.println(Text.zhEn("  1. 检查BLOCKED线程的堆栈跟踪, 找出阻塞发生的位置",
                    "  1. Inspect the stack trace of the BLOCKED thread to find where it is blocked").text(), Colors.GRAY);
            context.println(Text.zhEn("  2. 检查锁的获取顺序, 确保所有线程以相同的顺序获取锁",
                    "  2. Check the lock acquisition order and make sure all threads acquire locks in the same order").text(), Colors.GRAY);
            context.println(Text.zhEn("  3. 使用 tryLock() 替代 lock(), 避免无限等待",
                    "  3. Use tryLock() instead of lock() to avoid waiting forever").text(), Colors.GRAY);
        } else {
            context.println(Text.zhEn("当前没有检测到明显的死锁迹象", "No obvious sign of deadlock detected").text(), Colors.LIGHT_GREEN);
        }

        logger.info("线程状态分析完成, 发现 " + blockedCount + " 个BLOCKED线程");

        ThreadDeadlockResult result = new ThreadDeadlockResult();
        result.setTimestamp(System.currentTimeMillis());
        result.setBlockedThreadCount(blockedCount);
        result.setHasDeadlock(blockedCount > 0);
        result.setBlockedThreads(blockedThreads);

        return result;
    }

    private void printStateCount(CommandExecutor.CmdExecContext ctx, String stateName, int count, byte color) {
        ctx.print(stateName + ": ", Colors.GRAY);
        ctx.println(String.valueOf(count), color);
    }
}
