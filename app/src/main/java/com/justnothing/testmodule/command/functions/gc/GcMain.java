package com.justnothing.testmodule.command.functions.gc;

import static com.justnothing.testmodule.constants.CommandServer.CMD_GC_VER;

import android.content.Context;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

public class GcMain extends CommandBase {

    public GcMain() {
        super("GC");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: gc [--full] [--stats]
                
                手动触发垃圾回收, 并显示GC统计信息.
                
                支持的选项:
                    --full    - 执行完整的GC
                    --stats   - 显示GC统计信息
                
                示例:
                    gc
                    gc --full
                    gc --stats
                
                (Submodule gc %s)
                """, CMD_GC_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
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
            
            result.append("=== 垃圾回收 ===\n\n");
            
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
                result.append("内存增加: ").append(formatBytes(-freed)).append(" (?)\n");
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
                    android.app.ActivityManager activityManager = 
                        (android.app.ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
                    if (activityManager != null) {
                        android.app.ActivityManager.MemoryInfo memoryInfo = 
                            new android.app.ActivityManager.MemoryInfo();
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
                    
                    result.append("  总大小: ").append(formatBytes(size)).append("\n");
                    result.append("  驻留内存: ").append(formatBytes(resident)).append("\n");
                    result.append("  共享内存: ").append(formatBytes(shared)).append("\n");
                    result.append("  代码段: ").append(formatBytes(text)).append("\n");
                    result.append("  数据段: ").append(formatBytes(data)).append("\n");
                }
            }
            reader.close();
        } catch (IOException e) {
            result.append("  无法读取进程内存统计: ").append(e.getMessage()).append("\n");
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
