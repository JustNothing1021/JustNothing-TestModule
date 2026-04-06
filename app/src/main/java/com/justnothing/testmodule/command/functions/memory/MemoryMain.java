package com.justnothing.testmodule.command.functions.memory;

import static com.justnothing.testmodule.constants.CommandServer.CMD_MEMORY_VER;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MemoryMain extends CommandBase {

    private final String commandName;

    public MemoryMain() {
        super("Memory");
        this.commandName = "memory";
    }

    public MemoryMain(String commandName) {
        super("Memory");
        this.commandName = commandName;
    }

    @Override
    public String getHelpText() {
        return switch (commandName) {
            case "minfo" -> String.format("""
                    语法: minfo [options]
                    
                    显示详细的内存使用情况.
                    
                    选项:
                        -h, --heap       只显示堆内存信息
                        -d, --detailed   显示详细内存信息 (默认)
                    
                    示例:
                        minfo
                        minfo -h
                        minfo -d
                    
                    (Submodule memory %s)
                    """, CMD_MEMORY_VER);
            case "mgc" -> String.format("""
                    语法: mgc [options]
                    
                    手动触发垃圾回收.
                    
                    选项:
                        --full    - 执行完整的GC
                        --stats   - 显示GC统计信息
                    
                    示例:
                        mgc
                        mgc --full
                        mgc --stats
                    
                    (Submodule memory %s)
                    """, CMD_MEMORY_VER);
            case "mdump" -> String.format("""
                    语法: mdump [options] [file]
                    
                    导出堆信息和系统状态.
                    
                    选项:
                        --heap            - 只导出堆信息
                        --threads         - 只导出线程信息
                        --full            - 导出完整信息 (默认)
                    
                    示例:
                        mdump
                        mdump /sdcard/heap_dump.txt
                        mdump --heap /sdcard/heap_only.txt
                        mdump --full /sdcard/full_dump.txt
                    
                    (Submodule memory %s)
                    """, CMD_MEMORY_VER);
            default -> String.format("""
                    语法: memory <subcmd> [args...]
                    
                    内存调试和管理工具.
                    
                    子命令:
                        info [options]                     - 显示详细的内存使用情况
                        gc [options]                       - 手动触发垃圾回收 (建议开full)
                        dump [options]                     - 导出堆信息和系统状态
                    
                    info 选项:
                        -h, --heap       只显示堆内存信息
                        -d, --detailed   显示详细内存信息 (默认)
                    
                    gc 选项:
                        --full    - 执行完整的GC
                        --stats   - 显示GC统计信息
                    
                    dump 选项:
                        --heap            - 只导出堆信息
                        --threads         - 只导出线程信息
                        --full            - 导出完整信息 (默认)
                    
                    快捷命令:
                        minfo    - 等同于 memory info
                        mgc       - 等同于 memory gc
                        mdump     - 等同于 memory dump
                    
                    示例:
                        memory info
                        memory info -h
                        memory gc
                        memory gc --full
                        memory gc --stats
                        memory dump
                        memory dump /sdcard/heap_dump.txt
                        memory dump --heap /sdcard/heap_only.txt
                        memory dump --full /sdcard/full_dump.txt
                    
                    (Submodule memory %s)
                    """, CMD_MEMORY_VER);
        };
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String cmdName = context.cmdName();
        String[] args = context.args();
        
        if (args.length < 1 && !cmdName.equals("minfo") && !cmdName.equals("mgc") && !cmdName.equals("mdump")) {
            return getHelpText();
        }

        String subCommand;
        String[] subArgs;

        switch (cmdName) {
            case "minfo" -> {
                subCommand = "info";
                subArgs = args;
            }
            case "mgc" -> {
                subCommand = "gc";
                subArgs = args;
            }
            case "mdump" -> {
                subCommand = "dump";
                subArgs = args;
            }
            default -> {
                subCommand = args[0];
                subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
            }
        }

        try {
            return switch (subCommand) {
                case "info" -> handleInfo(subArgs, context);
                case "gc" -> handleGc(subArgs, context);
                case "dump" -> handleDump(subArgs, context);
                default -> {
                    context.print("未知子命令: ", Colors.RED);
                    context.println(subCommand, Colors.YELLOW);
                    yield null;
                }
            };
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("memory", e, context, "执行memory命令失败");
        }
    }

    private String handleInfo(String[] args, CommandExecutor.CmdExecContext ctx) {
        boolean showHeapOnly = false;

        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("--heap")) {
                showHeapOnly = true;
            } else if (arg.equals("-d") || arg.equals("--detailed")) {
                showHeapOnly = false;
            }
        }
        
        try {
            Context appContext = getApplicationContext();
            if (appContext == null) {
                logger.warn("无法获取应用上下文，部分信息可能不可用");
            }
            
            if (showHeapOnly) {
                ctx.println("===== 堆内存信息 =====", Colors.CYAN);
                ctx.println("");
                
                ctx.println("===== 原生堆内存 =====", Colors.CYAN);
                ctx.println("");
                printMemoryValue(ctx, "已分配: ", Debug.getNativeHeapAllocatedSize());
                printMemoryValue(ctx, "已用: ", Debug.getNativeHeapSize());
                printMemoryValue(ctx, "空闲: ", Debug.getNativeHeapFreeSize());
                ctx.println("");
                
                ctx.println("===== Java运行时内存 =====", Colors.CYAN);
                ctx.println("");
                Runtime runtime = Runtime.getRuntime();
                printMemoryValue(ctx, "最大内存: ", runtime.maxMemory());
                printMemoryValue(ctx, "已分配内存: ", runtime.totalMemory());
                printMemoryValue(ctx, "空闲内存: ", runtime.freeMemory());
                printMemoryValue(ctx, "已用内存: ", runtime.totalMemory() - runtime.freeMemory());
            } else {
                ctx.println("===== 详细内存信息 =====", Colors.CYAN);
                ctx.println("");
                
                ctx.println("===== Java运行时内存 =====", Colors.CYAN);
                ctx.println("");
                Runtime runtime = Runtime.getRuntime();
                printMemoryValue(ctx, "最大内存: ", runtime.maxMemory());
                printMemoryValue(ctx, "已分配内存: ", runtime.totalMemory());
                printMemoryValue(ctx, "空闲内存: ", runtime.freeMemory());
                
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                printMemoryValue(ctx, "已用内存: ", usedMemory);
                
                if (runtime.maxMemory() > 0) {
                    double percent = (double) usedMemory / runtime.maxMemory() * 100;
                    ctx.print("使用率: ", Colors.GRAY);
                    ctx.print(String.format(Locale.US, "%.2f", percent), getPercentColor(percent));
                    ctx.println("%", Colors.GRAY);
                }
                ctx.println("");
                
                ctx.println("===== 原生堆内存 =====", Colors.CYAN);
                ctx.println("");
                printMemoryValue(ctx, "已分配: ", Debug.getNativeHeapAllocatedSize());
                printMemoryValue(ctx, "已用: ", Debug.getNativeHeapSize());
                printMemoryValue(ctx, "空闲: ", Debug.getNativeHeapFreeSize());
                ctx.println("");
                
                if (appContext != null) {
                    ctx.println("===== 进程内存 =====", Colors.CYAN);
                    ctx.println("");
                    ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
                    if (activityManager != null) {
                        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                        activityManager.getMemoryInfo(memoryInfo);
                        
                        printMemoryValue(ctx, "可用内存: ", memoryInfo.availMem);
                        printMemoryValue(ctx, "总内存: ", memoryInfo.totalMem);
                        printMemoryValue(ctx, "内存阈值: ", memoryInfo.threshold);
                        ctx.print("低内存状态: ", Colors.GRAY);
                        ctx.println(memoryInfo.lowMemory ? "是" : "否", memoryInfo.lowMemory ? Colors.RED : Colors.LIGHT_GREEN);
                        ctx.println("");
                        
                        int pid = android.os.Process.myPid();
                        Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{pid});
                        if (memoryInfos.length > 0) {
                            Debug.MemoryInfo processMemory = memoryInfos[0];
                            ctx.println("当前进程内存:", Colors.CYAN);
                            printMemoryValue(ctx, "  PSS: ", processMemory.getTotalPss() * 1024L);
                            printMemoryValue(ctx, "  USS: ", processMemory.getTotalPrivateDirty() * 1024L);
                            printMemoryValue(ctx, "  RSS: ", processMemory.getTotalSharedDirty() * 1024L);
                            ctx.println("");
                        }
                    }
                }
                
                ctx.println("===== 系统内存 =====", Colors.CYAN);
                ctx.println("");
                printMeminfoColored(ctx);
                
                ctx.println("===== 进程内存统计 =====", Colors.CYAN);
                ctx.println("");
                printProcessMemoryStatsColored(ctx);
            }
            
            logger.info("内存信息查询完成");
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("memory info", e, ctx, "获取内存信息失败");
        }
        
        return null;
    }

    private byte getPercentColor(double percent) {
        if (percent < 50) {
            return Colors.LIGHT_GREEN;
        } else if (percent < 80) {
            return Colors.YELLOW;
        } else {
            return Colors.RED;
        }
    }

    private void printMemoryValue(CommandExecutor.CmdExecContext ctx, String label, long bytes) {
        ctx.print(label, Colors.GRAY);
        printBytes(ctx, bytes);
        ctx.println("", Colors.DEFAULT);
    }

    private void printBytes(CommandExecutor.CmdExecContext ctx, long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        String formatted;
        if (unitIndex == 0) {
            formatted = String.valueOf((long) size);
        } else {
            formatted = String.format(Locale.US, "%.2f", size);
        }
        
        ctx.print(formatted, Colors.YELLOW);
        ctx.print(" " + units[unitIndex], Colors.CYAN);
    }

    private void printMemoryWithPercent(CommandExecutor.CmdExecContext ctx, String label, long used, long total) {
        ctx.print(label, Colors.GRAY);
        printBytes(ctx, used);
        ctx.print(" / ", Colors.GRAY);
        printBytes(ctx, total);
        
        if (total > 0) {
            double percent = (double) used / total * 100;
            ctx.print(" (", Colors.GRAY);
            ctx.print(String.format(Locale.US, "%.1f", percent), getPercentColor(percent));
            ctx.print("%)", Colors.GRAY);
        }
        ctx.println("", Colors.DEFAULT);
    }

    private String handleGc(String[] args, CommandExecutor.CmdExecContext ctx) {
        boolean fullGc = false;
        boolean showStats = false;
        
        for (String arg : args) {
            if (arg.equals("--full")) {
                fullGc = true;
            } else if (arg.equals("--stats")) {
                showStats = true;
            }
        }
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            ctx.println("===== 垃圾回收 =====", Colors.CYAN);
            ctx.println("");
            
            long beforeUsed = runtime.totalMemory() - runtime.freeMemory();
            long beforeTotal = runtime.totalMemory();
            long beforeMax = runtime.maxMemory();
            
            ctx.print("GC前堆内存: ", Colors.GRAY);
            printBytes(ctx, beforeUsed);
            ctx.print(" / ", Colors.GRAY);
            printBytes(ctx, beforeTotal);
            if (beforeMax > 0) {
                double percent = (double) beforeUsed / beforeMax * 100;
                ctx.print(" (", Colors.GRAY);
                ctx.print(String.format(Locale.US, "%.1f", percent), getPercentColor(percent));
                ctx.print("%)", Colors.GRAY);
            }
            ctx.println("", Colors.DEFAULT);
            ctx.println("");
            
            if (fullGc) {
                logger.info("执行完整GC");
                System.gc();
                System.runFinalization();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.gc();
            } else {
                logger.info("执行GC");
                System.gc();
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            long afterUsed = runtime.totalMemory() - runtime.freeMemory();
            long afterTotal = runtime.totalMemory();
            long afterMax = runtime.maxMemory();
            
            ctx.print("GC后堆内存: ", Colors.GRAY);
            printBytes(ctx, afterUsed);
            ctx.print(" / ", Colors.GRAY);
            printBytes(ctx, afterTotal);
            if (afterMax > 0) {
                double percent = (double) afterUsed / afterMax * 100;
                ctx.print(" (", Colors.GRAY);
                ctx.print(String.format(Locale.US, "%.1f", percent), getPercentColor(percent));
                ctx.print("%)", Colors.GRAY);
            }
            ctx.println("", Colors.DEFAULT);
            ctx.println("");
            
            long freed = beforeUsed - afterUsed;
            if (freed > 0) {
                ctx.print("释放内存: ", Colors.LIGHT_GREEN);
                printBytes(ctx, freed);
                ctx.println("", Colors.DEFAULT);
            } else if (freed < 0) {
                ctx.print("内存增加: ", Colors.RED);
                printBytes(ctx, -freed);
                ctx.println(" (?)", Colors.GRAY);
                ctx.println("可以试试 memory gc --full", Colors.YELLOW);
            } else {
                ctx.println("内存未变化", Colors.GRAY);
            }
            
            if (showStats) {
                ctx.println("");
                ctx.println("===== GC统计信息 =====", Colors.CYAN);
                ctx.println("");
                ctx.println("Tip: Android不提供详细的GC统计信息", Colors.GRAY);
                ctx.println("以下是内存使用统计:", Colors.GRAY);
                ctx.println("");
                
                ctx.println("Java堆内存:", Colors.CYAN);
                printMemoryValue(ctx, "  最大: ", afterMax);
                printMemoryValue(ctx, "  已分配: ", afterTotal);
                printMemoryValue(ctx, "  已用: ", afterUsed);
                printMemoryValue(ctx, "  空闲: ", runtime.freeMemory());
                ctx.println("");
                
                Context appContext = getApplicationContext();
                if (appContext != null) {
                    ActivityManager activityManager = 
                        (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
                    if (activityManager != null) {
                        ActivityManager.MemoryInfo memoryInfo = 
                            new ActivityManager.MemoryInfo();
                        activityManager.getMemoryInfo(memoryInfo);
                        
                        ctx.println("系统内存:", Colors.CYAN);
                        printMemoryValue(ctx, "  可用: ", memoryInfo.availMem);
                        printMemoryValue(ctx, "  总计: ", memoryInfo.totalMem);
                        ctx.print("  低内存: ", Colors.GRAY);
                        ctx.println(memoryInfo.lowMemory ? "是" : "否", memoryInfo.lowMemory ? Colors.RED : Colors.LIGHT_GREEN);
                        ctx.println("");
                    }
                }
                
                ctx.println("进程内存统计:", Colors.CYAN);
                printProcessMemoryStatsColored(ctx);
            }
            
            logger.info("垃圾回收完成");
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("memory gc", e, ctx, "垃圾回收失败");
        }
        
        return null;
    }

    private String handleDump(String[] args, CommandExecutor.CmdExecContext ctx) {
        boolean dumpHeap = false;
        boolean dumpThreads = false;
        boolean dumpFull = true;
        String filePath = null;

        for (String arg : args) {
            if (arg.equals("--heap")) {
                dumpHeap = true;
                dumpFull = false;
            } else if (arg.equals("--threads")) {
                dumpThreads = true;
                dumpFull = false;
            } else if (arg.equals("--full")) {
                dumpFull = true;
            } else if (!arg.startsWith("--")) {
                filePath = arg;
            }
        }
        
        if (filePath != null) {
            StringBuilder output = new StringBuilder();
            
            output.append("=== 系统堆转储 ===\n");
            output.append("时间: ").append(new Date()).append("\n");
            output.append("文件: ").append(filePath).append("\n\n");
            
            if (dumpHeap || dumpFull) {
                dumpHeapInfo(output);
            }
            
            if (dumpThreads || dumpFull) {
                dumpThreadInfo(output);
            }
            
            if (dumpFull) {
                dumpSystemInfo(output);
            }
            
            File outputFile = new File(filePath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                IOManager.createDirectory(parentDir.getAbsolutePath());
            }
            
            try {
                logger.info("开始导出堆信息到: " + filePath);
                IOManager.writeFile(outputFile.getAbsolutePath(), output.toString());
                logger.info("堆信息导出完成");
                ctx.print("堆信息已导出到: ", Colors.LIGHT_GREEN);
                ctx.println(filePath, Colors.CYAN);
                return null;
            } catch (IOException e) {
                logger.error("导出堆信息失败", e);
                ctx.print("错误: ", Colors.RED);
                ctx.println(e.getMessage(), Colors.YELLOW);
                return null;
            }
        } else {
            ctx.println("=== 系统堆转储 ===", Colors.CYAN);
            ctx.print("时间: ", Colors.GRAY);
            ctx.println(new Date().toString(), Colors.YELLOW);
            ctx.println("");
            
            if (dumpHeap || dumpFull) {
                dumpHeapInfoColored(ctx);
            }
            
            if (dumpThreads || dumpFull) {
                dumpThreadInfoColored(ctx);
            }
            
            if (dumpFull) {
                dumpSystemInfoColored(ctx);
            }
            
            return null;
        }
    }

    private void dumpHeapInfoColored(CommandExecutor.CmdExecContext ctx) {
        ctx.println("=== 堆内存信息 ===", Colors.CYAN);
        ctx.println("");
        
        Runtime runtime = Runtime.getRuntime();
        ctx.println("Java运行时内存:", Colors.CYAN);
        printMemoryValue(ctx, "  最大: ", runtime.maxMemory());
        printMemoryValue(ctx, "  已分配: ", runtime.totalMemory());
        printMemoryValue(ctx, "  空闲: ", runtime.freeMemory());
        printMemoryValue(ctx, "  已用: ", runtime.totalMemory() - runtime.freeMemory());
        ctx.println("");
        
        ctx.println("原生堆内存:", Colors.CYAN);
        printMemoryValue(ctx, "  已分配: ", Debug.getNativeHeapAllocatedSize());
        printMemoryValue(ctx, "  已用: ", Debug.getNativeHeapSize());
        printMemoryValue(ctx, "  空闲: ", Debug.getNativeHeapFreeSize());
        ctx.println("");
        
        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);
                
                ctx.println("系统内存:", Colors.CYAN);
                printMemoryValue(ctx, "  可用: ", memoryInfo.availMem);
                printMemoryValue(ctx, "  总计: ", memoryInfo.totalMem);
                printMemoryValue(ctx, "  阈值: ", memoryInfo.threshold);
                ctx.print("  低内存: ", Colors.GRAY);
                ctx.println(memoryInfo.lowMemory ? "是" : "否", memoryInfo.lowMemory ? Colors.RED : Colors.LIGHT_GREEN);
                ctx.println("");
            }
        }
        
        ctx.println("=== 内存详细信息 ===", Colors.CYAN);
        ctx.println("");
        printMeminfoColored(ctx);
    }
    
    private void dumpThreadInfoColored(CommandExecutor.CmdExecContext ctx) {
        ctx.println("=== 线程信息 ===", Colors.CYAN);
        ctx.println("");
        
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        ctx.print("线程总数: ", Colors.GRAY);
        ctx.println(String.valueOf(allStackTraces.size()), Colors.YELLOW);
        ctx.println("");
        
        ctx.println("=== 线程详情 ===", Colors.CYAN);
        ctx.println("");
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();
            
            ctx.print("线程: ", Colors.CYAN);
            ctx.println(thread.getName(), Colors.LIGHT_GREEN);
            ctx.print("  ID: ", Colors.GRAY);
            ctx.println(String.valueOf(thread.getId()), Colors.YELLOW);
            ctx.print("  状态: ", Colors.GRAY);
            ctx.println(thread.getState().toString(), Colors.LIGHT_GREEN);
            ctx.print("  优先级: ", Colors.GRAY);
            ctx.println(String.valueOf(thread.getPriority()), Colors.YELLOW);
            ctx.print("  守护: ", Colors.GRAY);
            ctx.println(thread.isDaemon() ? "是" : "否", thread.isDaemon() ? Colors.PURPLE : Colors.LIGHT_GREEN);
            ctx.print("  中断: ", Colors.GRAY);
            ctx.println(thread.isInterrupted() ? "是" : "否", thread.isInterrupted() ? Colors.RED : Colors.LIGHT_GREEN);
            
            if (stackTrace != null && stackTrace.length > 0) {
                ctx.print("  堆栈:", Colors.GRAY);
                ctx.println("");
                for (StackTraceElement element : stackTrace) {
                    ctx.print("    ", Colors.DEFAULT);
                    ctx.println(element.toString(), Colors.GRAY);
                }
            }
            ctx.println("");
        }
    }
    
    private void dumpSystemInfoColored(CommandExecutor.CmdExecContext ctx) {
        ctx.println("=== 系统信息 ===", Colors.CYAN);
        ctx.println("");
        
        ctx.print("操作系统: ", Colors.GRAY);
        ctx.println(System.getProperty("os.name"), Colors.YELLOW);
        ctx.print("系统版本: ", Colors.GRAY);
        ctx.println(System.getProperty("os.version"), Colors.YELLOW);
        ctx.print("架构: ", Colors.GRAY);
        ctx.println(System.getProperty("os.arch"), Colors.YELLOW);
        ctx.print("处理器数: ", Colors.GRAY);
        ctx.println(String.valueOf(Runtime.getRuntime().availableProcessors()), Colors.YELLOW);
        ctx.print("Java版本: ", Colors.GRAY);
        ctx.println(System.getProperty("java.version"), Colors.YELLOW);
        ctx.print("Java供应商: ", Colors.GRAY);
        ctx.println(System.getProperty("java.vendor"), Colors.YELLOW);
        ctx.print("Java虚拟机: ", Colors.GRAY);
        ctx.println(System.getProperty("java.vm.name"), Colors.YELLOW);
        ctx.print("Java虚拟机版本: ", Colors.GRAY);
        ctx.println(System.getProperty("java.vm.version"), Colors.YELLOW);
        ctx.println("");
    }

    private static void dumpHeapInfo(StringBuilder output) {
        output.append("=== 堆内存信息 ===\n\n");
        
        Runtime runtime = Runtime.getRuntime();
        output.append("Java运行时内存:\n");
        output.append("  最大: ").append(formatBytes(runtime.maxMemory())).append("\n");
        output.append("  已分配: ").append(formatBytes(runtime.totalMemory())).append("\n");
        output.append("  空闲: ").append(formatBytes(runtime.freeMemory())).append("\n");
        output.append("  已用: ").append(formatBytes(runtime.totalMemory() - runtime.freeMemory())).append("\n\n");
        
        output.append("原生堆内存:\n");
        output.append("  已分配: ").append(formatBytes(Debug.getNativeHeapAllocatedSize())).append("\n");
        output.append("  已用: ").append(formatBytes(Debug.getNativeHeapSize())).append("\n");
        output.append("  空闲: ").append(formatBytes(Debug.getNativeHeapFreeSize())).append("\n\n");
        
        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);
                
                output.append("系统内存:\n");
                output.append("  可用: ").append(formatBytes(memoryInfo.availMem)).append("\n");
                output.append("  总计: ").append(formatBytes(memoryInfo.totalMem)).append("\n");
                output.append("  阈值: ").append(formatBytes(memoryInfo.threshold)).append("\n");
                output.append("  低内存: ").append(memoryInfo.lowMemory ? "是" : "否").append("\n\n");
                
                int pid = android.os.Process.myPid();
                android.os.Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{pid});
                if (memoryInfos.length > 0) {
                    android.os.Debug.MemoryInfo processMemory = memoryInfos[0];
                    output.append("当前进程内存:\n");
                    output.append("  PSS: ").append(formatBytes(processMemory.getTotalPss() * 1024L)).append("\n");
                    output.append("  USS: ").append(formatBytes(processMemory.getTotalPrivateDirty() * 1024L)).append("\n");
                    output.append("  RSS: ").append(formatBytes(processMemory.getTotalSharedDirty() * 1024L)).append("\n\n");
                }
            }
        }
        
        output.append("=== 内存详细信息 ===\n\n");
        output.append(readMeminfo());
    }
    
    private static void dumpThreadInfo(StringBuilder output) {
        output.append("=== 线程信息 ===\n\n");
        
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        output.append("线程总数: ").append(allStackTraces.size()).append("\n\n");
        
        output.append("=== 线程详情 ===\n\n");
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();
            
            output.append("线程: ").append(thread.getName()).append("\n");
            output.append("  ID: ").append(thread.getId()).append("\n");
            output.append("  状态: ").append(thread.getState()).append("\n");
            output.append("  优先级: ").append(thread.getPriority()).append("\n");
            output.append("  守护: ").append(thread.isDaemon()).append("\n");
            output.append("  中断: ").append(thread.isInterrupted()).append("\n");
            
            if (stackTrace != null && stackTrace.length > 0) {
                output.append("  堆栈:\n");
                for (StackTraceElement element : stackTrace) {
                    output.append("    ").append(element.toString()).append("\n");
                }
            }
            output.append("\n");
        }
    }
    
    private static void dumpSystemInfo(StringBuilder output) {
        output.append("=== 系统信息 ===\n\n");
        
        output.append("操作系统: ").append(System.getProperty("os.name")).append("\n");
        output.append("系统版本: ").append(System.getProperty("os.version")).append("\n");
        output.append("架构: ").append(System.getProperty("os.arch")).append("\n");
        output.append("处理器数: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        output.append("Java版本: ").append(System.getProperty("java.version")).append("\n");
        output.append("Java供应商: ").append(System.getProperty("java.vendor")).append("\n");
        output.append("Java虚拟机: ").append(System.getProperty("java.vm.name")).append("\n");
        output.append("Java虚拟机版本: ").append(System.getProperty("java.vm.version")).append("\n\n");
        
        output.append("=== 系统属性 ===\n\n");
        output.append("ro.product.model: ").append(System.getProperty("ro.product.model")).append("\n");
        output.append("ro.build.version.release: ").append(System.getProperty("ro.build.version.release")).append("\n");
        output.append("ro.build.version.sdk: ").append(System.getProperty("ro.build.version.sdk")).append("\n\n");
        
        output.append("=== JVM参数 ===\n\n");
        output.append("Android不提供JVM参数, 以下是系统属性:\n\n");
        
        output.append("java.vm.name: ").append(System.getProperty("java.vm.name")).append("\n");
        output.append("java.vm.version: ").append(System.getProperty("java.vm.version")).append("\n");
        output.append("java.specification.version: ").append(System.getProperty("java.specification.version")).append("\n");
        output.append("java.specification.vendor: ").append(System.getProperty("java.specification.vendor")).append("\n");
        output.append("java.specification.name: ").append(System.getProperty("java.specification.name")).append("\n");
    }
    
    private static String readMeminfo() {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 20) {
                result.append(line).append("\n");
                count++;
            }
        } catch (IOException e) {
            result.append("无法读取 /proc/meminfo: ").append(e.getMessage()).append("\n");
        }
        return result.toString();
    }
    
    private static String readProcessMemoryStats() {
        StringBuilder result = new StringBuilder();
        try {
            int pid = android.os.Process.myPid();
            try (BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/statm"))) {
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 7) {
                        long pageSize = 4096;
                        long size = Long.parseLong(parts[0]) * pageSize;
                        long resident = Long.parseLong(parts[1]) * pageSize;
                        long shared = Long.parseLong(parts[2]) * pageSize;
                        long text = Long.parseLong(parts[3]) * pageSize;
                        long data = Long.parseLong(parts[5]) * pageSize;
                        
                        result.append("总大小: ").append(formatBytes(size)).append("\n");
                        result.append("驻留内存: ").append(formatBytes(resident)).append("\n");
                        result.append("共享内存: ").append(formatBytes(shared)).append("\n");
                        result.append("代码段: ").append(formatBytes(text)).append("\n");
                        result.append("数据段: ").append(formatBytes(data)).append("\n");
                    }
                }
            }
        } catch (IOException e) {
            result.append("无法读取进程内存统计: ").append(e.getMessage()).append("\n");
        }
        return result.toString();
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private static String formatPercent(long used, long max) {
        if (max > 0) {
            DecimalFormat df = new DecimalFormat("#,##0.00");
            double percent = (double) used / max * 100;
            return df.format(percent) + "%";
        }
        return "N/A";
    }

    private void printMeminfoColored(CommandExecutor.CmdExecContext ctx) {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 20) {
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    
                    ctx.print(key, Colors.CYAN);
                    ctx.print(": ", Colors.GRAY);
                    
                    if (value.contains("kB")) {
                        String numStr = value.replace("kB", "").trim();
                        try {
                            long kb = Long.parseLong(numStr);
                            printBytes(ctx, kb * 1024);
                        } catch (NumberFormatException e) {
                            ctx.print(value, Colors.YELLOW);
                        }
                    } else {
                        ctx.print(value, Colors.YELLOW);
                    }
                    ctx.println("", Colors.DEFAULT);
                } else {
                    ctx.println(line, Colors.GRAY);
                }
                count++;
            }
        } catch (IOException e) {
            ctx.print("无法读取 /proc/meminfo: ", Colors.RED);
            ctx.println(e.getMessage(), Colors.YELLOW);
        }
    }
    
    private void printProcessMemoryStatsColored(CommandExecutor.CmdExecContext ctx) {
        try {
            int pid = android.os.Process.myPid();
            try (BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/statm"))) {
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 7) {
                        long pageSize = 4096;
                        long size = Long.parseLong(parts[0]) * pageSize;
                        long resident = Long.parseLong(parts[1]) * pageSize;
                        long shared = Long.parseLong(parts[2]) * pageSize;
                        long text = Long.parseLong(parts[3]) * pageSize;
                        long data = Long.parseLong(parts[5]) * pageSize;
                        
                        printMemoryValue(ctx, "总大小: ", size);
                        printMemoryValue(ctx, "驻留内存: ", resident);
                        printMemoryValue(ctx, "共享内存: ", shared);
                        printMemoryValue(ctx, "代码段: ", text);
                        printMemoryValue(ctx, "数据段: ", data);
                    }
                }
            }
        } catch (IOException e) {
            ctx.print("无法读取进程内存统计: ", Colors.RED);
            ctx.println(e.getMessage(), Colors.YELLOW);
        }
    }
    
    private static Context getApplicationContext() {
        try {
            @SuppressLint("PrivateApi") Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);
            
            Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
            return (Context) getApplicationMethod.invoke(activityThread);
        } catch (Exception e) {
            return null;
        }
    }
}
