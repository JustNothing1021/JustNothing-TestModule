package com.justnothing.testmodule.command.functions.heap;

import static com.justnothing.testmodule.constants.CommandServer.CMD_HEAP_VER;

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

public class HeapMain extends CommandBase {

    public HeapMain() {
        super("Heap");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: heap
                
                查看Java堆内存使用情况, 包括:
                    - 堆内存使用情况
                    - 非堆内存使用情况
                    - 内存使用百分比
                    - 对象统计信息
                
                示例:
                    heap
                
                (Submodule heap %s)
                """, CMD_HEAP_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        StringBuilder result = new StringBuilder();
        
        try {
            Context appContext = getApplicationContext();
            if (appContext == null) {
                getLogger().warn("无法获取应用上下文");
                return "错误: 无法获取应用上下文";
            }
            
            result.append("=== Java堆内存信息 ===\n\n");
            
            result.append("=== 堆内存信息 ===\n");
            result.append("已分配: ").append(formatBytes(Debug.getNativeHeapAllocatedSize())).append("\n");
            result.append("已用: ").append(formatBytes(Debug.getNativeHeapSize())).append("\n");
            result.append("空闲: ").append(formatBytes(Debug.getNativeHeapFreeSize())).append("\n\n");
            
            Runtime runtime = Runtime.getRuntime();
            result.append("=== Java运行时内存 ===\n");
            result.append("最大内存: ").append(formatBytes(runtime.maxMemory())).append("\n");
            result.append("已分配内存: ").append(formatBytes(runtime.totalMemory())).append("\n");
            result.append("空闲内存: ").append(formatBytes(runtime.freeMemory())).append("\n");
            result.append("已用内存: ").append(formatBytes(runtime.totalMemory() - runtime.freeMemory())).append("\n");
            if (runtime.maxMemory() > 0) {
                double percent = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100;
                result.append("使用率: ").append(new DecimalFormat("#,##0.00").format(percent)).append("%\n");
            }
            result.append("\n");
            
            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);
                
                result.append("=== 系统内存信息 ===\n");
                result.append("可用内存: ").append(formatBytes(memoryInfo.availMem)).append("\n");
                result.append("总内存: ").append(formatBytes(memoryInfo.totalMem)).append("\n");
                result.append("内存阈值: ").append(formatBytes(memoryInfo.threshold)).append("\n");
                result.append("低内存状态: ").append(memoryInfo.lowMemory ? "是" : "否").append("\n\n");
                
                int pid = android.os.Process.myPid();
                android.os.Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{pid});
                if (memoryInfos.length > 0) {
                    android.os.Debug.MemoryInfo processMemory = memoryInfos[0];
                    result.append("=== 当前进程内存 ===\n");
                    result.append("PSS: ").append(formatBytes(processMemory.getTotalPss() * 1024L)).append("\n");
                    result.append("USS: ").append(formatBytes(processMemory.getTotalPrivateDirty() * 1024L)).append("\n");
                    result.append("RSS: ").append(formatBytes(processMemory.getTotalSharedDirty() * 1024L)).append("\n\n");
                }
            }
            
            result.append("=== 内存详细信息 ===\n\n");
            result.append(readMeminfo());
            
            getLogger().info("堆内存信息查询完成");
            
        } catch (Exception e) {
            getLogger().error("获取堆内存信息失败", e);
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
