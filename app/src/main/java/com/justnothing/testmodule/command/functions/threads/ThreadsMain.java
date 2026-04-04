package com.justnothing.testmodule.command.functions.threads;

import static com.justnothing.testmodule.constants.CommandServer.CMD_THREADS_VER;


import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandArgumentParser;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

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
                case "list" -> handleList(args, context);
                case "deadlock" -> handleDeadlock(context);
                case "profile" -> handleProfile(args, context);
                default -> {
                    context.print("未知子命令: ", Colors.RED);
                    context.println(subCommand, Colors.YELLOW);
                    yield null;
                }
            };
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("threads", e, logger, "执行threads命令失败");
        }
    }

    private String handleList(String[] args, CommandExecutor.CmdExecContext ctx) {
        Long filterId = null;
        String filterName = null;
        Thread.State filterState = null;
        
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--id") && i + 1 < args.length) {
                try {
                    filterId = Long.parseLong(args[++i]);
                } catch (NumberFormatException e) {
                    ctx.print("错误: ", Colors.RED);
                    ctx.println("线程ID必须是数字", Colors.YELLOW);
                    return null;
                }
            } else if (arg.equals("--name") && i + 1 < args.length) {
                filterName = args[++i];
            } else if (arg.equals("--state") && i + 1 < args.length) {
                try {
                    filterState = Thread.State.valueOf(args[++i]);
                } catch (IllegalArgumentException e) {
                    ctx.print("错误: ", Colors.RED);
                    ctx.print("无效的线程状态, 有效值: ", Colors.GRAY);
                    ctx.println(Arrays.toString(Thread.State.values()), Colors.YELLOW);
                    return null;
                }
            }
        }
        
        try {
            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
            
            ctx.println("=== 线程信息 ===", Colors.CYAN);
            ctx.println("");
            ctx.print("线程总数: ", Colors.GRAY);
            ctx.println(String.valueOf(allStackTraces.size()), Colors.YELLOW);
            ctx.println("");
            
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
            
            ctx.println("=== 线程状态统计 ===", Colors.CYAN);
            ctx.println("");
            printStateCount(ctx, "BLOCKED", blockedCount, Colors.RED);
            printStateCount(ctx, "WAITING", waitingCount, Colors.YELLOW);
            printStateCount(ctx, "TIMED_WAITING", timedWaitingCount, Colors.PURPLE);
            printStateCount(ctx, "RUNNABLE", runnableCount, Colors.LIGHT_GREEN);
            printStateCount(ctx, "TERMINATED", terminatedCount, Colors.GRAY);
            printStateCount(ctx, "NEW", newStateCount, Colors.CYAN);
            ctx.println("");
            
            ctx.println("=== 线程详情 ===", Colors.CYAN);
            ctx.println("");
            
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
                printThreadInfo(ctx, thread, stackTrace);
            }
            
            if (!found && (filterId != null || filterName != null || filterState != null)) {
                ctx.println("未找到匹配的线程", Colors.GRAY);
            }
            
            logger.info("线程信息查询完成");
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("threads list", e, logger, "获取线程信息失败");
        }
        
        return null;
    }

    private byte getStateColor(Thread.State state) {
        return switch (state) {
            case RUNNABLE -> Colors.LIGHT_GREEN;
            case BLOCKED -> Colors.RED;
            case WAITING -> Colors.YELLOW;
            case TIMED_WAITING -> Colors.PURPLE;
            case TERMINATED -> Colors.GRAY;
            case NEW -> Colors.CYAN;
        };
    }

    private void printStateCount(CommandExecutor.CmdExecContext ctx, String stateName, int count, byte color) {
        ctx.print(stateName + ": ", Colors.GRAY);
        ctx.println(String.valueOf(count), color);
    }

    private void printThreadInfo(CommandExecutor.CmdExecContext ctx, Thread thread, StackTraceElement[] stackTrace) {
        ctx.print("线程: ", Colors.CYAN);
        ctx.println(thread.getName(), Colors.LIGHT_GREEN);
        
        ctx.print("  ID: ", Colors.GRAY);
        ctx.println(String.valueOf(thread.getId()), Colors.YELLOW);
        
        byte stateColor = getStateColor(thread.getState());
        ctx.print("  状态: ", Colors.GRAY);
        ctx.println(thread.getState().toString(), stateColor);
        
        ctx.print("  优先级: ", Colors.GRAY);
        ctx.println(String.valueOf(thread.getPriority()), Colors.LIGHT_GREEN);
        
        ctx.print("  守护: ", Colors.GRAY);
        ctx.println(thread.isDaemon() ? "是" : "否", thread.isDaemon() ? Colors.PURPLE : Colors.LIGHT_GREEN);
        
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
        ctx.print("(", Colors.PURPLE);
        
        if (fileName != null) {
            ctx.print(fileName, Colors.CYAN);
            if (lineNumber >= 0) {
                ctx.print(":", Colors.GRAY);
                ctx.print(String.valueOf(lineNumber), Colors.YELLOW);
            }
        } else {
            ctx.print("Unknown Source", Colors.GRAY);
        }
        ctx.println(")", Colors.PURPLE);
    }

    private String handleDeadlock(CommandExecutor.CmdExecContext ctx) {
        try {
            ctx.println("===== 线程状态分析 =====", Colors.CYAN);
            ctx.print("时间: ", Colors.GRAY);
            ctx.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), Colors.YELLOW);
            ctx.println("");
            
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
            
            ctx.println("===== 线程状态统计 =====", Colors.CYAN);
            ctx.println("");
            ctx.print("线程总数: ", Colors.GRAY);
            ctx.println(String.valueOf(allStackTraces.size()), Colors.YELLOW);
            printStateCount(ctx, " BLOCKED", blockedCount, Colors.RED);
            printStateCount(ctx, " WAITING", waitingCount, Colors.YELLOW);
            printStateCount(ctx, " TIMED_WAITING", timedWaitingCount, Colors.PURPLE);
            printStateCount(ctx, " RUNNABLE", runnableCount, Colors.LIGHT_GREEN);
            printStateCount(ctx, " TERMINATED", terminatedCount, Colors.GRAY);
            printStateCount(ctx, " NEW", newStateCount, Colors.CYAN);
            ctx.println("");
            
            if (blockedCount > 0) {
                ctx.println("===== 可能的死锁线程 =====", Colors.CYAN);
                ctx.println("");
                ctx.println("Tip: Android不提供完整的死锁检测API, 此命令只基于基本的线程状态分析", Colors.GRAY);
                ctx.println("以下BLOCKED状态的线程可能存在死锁:", Colors.GRAY);
                ctx.println("");
                
                for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                    Thread thread = entry.getKey();
                    if (thread.getState() == Thread.State.BLOCKED) {
                        StackTraceElement[] stackTrace = entry.getValue();
                        
                        ctx.print("线程: ", Colors.CYAN);
                        ctx.println(thread.getName(), Colors.LIGHT_GREEN);
                        ctx.print("  ID: ", Colors.GRAY);
                        ctx.println(String.valueOf(thread.getId()), Colors.YELLOW);
                        ctx.print("  状态: ", Colors.GRAY);
                        ctx.println(thread.getState().toString(), Colors.RED);
                        ctx.print("  优先级: ", Colors.GRAY);
                        ctx.println(String.valueOf(thread.getPriority()), Colors.LIGHT_GREEN);
                        ctx.print("  守护: ", Colors.GRAY);
                        ctx.println(String.valueOf(thread.isDaemon()), thread.isDaemon() ? Colors.PURPLE : Colors.LIGHT_GREEN);
                        ctx.print("  中断: ", Colors.GRAY);
                        ctx.println(String.valueOf(thread.isInterrupted()), thread.isInterrupted() ? Colors.RED : Colors.LIGHT_GREEN);
                        ctx.println("");
                        
                        if (stackTrace != null && stackTrace.length > 0) {
                            ctx.print("  堆栈跟踪:", Colors.GRAY);
                            ctx.println("");
                            for (StackTraceElement element : stackTrace) {
                                ctx.print("    ", Colors.DEFAULT);
                                printStackTraceElement(ctx, element);
                            }
                        }
                        ctx.println("");
                    }
                }
            } else {
                ctx.println("未检测到BLOCKED状态的线程, 应该是没有死锁的", Colors.LIGHT_GREEN);
                ctx.println("");
            }
            
            ctx.println("===== 检测结果 =====", Colors.CYAN);
            ctx.println("");
            ctx.print("检测到 ", Colors.GRAY);
            ctx.print(String.valueOf(blockedCount), blockedCount > 0 ? Colors.RED : Colors.LIGHT_GREEN);
            ctx.println(" 个BLOCKED状态的线程", Colors.GRAY);
            
            if (blockedCount > 0) {
                ctx.println("建议采取以下措施 (来自GLM 4.7 AI Assistant的忠告):", Colors.YELLOW);
                ctx.println("  1. 检查BLOCKED线程的堆栈跟踪, 找出阻塞发生的位置", Colors.GRAY);
                ctx.println("  2. 检查锁的获取顺序, 确保所有线程以相同的顺序获取锁", Colors.GRAY);
                ctx.println("  3. 使用 tryLock() 替代 lock(), 避免无限等待", Colors.GRAY);
                ctx.println("  4. 考虑使用超时机制, 避免线程永久阻塞", Colors.GRAY);
                ctx.println("  5. 如果可能, 重构代码以减少锁的使用", Colors.GRAY);
                ctx.println("  6. 使用android.os.Looper和Handler进行线程间通信", Colors.GRAY);
            } else {
                ctx.println("当前没有检测到明显的死锁迹象", Colors.LIGHT_GREEN);
            }
            
            logger.info("线程状态分析完成, 发现 " + blockedCount + " 个BLOCKED线程");
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("threads deadlock", e, logger, "线程状态分析失败");
        }
        
        return null;
    }

    private String handleProfile(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 2) {
            ctx.print("错误: ", Colors.RED);
            ctx.println("参数不足", Colors.YELLOW);
            ctx.println("用法: threads profile <subcmd> [args...]", Colors.GRAY);
            return null;
        }

        String profileSubCommand = args[1];
        ProfileManager manager = ProfileManager.getInstance();

        try {
            return switch (profileSubCommand) {
                case "start" -> handleProfileStart(args, manager, ctx);
                case "stop" -> handleProfileStop(manager, ctx);
                case "show" -> handleProfileShow(manager, ctx);
                case "export" -> handleProfileExport(args, manager, ctx);
                default -> {
                    ctx.print("未知子命令: ", Colors.RED);
                    ctx.println(profileSubCommand, Colors.YELLOW);
                    yield null;
                }
            };
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("threads profile", e, logger, "执行profile命令失败");
        }
    }

    private String handleProfileStart(String[] args, ProfileManager manager, CommandExecutor.CmdExecContext ctx) {
        int duration = 60;
        
        if (args.length > 2) {
            try {
                duration = CommandArgumentParser.parseInt(args, 2, "持续时间");
                CommandArgumentParser.requireMin(duration, 1, "持续时间");
            } catch (IllegalArgumentException e) {
                ctx.print("错误: ", Colors.RED);
                ctx.println(e.getMessage(), Colors.YELLOW);
                return null;
            }
        }

        try {
            manager.startProfiling(duration);
            ctx.print("开始性能分析, 持续时间: ", Colors.LIGHT_GREEN);
            ctx.print(String.valueOf(duration), Colors.YELLOW);
            ctx.println("秒", Colors.GRAY);
            return null;
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("threads profile start", e, logger, "开始性能分析失败");
        }
    }

    private String handleProfileStop(ProfileManager manager, CommandExecutor.CmdExecContext ctx) {
        try {
            manager.stopProfiling();
            ctx.println("停止性能分析成功", Colors.LIGHT_GREEN);
            return null;
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("threads profile stop", e, logger, "停止性能分析失败");
        }
    }

    private String handleProfileShow(ProfileManager manager, CommandExecutor.CmdExecContext ctx) {
        try {
            ctx.println(manager.getProfileReport(), Colors.DEFAULT);
            return null;
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("threads profile show", e, logger, "获取分析结果失败");
        }
    }

    private String handleProfileExport(String[] args, ProfileManager manager, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 3) {
            ctx.print("错误: ", Colors.RED);
            ctx.println("参数不足", Colors.YELLOW);
            ctx.println("用法: threads profile export <file>", Colors.GRAY);
            return null;
        }

        try {
            String filePath = args[2];
            boolean success = manager.exportToFile(filePath);
            if (success) {
                ctx.print("导出分析结果成功: ", Colors.LIGHT_GREEN);
                ctx.println(filePath, Colors.CYAN);
            } else {
                ctx.println("导出分析结果失败", Colors.RED);
            }
            return null;
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("threads profile export", e, logger, "导出分析结果失败");
        }
    }
}
