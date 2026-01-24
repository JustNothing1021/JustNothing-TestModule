package com.justnothing.testmodule.command.functions.deadlock;

import static com.justnothing.testmodule.constants.CommandServer.CMD_DEADLOCK_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class DeadlockMain extends CommandBase {

    public DeadlockMain() {
        super("Deadlock");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: deadlock
                
                检测Java应用程序中的死锁.

                (我的神秘架构有救了, 但不多)
                
                该命令会:
                    - 检测所有线程的状态
                    - 显示BLOCKED状态的线程
                    - 显示线程的堆栈跟踪
                    - 分析可能的死锁情况
                
                注意: Android不提供完整的死锁检测API, 此命令只提供基本的线程状态分析.
                
                示例:
                    deadlock
                
                (Submodule deadlock %s)
                """, CMD_DEADLOCK_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        StringBuilder result = new StringBuilder();
        
        try {
            result.append("===== 线程状态分析 =====\n");
            result.append("时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");
            
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
                    case BLOCKED:
                        blockedCount++;
                        break;
                    case WAITING:
                        waitingCount++;
                        break;
                    case TIMED_WAITING:
                        timedWaitingCount++;
                        break;
                    case RUNNABLE:
                        runnableCount++;
                        break;
                    case TERMINATED:
                        terminatedCount++;
                        break;
                    case NEW:
                        newStateCount++;
                        break;
                }
            }
            
            result.append("===== 线程状态统计 =====\n\n");
            result.append("线程总数: ").append(allStackTraces.size()).append("\n");
            result.append(" BLOCKED: ").append(blockedCount).append("\n");
            result.append(" WAITING: ").append(waitingCount).append("\n");
            result.append(" TIMED_WAITING: ").append(timedWaitingCount).append("\n");
            result.append(" RUNNABLE: ").append(runnableCount).append("\n");
            result.append(" TERMINATED: ").append(terminatedCount).append("\n");
            result.append(" NEW: ").append(newStateCount).append("\n\n");
            
            if (blockedCount > 0) {
                result.append("===== 可能的死锁线程 =====\n\n");
                result.append("Tip: Android不提供完整的死锁检测API, 此命令只基于基本的线程状态分析\n");
                result.append("以下BLOCKED状态的线程可能存在死锁:\n\n");
                
                for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                    Thread thread = entry.getKey();
                    if (thread.getState() == Thread.State.BLOCKED) {
                        StackTraceElement[] stackTrace = entry.getValue();
                        
                        result.append("线程: ").append(thread.getName()).append("\n");
                        result.append("  ID: ").append(thread.getId()).append("\n");
                        result.append("  状态: ").append(thread.getState()).append("\n");
                        result.append("  优先级: ").append(thread.getPriority()).append("\n");
                        result.append("  守护: ").append(thread.isDaemon()).append("\n");
                        result.append("  中断: ").append(thread.isInterrupted()).append("\n\n");
                        
                        if (stackTrace != null && stackTrace.length > 0) {
                            result.append("  堆栈跟踪:\n");
                            for (StackTraceElement element : stackTrace) {
                                result.append("    ").append(element.toString()).append("\n");
                            }
                        }
                        result.append("\n");
                    }
                }
            } else {
                result.append("未检测到BLOCKED状态的线程, 应该是没有死锁的\n\n");
            }
            
            result.append("===== 所有线程详情 =====\n\n");
            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                Thread thread = entry.getKey();
                StackTraceElement[] stackTrace = entry.getValue();
                
                result.append("线程: ").append(thread.getName()).append("\n");
                result.append("  ID: ").append(thread.getId()).append("\n");
                result.append("  状态: ").append(thread.getState()).append("\n");
                result.append("  优先级: ").append(thread.getPriority()).append("\n");
                result.append("  守护: ").append(thread.isDaemon()).append("\n");
                result.append("  中断: ").append(thread.isInterrupted()).append("\n");
                
                if (stackTrace != null && stackTrace.length > 0) {
                    result.append("  堆栈:\n");
                    for (StackTraceElement element : stackTrace) {
                        result.append("    ").append(element.toString()).append("\n");
                    }
                }
                result.append("\n");
            }
            
            result.append("===== 检测结果 =====\n\n");
            result.append("检测到 ").append(blockedCount).append(" 个BLOCKED状态的线程\n");
            if (blockedCount > 0) {
                result.append("建议采取以下措施 (来自GLM 4.7 AI Assistant的忠告):\n");
                result.append("  1. 检查BLOCKED线程的堆栈跟踪，找出阻塞发生的位置\n");
                result.append("  2. 检查锁的获取顺序，确保所有线程以相同的顺序获取锁\n");
                result.append("  3. 使用 tryLock() 替代 lock()，避免无限等待\n");
                result.append("  4. 考虑使用超时机制，避免线程永久阻塞\n");
                result.append("  5. 如果可能，重构代码以减少锁的使用\n");
                result.append("  6. 使用android.os.Looper和Handler进行线程间通信\n");
            } else {
                result.append("当前没有检测到明显的死锁迹象\n");
            }
            
            logger.info("线程状态分析完成，发现 " + blockedCount + " 个BLOCKED线程");
            
        } catch (Exception e) {
            logger.error("线程状态分析失败", e);
            return "错误: " + e.getMessage();
        }
        
        return result.toString();
    }
}
