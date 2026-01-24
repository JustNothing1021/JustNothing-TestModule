package com.justnothing.testmodule.command.functions.system;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SYSTEM_VER;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;

public class SystemMain extends CommandBase {

    public SystemMain() {
        super("System");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: system [--cpu] [--memory] [--os] [--props] [--all]
                
                显示系统信息。
                
                选项:
                  --cpu      - 显示CPU信息
                  --memory   - 显示内存信息
                  --os       - 显示操作系统信息
                  --props    - 显示系统属性
                  --all      - 显示所有信息（默认）
                
                示例:
                  system
                  system --cpu
                  system --memory
                  system --os
                  system --props
                
                (Submodule system %s)
                """, CMD_SYSTEM_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        boolean showCpu = false;
        boolean showMemory = false;
        boolean showOs = false;
        boolean showProps = false;
        boolean showAll = true;
        
        for (String arg : args) {
            if (arg.equals("--cpu")) {
                showCpu = true;
                showAll = false;
            } else if (arg.equals("--memory")) {
                showMemory = true;
                showAll = false;
            } else if (arg.equals("--os")) {
                showOs = true;
                showAll = false;
            } else if (arg.equals("--props")) {
                showProps = true;
                showAll = false;
            } else if (arg.equals("--all")) {
                showAll = true;
            }
        }
        
        StringBuilder result = new StringBuilder();
        
        try {
            if (showAll || showOs) {
                result.append("=== 操作系统信息 ===\n\n");
                result.append("操作系统: ").append(System.getProperty("os.name")).append("\n");
                result.append("系统版本: ").append(System.getProperty("os.version")).append("\n");
                result.append("架构: ").append(System.getProperty("os.arch")).append("\n");
                result.append("处理器数: ").append(Runtime.getRuntime().availableProcessors()).append("\n\n");
                
                result.append("=== Android信息 ===\n\n");
                result.append("设备制造商: ").append(Build.MANUFACTURER).append("\n");
                result.append("设备品牌: ").append(Build.BRAND).append("\n");
                result.append("设备型号: ").append(Build.MODEL).append("\n");
                result.append("设备名称: ").append(Build.DEVICE).append("\n");
                result.append("产品名称: ").append(Build.PRODUCT).append("\n");
                result.append("硬件名称: ").append(Build.HARDWARE).append("\n");
                result.append("序列号: ").append(Build.getSerial()).append("\n");
                result.append("Android版本: ").append(Build.VERSION.RELEASE).append("\n");
                result.append("SDK版本: ").append(Build.VERSION.SDK_INT).append("\n");
                result.append("构建版本: ").append(Build.DISPLAY).append("\n");
                result.append("构建类型: ").append(Build.TYPE).append("\n");
                result.append("构建标签: ").append(Build.TAGS).append("\n\n");
            }
            
            if (showAll || showCpu) {
                result.append("=== CPU信息 ===\n\n");
                result.append("CPU架构: ").append(System.getProperty("os.arch")).append("\n");
                result.append("CPU信息: ").append(System.getProperty("ro.product.cpu")).append("\n");
                result.append("CPU ABI: ").append(Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown").append("\n");
                result.append("CPU ABI2: ").append(Build.SUPPORTED_ABIS.length > 1 ? Build.SUPPORTED_ABIS[1] : "unknown").append("\n");
                result.append("处理器数: ").append(Runtime.getRuntime().availableProcessors()).append("\n\n");
                
                result.append("=== CPU使用率 ===\n\n");
                result.append(readCpuInfo());
            }
            
            if (showAll || showMemory) {
                result.append("=== 内存信息 ===\n\n");
                result.append("Java堆内存:\n");
                Runtime runtime = Runtime.getRuntime();
                result.append("  最大: ").append(formatBytes(runtime.maxMemory())).append("\n");
                result.append("  已分配: ").append(formatBytes(runtime.totalMemory())).append("\n");
                result.append("  空闲: ").append(formatBytes(runtime.freeMemory())).append("\n");
                result.append("  已用: ").append(formatBytes(runtime.totalMemory() - runtime.freeMemory())).append("\n\n");
                
                Context appContext = getApplicationContext();
                if (appContext != null) {
                    ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
                    if (activityManager != null) {
                        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                        activityManager.getMemoryInfo(memoryInfo);
                        
                        result.append("系统内存:\n");
                        result.append("  可用: ").append(formatBytes(memoryInfo.availMem)).append("\n");
                        result.append("  总计: ").append(formatBytes(memoryInfo.totalMem)).append("\n");
                        result.append("  阈值: ").append(formatBytes(memoryInfo.threshold)).append("\n");
                        result.append("  低内存: ").append(memoryInfo.lowMemory ? "是" : "否").append("\n\n");
                    }
                }
                
                result.append("=== 内存详细信息 ===\n\n");
                result.append(readMeminfo());
            }
            
            if (showAll || showProps) {
                result.append("=== 系统属性 ===\n\n");
                result.append(readSystemProperties());
            }
            
            getLogger().info("系统信息查询完成");
            
        } catch (Exception e) {
            getLogger().error("获取系统信息失败", e);
            return "错误: " + e.getMessage();
        }
        
        return result.toString();
    }
    
    private static String readCpuInfo() {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 10) {
                result.append(line).append("\n");
                count++;
            }
        } catch (IOException e) {
            result.append("无法读取 /proc/cpuinfo: ").append(e.getMessage()).append("\n");
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
    
    private static String readSystemProperties() {
        StringBuilder result = new StringBuilder();
        String[] props = {
            "ro.build.version.sdk",
            "ro.build.version.release",
            "ro.build.version.codename",
            "ro.build.type",
            "ro.product.model",
            "ro.product.brand",
            "ro.product.manufacturer",
            "ro.product.device",
            "ro.product.name",
            "ro.product.cpu",
            "ro.hardware",
            "ro.revision",
            "ro.bootloader",
            "ro.sf.lcd_density",
            "ro.debuggable"
        };
        
        for (String prop : props) {
            String value = System.getProperty(prop);
            if (value != null) {
                result.append(prop).append(": ").append(value).append("\n");
            }
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
    
    private Context getApplicationContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);
            
            Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
            return (Context) getApplicationMethod.invoke(activityThread);
        } catch (Exception e) {
            getLogger().error("获取应用上下文失败", e);
            return null;
        }
    }
}
