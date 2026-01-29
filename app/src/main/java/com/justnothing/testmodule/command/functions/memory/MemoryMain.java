package com.justnothing.testmodule.command.functions.memory;

import static com.justnothing.testmodule.constants.CommandServer.CMD_MEMORY_VER;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
        if (commandName.equals("minfo")) {
            return String.format("""
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
        } else if (commandName.equals("mgc")) {
            return String.format("""
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
        } else if (commandName.equals("mdump")) {
            return String.format("""
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
        } else {
            return String.format("""
                    语法: memory <subcmd> [args...]
                    
                    内存调试和管理工具.
                    
                    子命令:
                        info [options]                     - 显示详细的内存使用情况
                        gc [options]                       - 手动触发垃圾回收
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
        }
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
        
        if (cmdName.equals("minfo")) {
            subCommand = "info";
            subArgs = args;
        } else if (cmdName.equals("mgc")) {
            subCommand = "gc";
            subArgs = args;
        } else if (cmdName.equals("mdump")) {
            subCommand = "dump";
            subArgs = args;
        } else {
            if (args.length < 1) {
                return getHelpText();
            }
            subCommand = args[0];
            subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
        }

        try {
            return switch (subCommand) {
                case "info" -> handleInfo(subArgs);
                case "gc" -> handleGc(subArgs);
                case "dump" -> handleDump(subArgs);
                default -> "未知子命令: " + subCommand + "\n" + getHelpText();
            };
        } catch (Exception e) {
            logger.error("执行memory命令失败", e);
            return "错误: " + e.getMessage();
        }
    }

    private String handleInfo(String[] args) {
        boolean showHeapOnly = false;
        boolean showDetailed = true;
        
        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("--heap")) {
                showHeapOnly = true;
                showDetailed = false;
            } else if (arg.equals("-d") || arg.equals("--detailed")) {
                showDetailed = true;
                showHeapOnly = false;
            }
        }
        
        StringBuilder result = new StringBuilder();
        
        try {
            Context appContext = getApplicationContext();
            if (appContext == null) {
                logger.warn("无法获取应用上下文，部分信息可能不可用");
            }
            
            if (showHeapOnly) {
                result.append("===== 堆内存信息 =====\n\n");
                result.append("===== 原生堆内存 =====\n\n");
                result.append("已分配: ").append(formatBytes(Debug.getNativeHeapAllocatedSize())).append("\n");
                result.append("已用: ").append(formatBytes(Debug.getNativeHeapSize())).append("\n");
                result.append("空闲: ").append(formatBytes(Debug.getNativeHeapFreeSize())).append("\n\n");
                
                result.append("===== Java运行时内存 =====\n\n");
                Runtime runtime = Runtime.getRuntime();
                result.append("最大内存: ").append(formatBytes(runtime.maxMemory())).append("\n");
                result.append("已分配内存: ").append(formatBytes(runtime.totalMemory())).append("\n");
                result.append("空闲内存: ").append(formatBytes(runtime.freeMemory())).append("\n");
                result.append("已用内存: ").append(formatBytes(runtime.totalMemory() - runtime.freeMemory())).append("\n");
            } else {
                result.append("===== 详细内存信息 =====\n\n");
                
                result.append("===== Java运行时内存 =====\n\n");
                Runtime runtime = Runtime.getRuntime();
                result.append("最大内存: ").append(formatBytes(runtime.maxMemory())).append("\n");
                result.append("已分配内存: ").append(formatBytes(runtime.totalMemory())).append("\n");
                result.append("空闲内存: ").append(formatBytes(runtime.freeMemory())).append("\n");
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                result.append("已用内存: ").append(formatBytes(usedMemory)).append("\n");
                if (runtime.maxMemory() > 0) {
                    double percent = (double) usedMemory / runtime.maxMemory() * 100;
                    result.append("使用率: ").append(new DecimalFormat("#,##0.00").format(percent)).append("%\n");
                }
                result.append("\n");
                
                result.append("===== 原生堆内存 =====\n\n");
                result.append("已分配: ").append(formatBytes(Debug.getNativeHeapAllocatedSize())).append("\n");
                result.append("已用: ").append(formatBytes(Debug.getNativeHeapSize())).append("\n");
                result.append("空闲: ").append(formatBytes(Debug.getNativeHeapFreeSize())).append("\n\n");
                
                if (appContext != null) {
                    result.append("===== 进程内存 =====\n\n");
                    ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
                    if (activityManager != null) {
                        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                        activityManager.getMemoryInfo(memoryInfo);
                        
                        result.append("可用内存: ").append(formatBytes(memoryInfo.availMem)).append("\n");
                        result.append("总内存: ").append(formatBytes(memoryInfo.totalMem)).append("\n");
                        result.append("内存阈值: ").append(formatBytes(memoryInfo.threshold)).append("\n");
                        result.append("低内存状态: ").append(memoryInfo.lowMemory ? "是" : "否").append("\n\n");
                        
                        int pid = android.os.Process.myPid();
                        Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{pid});
                        if (memoryInfos.length > 0) {
                            Debug.MemoryInfo processMemory = memoryInfos[0];
                            result.append("当前进程内存:\n");
                            result.append("  PSS: ").append(formatBytes(processMemory.getTotalPss() * 1024L)).append("\n");
                            result.append("  USS: ").append(formatBytes(processMemory.getTotalPrivateDirty() * 1024L)).append("\n");
                            result.append("  RSS: ").append(formatBytes(processMemory.getTotalSharedDirty() * 1024L)).append("\n\n");
                        }
                    }
                }
                
                result.append("===== 系统内存 =====\n\n");
                result.append(readMeminfo());
                
                result.append("===== 进程内存统计 =====\n\n");
                result.append(readProcessMemoryStats());
            }
            
            logger.info("内存信息查询完成");
            
        } catch (Exception e) {
            logger.error("获取内存信息失败", e);
            return "错误: " + e.getMessage();
        }
        
        return result.toString();
    }

    private String handleGc(String[] args) {
        boolean fullGc = false;
        boolean showStats = false;
        
        for (String arg : args) {
            if (arg.equals("--full")) {
                fullGc = true;
            } else if (arg.equals("--stats")) {
                showStats = true;
            }
        }
        
        StringBuilder result = new StringBuilder();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            result.append("===== 垃圾回收 =====\n\n");
            
            long beforeUsed = runtime.totalMemory() - runtime.freeMemory();
            long beforeTotal = runtime.totalMemory();
            long beforeMax = runtime.maxMemory();
            
            result.append("GC前堆内存: ").append(formatBytes(beforeUsed));
            result.append(" / ").append(formatBytes(beforeTotal));
            if (beforeMax > 0) {
                result.append(" (").append(formatPercent(beforeUsed, beforeMax)).append(")");
            }
            result.append("\n\n");
            
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
            
            result.append("GC后堆内存: ").append(formatBytes(afterUsed));
            result.append(" / ").append(formatBytes(afterTotal));
            if (afterMax > 0) {
                result.append(" (").append(formatPercent(afterUsed, afterMax)).append(")");
            }
            result.append("\n\n");
            
            long freed = beforeUsed - afterUsed;
            if (freed > 0) {
                result.append("释放内存: ").append(formatBytes(freed)).append("\n");
            } else if (freed < 0) {
                result.append("内存增加: ").append(formatBytes(-freed)).append(" (?)\n").append("可以试试memory gc --full\n");
            } else {
                result.append("内存未变化\n");
            }
            
            if (showStats) {
                result.append("\n===== GC统计信息 =====\n\n");
                result.append("Tip: Android不提供详细的GC统计信息\n");
                result.append("以下是内存使用统计:\n\n");
                
                result.append("Java堆内存:\n");
                result.append("  最大: ").append(formatBytes(afterMax)).append("\n");
                result.append("  已分配: ").append(formatBytes(afterTotal)).append("\n");
                result.append("  已用: ").append(formatBytes(afterUsed)).append("\n");
                result.append("  空闲: ").append(formatBytes(runtime.freeMemory())).append("\n\n");
                
                Context appContext = getApplicationContext();
                if (appContext != null) {
                    ActivityManager activityManager = 
                        (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
                    if (activityManager != null) {
                        ActivityManager.MemoryInfo memoryInfo = 
                            new ActivityManager.MemoryInfo();
                        activityManager.getMemoryInfo(memoryInfo);
                        
                        result.append("系统内存:\n");
                        result.append("  可用: ").append(formatBytes(memoryInfo.availMem)).append("\n");
                        result.append("  总计: ").append(formatBytes(memoryInfo.totalMem)).append("\n");
                        result.append("  低内存: ").append(memoryInfo.lowMemory ? "是" : "否").append("\n\n");
                    }
                }
                
                result.append("进程内存统计:\n");
                result.append(readProcessMemoryStats());
            }
            
            logger.info("垃圾回收完成");
            
        } catch (Exception e) {
            logger.error("垃圾回收失败", e);
            return "错误: " + e.getMessage();
        }
        
        return result.toString();
    }

    private String handleDump(String[] args) {
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
        
        StringBuilder output = new StringBuilder();
        
        output.append("=== 系统堆转储 ===\n");
        output.append("时间: ").append(new Date()).append("\n");
        
        if (filePath != null) {
            output.append("文件: ").append(filePath).append("\n");
        }
        output.append("\n");
        
        if (dumpHeap || dumpFull) {
            dumpHeapInfo(output);
        }
        
        if (dumpThreads || dumpFull) {
            dumpThreadInfo(output);
        }
        
        if (dumpFull) {
            dumpSystemInfo(output);
        }
        
        if (filePath != null) {
            File outputFile = new File(filePath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                logger.info("开始导出堆信息到: " + filePath);
                writer.write(output.toString());
                writer.flush();
                logger.info("堆信息导出完成");
                return "堆信息已导出到: " + filePath;
            } catch (IOException e) {
                logger.error("导出堆信息失败", e);
                return "错误: " + e.getMessage();
            }
        } else {
            return output.toString();
        }
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
            BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/statm"));
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
            reader.close();
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
