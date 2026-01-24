package com.justnothing.testmodule.command.functions.memory;

import static com.justnothing.testmodule.constants.CommandServer.CMD_MEMORY_VER;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Locale;

public class MemoryMain extends CommandBase {

    public MemoryMain() {
        super("Memory");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: memory
                
                显示详细的内存使用情况, 包括:
                    - Java堆内存
                    - 非堆内存
                    - 系统内存
                    - 进程内存
                    - 内存使用趋势
                
                示例:
                    memory
                
                (Submodule memory %s)
                """, CMD_MEMORY_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        StringBuilder result = new StringBuilder();
        
        try {
            Context appContext = getApplicationContext();
            if (appContext == null) {
                getLogger().warn("无法获取应用上下文，部分信息可能不可用");
            }
            
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
                    android.os.Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{pid});
                    if (memoryInfos.length > 0) {
                        android.os.Debug.MemoryInfo processMemory = memoryInfos[0];
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
            
            getLogger().info("内存信息查询完成");
            
        } catch (Exception e) {
            getLogger().error("获取内存信息失败", e);
            return "错误: " + e.getMessage();
        }
        
        return result.toString();
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
