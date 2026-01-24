package com.justnothing.testmodule.command.functions.dump;

import static com.justnothing.testmodule.constants.CommandServer.CMD_DUMP_VER;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;

public class DumpMain extends CommandBase {

    public DumpMain() {
        super("Dump");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: dump [--heap] [--threads] [--full] [--format <format>] <file_path>
                
                导出堆信息和系统状态.
                
                选项:
                    --heap            - 只导出堆信息
                    --threads         - 只导出线程信息
                    --full            - 导出完整信息 (默认)
                
                如果不指定文件路径, 将输出到控制台.
                
                示例:
                    dump
                    dump /sdcard/heap_dump.txt
                    dump --heap /sdcard/heap_only.txt
                    dump --full /sdcard/full_dump.txt
                
                (Submodule dump %s)
                """, CMD_DUMP_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        boolean dumpHeap = false;
        boolean dumpThreads = false;
        boolean dumpFull = true;
        String filePath = null;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
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
            
            if (stackTrace != null && stackTrace.length >0) {
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
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader("/proc/meminfo"))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 30) {
                result.append(line).append("\n");
                count++;
            }
        } catch (IOException e) {
            result.append("无法读取 /proc/meminfo: ").append(e.getMessage()).append("\n");
        }
        return result.toString();
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    private static Context getApplicationContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);
            
            Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
            return (Context) getApplicationMethod.invoke(activityThread);
        } catch (Exception e) {
            return null;
        }
    }
}
