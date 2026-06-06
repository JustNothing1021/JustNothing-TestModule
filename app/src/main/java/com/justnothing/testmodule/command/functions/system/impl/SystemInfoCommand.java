package com.justnothing.testmodule.command.functions.system.impl;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.script.SystemInfoRequest;
import com.justnothing.testmodule.command.functions.system.SystemFieldInfo;
import com.justnothing.testmodule.command.functions.system.SystemInfoResult;
import com.justnothing.testmodule.command.output.Colors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SubCommandInfo(
    description = "显示系统信息, 包括操作系统、Android、CPU、内存等",
    usage = "system [options]",
    examples = {
        "system",
        "system --cpu",
        "system --memory"
    },
    optionsDesc = """
            选项:
              --cpu     - 只显示CPU信息
              --memory  - 只显示内存信息
              --os      - 只显示操作系统信息
              --props   - 只显示系统属性
            """
)
public class SystemInfoCommand extends AbstractCommand<SystemInfoRequest, SystemInfoResult> {

    public SystemInfoCommand() {
        super("system", SystemInfoRequest.class, SystemInfoResult.class);
    }

    @Override
    protected SystemInfoResult executeInternal(CommandExecutor.CmdExecContext<SystemInfoRequest> context) throws Exception {
        SystemInfoRequest request = context.getRequest();
        if (request == null) {
            request = new SystemInfoRequest();
        }

        boolean showCpu = request.isShowCpu();
        boolean showMemory = request.isShowMemory();
        boolean showOs = request.isShowOs();
        boolean showProps = request.isShowProps();
        boolean showAll = request.isShowAll() && !showCpu && !showMemory && !showOs && !showProps;

        // 构建结构化结果数据
        List<SystemFieldInfo> fields = new ArrayList<>();

        if (showAll || showOs) {
            addOsFields(fields);
            printOsInfo(context);
        }

        if (showAll || showCpu) {
            addCpuFields(fields);
            printCpuInfo(context);
        }

        if (showAll || showMemory) {
            addMemoryFields(fields);
            printMemoryInfo(context);
        }

        if (showAll || showProps) {
            addSystemPropertyFields(fields);
            printSystemProperties(context);
        }

        SystemInfoResult result = new SystemInfoResult(request.getRequestId());
        result.setFields(fields);
        result.setSuccess(true);

        return result;
    }

    // ========== 结构化数据构建 ==========

    private void addOsFields(List<SystemFieldInfo> fields) {
        addSection(fields, "操作系统信息");
        addField(fields, "操作系统", "操作系统", System.getProperty("os.name"));
        addField(fields, "操作系统", "系统版本", System.getProperty("os.version"));
        addField(fields, "操作系统", "架构", System.getProperty("os.arch"));
        addField(fields, "操作系统", "处理器数", String.valueOf(Runtime.getRuntime().availableProcessors()));

        addSection(fields, "Android信息");
        addField(fields, "Android", "设备制造商", Build.MANUFACTURER);
        addField(fields, "Android", "设备品牌", Build.BRAND);
        addField(fields, "Android", "设备型号", Build.MODEL);
        addField(fields, "Android", "设备名称", Build.DEVICE);
        addField(fields, "Android", "产品名称", Build.PRODUCT);
        addField(fields, "Android", "硬件名称", Build.HARDWARE);
        addField(fields, "Android", "序列号", "需要权限");
        addField(fields, "Android", "Android版本", Build.VERSION.RELEASE);
        addField(fields, "Android", "SDK版本", String.valueOf(Build.VERSION.SDK_INT));
        addField(fields, "Android", "构建版本", Build.DISPLAY);
        addField(fields, "Android", "构建类型", Build.TYPE);
        addField(fields, "Android", "构建标签", Build.TAGS);
    }

    private void addCpuFields(List<SystemFieldInfo> fields) {
        addSection(fields, "CPU信息");
        addField(fields, "CPU", "CPU架构", System.getProperty("os.arch"));
        addField(fields, "CPU", "CPU信息", System.getProperty("ro.product.cpu"));
        addField(fields, "CPU", "CPU ABI", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown");
        addField(fields, "CPU", "CPU ABI2", Build.SUPPORTED_ABIS.length > 1 ? Build.SUPPORTED_ABIS[1] : "unknown");
        addField(fields, "CPU", "处理器数", String.valueOf(Runtime.getRuntime().availableProcessors()));
        addField(fields, "CPU", "CPU详情", readCpuInfo());
    }

    private void addMemoryFields(List<SystemFieldInfo> fields) {
        addSection(fields, "内存信息");
        Runtime runtime = Runtime.getRuntime();
        addField(fields, "Java堆内存", "最大内存", formatBytes(runtime.maxMemory()));
        addField(fields, "Java堆内存", "已分配内存", formatBytes(runtime.totalMemory()));
        addField(fields, "Java堆内存", "空闲内存", formatBytes(runtime.freeMemory()));
        addField(fields, "Java堆内存", "已用内存", formatBytes(runtime.totalMemory() - runtime.freeMemory()));

        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                addField(fields, "系统内存", "可用内存", formatBytes(memoryInfo.availMem));
                addField(fields, "系统内存", "总内存", formatBytes(memoryInfo.totalMem));
                addField(fields, "系统内存", "阈值", formatBytes(memoryInfo.threshold));
                addField(fields, "系统内存", "低内存", memoryInfo.lowMemory ? "是" : "否");
            }
        }

        addField(fields, "内存详细信息", "内存详情", readMeminfo());
    }

    private void addSystemPropertyFields(List<SystemFieldInfo> fields) {
        addSection(fields, "系统属性");
        String[] props = {
            "ro.build.version.sdk", "ro.build.version.release", "ro.build.version.codename",
            "ro.build.type", "ro.product.model", "ro.product.brand",
            "ro.product.manufacturer", "ro.product.device", "ro.product.name",
            "ro.product.cpu", "ro.hardware", "ro.revision",
            "ro.bootloader", "ro.sf.lcd_density", "ro.debuggable"
        };

        for (String prop : props) {
            String value = System.getProperty(prop);
            if (value != null) {
                addField(fields, "系统属性", prop, value);
            }
        }
    }

    private static void addSection(List<SystemFieldInfo> fields, String category) {
        fields.add(new SystemFieldInfo(category, "[SECTION]", category));
    }

    private static void addField(List<SystemFieldInfo> fields, String category, String label, String value) {
        fields.add(new SystemFieldInfo(category, label, value != null ? value : ""));
    }

    // ========== CLI 输出 ==========

    private void printOsInfo(CommandExecutor.CmdExecContext<?> ctx) {
        ctx.println("=== 操作系统信息 ===", Colors.CYAN);
        ctx.println("", Colors.WHITE);
        printInfoLine(ctx, "操作系统: ", System.getProperty("os.name"));
        printInfoLine(ctx, "系统版本: ", System.getProperty("os.version"));
        printInfoLine(ctx, "架构: ", System.getProperty("os.arch"));
        printInfoLine(ctx, "处理器数: ", String.valueOf(Runtime.getRuntime().availableProcessors()));
        ctx.println("", Colors.WHITE);

        ctx.println("=== Android信息 ===", Colors.CYAN);
        ctx.println("", Colors.WHITE);
        printInfoLine(ctx, "设备制造商: ", Build.MANUFACTURER);
        printInfoLine(ctx, "设备品牌: ", Build.BRAND);
        printInfoLine(ctx, "设备型号: ", Build.MODEL);
        printInfoLine(ctx, "设备名称: ", Build.DEVICE);
        printInfoLine(ctx, "产品名称: ", Build.PRODUCT);
        printInfoLine(ctx, "硬件名称: ", Build.HARDWARE);
        printInfoLine(ctx, "序列号: ", "需要权限");
        printInfoLine(ctx, "Android版本: ", Build.VERSION.RELEASE);
        printInfoLine(ctx, "SDK版本: ", String.valueOf(Build.VERSION.SDK_INT));
        printInfoLine(ctx, "构建版本: ", Build.DISPLAY);
        printInfoLine(ctx, "构建类型: ", Build.TYPE);
        printInfoLine(ctx, "构建标签: ", Build.TAGS);
        ctx.println("", Colors.WHITE);
    }

    private void printCpuInfo(CommandExecutor.CmdExecContext<?> ctx) {
        ctx.println("=== CPU信息 ===", Colors.CYAN);
        ctx.println("", Colors.WHITE);
        printInfoLine(ctx, "CPU架构: ", System.getProperty("os.arch"));
        printInfoLine(ctx, "CPU信息: ", System.getProperty("ro.product.cpu"));
        printInfoLine(ctx, "CPU ABI: ", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown");
        printInfoLine(ctx, "CPU ABI2: ", Build.SUPPORTED_ABIS.length > 1 ? Build.SUPPORTED_ABIS[1] : "unknown");
        printInfoLine(ctx, "处理器数: ", String.valueOf(Runtime.getRuntime().availableProcessors()));
        ctx.println("", Colors.WHITE);

        ctx.println("=== CPU使用率 ===", Colors.CYAN);
        ctx.println("", Colors.WHITE);
        ctx.println(readCpuInfo(), Colors.GRAY);
    }

    private void printMemoryInfo(CommandExecutor.CmdExecContext<?> ctx) {
        ctx.println("=== 内存信息 ===", Colors.CYAN);
        ctx.println("", Colors.WHITE);
        ctx.println("Java堆内存:", Colors.GREEN);
        Runtime runtime = Runtime.getRuntime();
        printInfoLine(ctx, "  最大: ", formatBytes(runtime.maxMemory()));
        printInfoLine(ctx, "  已分配: ", formatBytes(runtime.totalMemory()));
        printInfoLine(ctx, "  空闲: ", formatBytes(runtime.freeMemory()));
        printInfoLine(ctx, "  已用: ", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
        ctx.println("", Colors.WHITE);

        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                ctx.println("系统内存:", Colors.GREEN);
                printInfoLine(ctx, "  可用: ", formatBytes(memoryInfo.availMem));
                printInfoLine(ctx, "  总计: ", formatBytes(memoryInfo.totalMem));
                printInfoLine(ctx, "  阈值: ", formatBytes(memoryInfo.threshold));
                ctx.print("  低内存: ", Colors.CYAN);
                ctx.println(memoryInfo.lowMemory ? "是" : "否", memoryInfo.lowMemory ? Colors.RED : Colors.GREEN);
                ctx.println("", Colors.WHITE);
            }
        }

        ctx.println("=== 内存详细信息 ===", Colors.CYAN);
        ctx.println("", Colors.WHITE);
        ctx.println(readMeminfo(), Colors.GRAY);
    }

    private void printSystemProperties(CommandExecutor.CmdExecContext<?> ctx) {
        ctx.println("=== 系统属性 ===", Colors.CYAN);
        ctx.println("", Colors.WHITE);
        ctx.println(readSystemProperties(), Colors.GRAY);
    }

    private static void printInfoLine(CommandExecutor.CmdExecContext<?> ctx, String label, String value) {
        ctx.print(label, Colors.CYAN);
        ctx.println(value, Colors.YELLOW);
    }

    // ========== 工具方法 ==========

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
            "ro.build.version.sdk", "ro.build.version.release", "ro.build.version.codename",
            "ro.build.type", "ro.product.model", "ro.product.brand",
            "ro.product.manufacturer", "ro.product.device", "ro.product.name",
            "ro.product.cpu", "ro.hardware", "ro.revision",
            "ro.bootloader", "ro.sf.lcd_density", "ro.debuggable"
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
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024.0));
        }
    }

    @SuppressLint("PrivateApi")
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
