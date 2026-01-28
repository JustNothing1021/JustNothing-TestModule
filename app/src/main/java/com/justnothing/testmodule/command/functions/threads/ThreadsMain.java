package com.justnothing.testmodule.command.functions.threads;

import static com.justnothing.testmodule.constants.CommandServer.CMD_THREADS_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public class ThreadsMain extends CommandBase {

    public ThreadsMain() {
        super("Threads");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: threads <subcmd> [args...]
                
                线程管理和分析工具.
                
                子命令:
                    list [options]                                 - 列出所有线程及其状态
                    deadlock                                       - 检测Java应用程序中的死锁
                    profile <subcmd> [args...]                     - 性能分析
                    
                profile 子命令:
                    start [duration: seconds]                      - 开始性能分析, 默认60秒
                    stop                                           - 停止当前分析
                    show                                           - 显示分析结果
                    export <file>                                  - 导出分析结果到文件
                
                list 选项:
                    --id <thread_id>     - 只显示指定ID的线程
                    --name <thread_name> - 只显示指定名称的线程
                    --state <state>      - 只显示指定状态的线程 (BLOCKED, WAITING, RUNNABLE等)
                
                profile 选项:
                    duration    - 分析持续时间(秒), 默认60秒
                
                示例:
                    threads list
                    threads list --id 1
                    threads list --name main
                    threads list --state BLOCKED
                    threads deadlock
                    threads profile start
                    threads profile start 120
                    threads profile stop
                    threads profile show
                    threads profile export /sdcard/profile_report.txt
                
                注意:
                    - deadlock命令会检测所有线程的状态, 显示BLOCKED状态的线程
                    - profile命令会监控CPU, 内存, 线程等资源使用情况
                    - profile任务在后台运行, 不会阻塞其他命令执行
                    - 建议profile分析时间不要太长, 避免影响系统性能
                
                (Submodule threads %s)
                """, CMD_THREADS_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        if (args.length < 1) {
            return getHelpText();
        }

        String subCommand = args[0];

        try {
            return switch (subCommand) {
                case "list" -> handleList(args);
                case "deadlock" -> handleDeadlock();
                case "profile" -> handleProfile(args);
                default -> "未知子命令: " + subCommand + "\n" + getHelpText();
            };
        } catch (Exception e) {
            logger.error("执行threads命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String handleList(String[] args) {
        Long filterId = null;
        String filterName = null;
        Thread.State filterState = null;
        
        for (int i = 1; i < args.length; i++) {
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
                    return "错误: 无效的线程状态, 有效值: " + Arrays.toString(Thread.State.values());
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
            
            logger.info("线程信息查询完成");
            
        } catch (Exception e) {
            logger.error("获取线程信息失败", e);
            return "错误: " + e.getMessage();
        }
        
        return result.toString();
    }

    private String handleDeadlock() {
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
            
            result.append("===== 检测结果 =====\n\n");
            result.append("检测到 ").append(blockedCount).append(" 个BLOCKED状态的线程\n");
            if (blockedCount > 0) {
                result.append("建议采取以下措施 (来自GLM 4.7 AI Assistant的忠告):\n");
                result.append("  1. 检查BLOCKED线程的堆栈跟踪, 找出阻塞发生的位置\n");
                result.append("  2. 检查锁的获取顺序, 确保所有线程以相同的顺序获取锁\n");
                result.append("  3. 使用 tryLock() 替代 lock(), 避免无限等待\n");
                result.append("  4. 考虑使用超时机制, 避免线程永久阻塞\n");
                result.append("  5. 如果可能, 重构代码以减少锁的使用\n");
                result.append("  6. 使用android.os.Looper和Handler进行线程间通信\n");
            } else {
                result.append("当前没有检测到明显的死锁迹象\n");
            }
            
            logger.info("线程状态分析完成, 发现 " + blockedCount + " 个BLOCKED线程");
            
        } catch (Exception e) {
            logger.error("线程状态分析失败", e);
            return "错误: " + e.getMessage();
        }
        
        return result.toString();
    }

    private String handleProfile(String[] args) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: threads profile <subcmd> [args...]\n" + getHelpText();
        }

        String profileSubCommand = args[1];
        ProfileManager manager = ProfileManager.getInstance();

        try {
            return switch (profileSubCommand) {
                case "start" -> handleProfileStart(args, manager);
                case "stop" -> handleProfileStop(manager);
                case "show" -> handleProfileShow(manager);
                case "export" -> handleProfileExport(args, manager);
                default -> "未知子命令: " + profileSubCommand + "\n" + getHelpText();
            };
        } catch (Exception e) {
            logger.error("执行profile命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String handleProfileStart(String[] args, ProfileManager manager) {
        int duration = 60;
        
        if (args.length > 2) {
            try {
                duration = Integer.parseInt(args[2]);
                if (duration <= 0) {
                    return "错误: 持续时间必须大于0";
                }
            } catch (NumberFormatException e) {
                return "错误: 无效的持续时间: " + args[2];
            }
        }

        try {
            manager.startProfiling(duration);
            return "开始性能分析, 持续时间: " + duration + "秒";
        } catch (Exception e) {
            logger.error("开始性能分析失败", e);
            return "开始性能分析失败: " + e.getMessage();
        }
    }

    private String handleProfileStop(ProfileManager manager) {
        try {
            manager.stopProfiling();
            return "停止性能分析成功";
        } catch (Exception e) {
            logger.error("停止性能分析失败", e);
            return "停止性能分析失败: " + e.getMessage();
        }
    }

    private String handleProfileShow(ProfileManager manager) {
        try {
            return manager.getProfileReport();
        } catch (Exception e) {
            logger.error("获取分析结果失败", e);
            return "获取分析结果失败: " + e.getMessage();
        }
    }

    private String handleProfileExport(String[] args, ProfileManager manager) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: threads profile export <file>";
        }

        try {
            String filePath = args[2];
            boolean success = manager.exportToFile(filePath);
            if (success) {
                return "导出分析结果成功: " + filePath;
            } else {
                return "导出分析结果失败";
            }
        } catch (Exception e) {
            logger.error("导出分析结果失败", e);
            return "导出分析结果失败: " + e.getMessage();
        }
    }
}
