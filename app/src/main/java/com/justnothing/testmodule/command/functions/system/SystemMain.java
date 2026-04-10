package com.justnothing.testmodule.command.functions.system;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SYSTEM_VER;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.Colors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

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
    public void runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        boolean showCpu = false;
        boolean showMemory = false;
        boolean showOs = false;
        boolean showProps = false;
        boolean showAll = true;
        
        for (String arg : args) {
            switch (arg) {
                case "--cpu" -> {
                    showCpu = true;
                    showAll = false;
                }
                case "--memory" -> {
                    showMemory = true;
                    showAll = false;
                }
                case "--os" -> {
                    showOs = true;
                    showAll = false;
                }
                case "--props" -> {
                    showProps = true;
                    showAll = false;
                }
                case "--all" -> showAll = true;
            }
        }
        
        try {
            if (showAll || showOs) {
                context.println("=== 操作系统信息 ===", Colors.CYAN);
                context.println("", Colors.WHITE);
                printInfoLine(context, "操作系统: ", System.getProperty("os.name"));
                printInfoLine(context, "系统版本: ", System.getProperty("os.version"));
                printInfoLine(context, "架构: ", System.getProperty("os.arch"));
                printInfoLine(context, "处理器数: ", String.valueOf(Runtime.getRuntime().availableProcessors()));
                context.println("", Colors.WHITE);
                
                context.println("=== Android信息 ===", Colors.CYAN);
                context.println("", Colors.WHITE);
                printInfoLine(context, "设备制造商: ", Build.MANUFACTURER);
                printInfoLine(context, "设备品牌: ", Build.BRAND);
                printInfoLine(context, "设备型号: ", Build.MODEL);
                printInfoLine(context, "设备名称: ", Build.DEVICE);
                printInfoLine(context, "产品名称: ", Build.PRODUCT);
                printInfoLine(context, "硬件名称: ", Build.HARDWARE);
                printInfoLine(context, "序列号: ", Build.getSerial());
                printInfoLine(context, "Android版本: ", Build.VERSION.RELEASE);
                printInfoLine(context, "SDK版本: ", String.valueOf(Build.VERSION.SDK_INT));
                printInfoLine(context, "构建版本: ", Build.DISPLAY);
                printInfoLine(context, "构建类型: ", Build.TYPE);
                printInfoLine(context, "构建标签: ", Build.TAGS);
                context.println("", Colors.WHITE);
            }
            
            if (showAll || showCpu) {
                context.println("=== CPU信息 ===", Colors.CYAN);
                context.println("", Colors.WHITE);
                printInfoLine(context, "CPU架构: ", System.getProperty("os.arch"));
                printInfoLine(context, "CPU信息: ", System.getProperty("ro.product.cpu"));
                printInfoLine(context, "CPU ABI: ", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown");
                printInfoLine(context, "CPU ABI2: ", Build.SUPPORTED_ABIS.length > 1 ? Build.SUPPORTED_ABIS[1] : "unknown");
                printInfoLine(context, "处理器数: ", String.valueOf(Runtime.getRuntime().availableProcessors()));
                context.println("", Colors.WHITE);
                
                context.println("=== CPU使用率 ===", Colors.CYAN);
                context.println("", Colors.WHITE);
                String cpuInfo = readCpuInfo();
                context.println(cpuInfo, Colors.GRAY);
            }
            
            if (showAll || showMemory) {
                context.println("=== 内存信息 ===", Colors.CYAN);
                context.println("", Colors.WHITE);
                context.println("Java堆内存:", Colors.GREEN);
                Runtime runtime = Runtime.getRuntime();
                printInfoLine(context, "  最大: ", formatBytes(runtime.maxMemory()));
                printInfoLine(context, "  已分配: ", formatBytes(runtime.totalMemory()));
                printInfoLine(context, "  空闲: ", formatBytes(runtime.freeMemory()));
                printInfoLine(context, "  已用: ", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
                context.println("", Colors.WHITE);
                
                Context appContext = getApplicationContext();
                if (appContext != null) {
                    ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
                    if (activityManager != null) {
                        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                        activityManager.getMemoryInfo(memoryInfo);
                        
                        context.println("系统内存:", Colors.GREEN);
                        printInfoLine(context, "  可用: ", formatBytes(memoryInfo.availMem));
                        printInfoLine(context, "  总计: ", formatBytes(memoryInfo.totalMem));
                        printInfoLine(context, "  阈值: ", formatBytes(memoryInfo.threshold));
                        context.print("  低内存: ", Colors.CYAN);
                        context.println(memoryInfo.lowMemory ? "是" : "否", memoryInfo.lowMemory ? Colors.RED : Colors.GREEN);
                        context.println("", Colors.WHITE);
                    }
                }
                
                context.println("=== 内存详细信息 ===", Colors.CYAN);
                context.println("", Colors.WHITE);
                String memInfo = readMeminfo();
                context.println(memInfo, Colors.GRAY);
            }
            
            if (showAll || showProps) {
                context.println("=== 系统属性 ===", Colors.CYAN);
                context.println("", Colors.WHITE);
                String props = readSystemProperties();
                context.println(props, Colors.GRAY);
            }
            
            logger.info("系统信息查询完成");
            
        } catch (Exception e) {
            logger.error("获取系统信息失败", e);
            context.println("错误: " + e.getMessage(), Colors.RED);
        }
    }
    
    private void printInfoLine(CommandExecutor.CmdExecContext context, String label, String value) {
        context.print(label, Colors.CYAN);
        context.println(value, Colors.YELLOW);
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
            return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    private Context getApplicationContext() {
        try {
            @SuppressLint("PrivateApi") Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);
            
            Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
            return (Context) getApplicationMethod.invoke(activityThread);
        } catch (Exception e) {
            logger.error("获取应用上下文失败", e);
            return null;
        }
    }
}
