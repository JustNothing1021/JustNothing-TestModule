package com.justnothing.testmodule.command.functions.threads;

import static com.justnothing.testmodule.constants.CommandServer.CMD_THREADS_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.util.Map;

public class ThreadsMain extends CommandBase {

    public ThreadsMain() {
        super("Threads");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: threads [--id <thread_id>] [--name <thread_name>] [--state <state>]
                
                列出所有线程及其状态。
                
                选项:
                  --id <thread_id>    - 只显示指定ID的线程
                  --name <thread_name> - 只显示指定名称的线程
                  --state <state>     - 只显示指定状态的线程（BLOCKED, WAITING, RUNNABLE等）
                
                示例:
                  threads
                  threads --id 1
                  threads --name main
                  threads --state BLOCKED
                
                (Submodule threads %s)
                """, CMD_THREADS_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        Long filterId = null;
        String filterName = null;
        Thread.State filterState = null;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--id") && i + 1 < args.length) {
                try {
                    filterId = Long.parseLong(args[++i]);
                } catch (NumberFormatException e) {
                    return "错误: 线程ID必须是数字";
                }
            } else if (arg.equals("--name") && i + 1 < args.length) {
                filterName = args[++i];
            } else if (arg.equals("--state") && i + 1 < args.length) {
                try {
                    filterState = Thread.State.valueOf(args[++i]);
                } catch (IllegalArgumentException e) {
                    return "错误: 无效的线程状态。有效值: " + java.util.Arrays.toString(Thread.State.values());
                }
            }
        }
        
        StringBuilder result = new StringBuilder();
        
        try {
            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
            
            result.append("=== 线程信息 ===\n\n");
            result.append("线程总数: ").append(allStackTraces.size()).append("\n\n");
            
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
            
            result.append("=== 线程状态统计 ===\n\n");
            result.append("BLOCKED: ").append(blockedCount).append("\n");
            result.append("WAITING: ").append(waitingCount).append("\n");
            result.append("TIMED_WAITING: ").append(timedWaitingCount).append("\n");
            result.append("RUNNABLE: ").append(runnableCount).append("\n");
            result.append("TERMINATED: ").append(terminatedCount).append("\n");
            result.append("NEW: ").append(newStateCount).append("\n\n");
            
            result.append("=== 线程详情 ===\n\n");
            
            boolean found = false;
            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                Thread thread = entry.getKey();
                StackTraceElement[] stackTrace = entry.getValue();
                
                if (filterId != null && thread.getId() != filterId) {
                    continue;
                }
                if (filterName != null && !thread.getName().equals(filterName)) {
                    continue;
                }
                if (filterState != null && thread.getState() != filterState) {
                    continue;
                }
                
                found = true;
                result.append("线程: ").append(thread.getName()).append("\n");
                result.append("  ID: ").append(thread.getId()).append("\n");
                result.append("  状态: ").append(thread.getState()).append("\n");
                result.append("  优先级: ").append(thread.getPriority()).append("\n");
                result.append("  守护: ").append(thread.isDaemon() ? "是" : "否").append("\n");
                result.append("  中断: ").append(thread.isInterrupted() ? "是" : "否").append("\n");
                result.append("  是否存活: ").append(thread.isAlive() ? "是" : "否").append("\n");
                
                if (stackTrace != null && stackTrace.length > 0) {
                    result.append("  堆栈:\n");
                    for (StackTraceElement element : stackTrace) {
                        result.append("    ").append(element.toString()).append("\n");
                    }
                }
                result.append("\n");
            }
            
            if (!found && (filterId != null || filterName != null || filterState != null)) {
                result.append("未找到匹配的线程\n");
            }
            
            getLogger().info("线程信息查询完成");
            
        } catch (Exception e) {
            getLogger().error("获取线程信息失败", e);
            return "错误: " + e.getMessage();
        }
        
        return result.toString();
    }
}
