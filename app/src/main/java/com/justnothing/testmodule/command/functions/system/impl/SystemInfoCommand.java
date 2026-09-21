package com.justnothing.testmodule.command.functions.system.impl;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.functions.script.request.SystemInfoRequest;
import com.justnothing.testmodule.command.functions.system.SystemTexts;
import com.justnothing.testmodule.command.functions.system.model.SystemFieldInfo;
import com.justnothing.testmodule.command.functions.system.response.SystemInfoResult;
import com.justnothing.testmodule.command.framework.output.Colors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SubCommandInfo(
    description = SystemTexts.SUB_SYSTEM_DESC,
    usage = "system [options]",
    examples = {
        "system",
        "system --cpu",
        "system --memory"
    },
    optionsDesc = SystemTexts.SUB_SYSTEM_OPTIONS
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
        addSection(fields, Text.zhEn("操作系统信息", "OS information").text());
        addField(fields, SystemTexts.FIELD_OS.text(), SystemTexts.FIELD_OS.text(), System.getProperty("os.name"));
        addField(fields, SystemTexts.FIELD_OS.text(), Text.zhEn("系统版本", "OS version").text(),
                System.getProperty("os.version"));
        addField(fields, SystemTexts.FIELD_OS.text(), Text.zhEn("架构", "Architecture").text(),
                System.getProperty("os.arch"));
        addField(fields, SystemTexts.FIELD_OS.text(), SystemTexts.FIELD_PROCESSOR_COUNT.text(),
                String.valueOf(Runtime.getRuntime().availableProcessors()));

        addSection(fields, Text.zhEn("Android信息", "Android information").text());
        addField(fields, "Android", Text.zhEn("设备制造商", "Manufacturer").text(), Build.MANUFACTURER);
        addField(fields, "Android", Text.zhEn("设备品牌", "Brand").text(), Build.BRAND);
        addField(fields, "Android", Text.zhEn("设备型号", "Model").text(), Build.MODEL);
        addField(fields, "Android", Text.zhEn("设备名称", "Device").text(), Build.DEVICE);
        addField(fields, "Android", Text.zhEn("产品名称", "Product").text(), Build.PRODUCT);
        addField(fields, "Android", Text.zhEn("硬件名称", "Hardware").text(), Build.HARDWARE);
        addField(fields, "Android", Text.zhEn("序列号", "Serial number").text(),
                SystemTexts.VALUE_PERMISSION_REQUIRED.text());
        addField(fields, "Android", Text.zhEn("Android版本", "Android version").text(), Build.VERSION.RELEASE);
        addField(fields, "Android", Text.zhEn("SDK版本", "SDK version").text(),
                String.valueOf(Build.VERSION.SDK_INT));
        addField(fields, "Android", Text.zhEn("构建版本", "Build version").text(), Build.DISPLAY);
        addField(fields, "Android", Text.zhEn("构建类型", "Build type").text(), Build.TYPE);
        addField(fields, "Android", Text.zhEn("构建标签", "Build tags").text(), Build.TAGS);
    }

    private void addCpuFields(List<SystemFieldInfo> fields) {
        addSection(fields, SystemTexts.FIELD_CPU_INFO.text());
        addField(fields, "CPU", Text.zhEn("CPU架构", "CPU architecture").text(), System.getProperty("os.arch"));
        addField(fields, "CPU", SystemTexts.FIELD_CPU_INFO.text(), System.getProperty("ro.product.cpu"));
        addField(fields, "CPU", "CPU ABI", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown");
        addField(fields, "CPU", "CPU ABI2", Build.SUPPORTED_ABIS.length > 1 ? Build.SUPPORTED_ABIS[1] : "unknown");
        addField(fields, "CPU", SystemTexts.FIELD_PROCESSOR_COUNT.text(),
                String.valueOf(Runtime.getRuntime().availableProcessors()));
        addField(fields, "CPU", Text.zhEn("CPU详情", "CPU details").text(), readCpuInfo());
    }

    private void addMemoryFields(List<SystemFieldInfo> fields) {
        addSection(fields, Text.zhEn("内存信息", "Memory information").text());
        Runtime runtime = Runtime.getRuntime();
        addField(fields, SystemTexts.FIELD_JAVA_HEAP.text(), Text.zhEn("最大内存", "Max memory").text(),
                formatBytes(runtime.maxMemory()));
        addField(fields, SystemTexts.FIELD_JAVA_HEAP.text(), Text.zhEn("已分配内存", "Allocated memory").text(),
                formatBytes(runtime.totalMemory()));
        addField(fields, SystemTexts.FIELD_JAVA_HEAP.text(), Text.zhEn("空闲内存", "Free memory").text(),
                formatBytes(runtime.freeMemory()));
        addField(fields, SystemTexts.FIELD_JAVA_HEAP.text(), Text.zhEn("已用内存", "Used memory").text(),
                formatBytes(runtime.totalMemory() - runtime.freeMemory()));

        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                addField(fields, SystemTexts.FIELD_SYSTEM_MEMORY.text(),
                        Text.zhEn("可用内存", "Available memory").text(), formatBytes(memoryInfo.availMem));
                addField(fields, SystemTexts.FIELD_SYSTEM_MEMORY.text(),
                        Text.zhEn("总内存", "Total memory").text(), formatBytes(memoryInfo.totalMem));
                addField(fields, SystemTexts.FIELD_SYSTEM_MEMORY.text(),
                        Text.zhEn("阈值", "Threshold").text(), formatBytes(memoryInfo.threshold));
                addField(fields, SystemTexts.FIELD_SYSTEM_MEMORY.text(),
                        Text.zhEn("低内存", "Low memory").text(),
                        memoryInfo.lowMemory ? SystemTexts.VALUE_YES.text() : SystemTexts.VALUE_NO.text());
            }
        }

        addField(fields, Text.zhEn("内存详细信息", "Detailed memory information").text(),
                Text.zhEn("内存详情", "Memory details").text(), readMeminfo());
    }

    private void addSystemPropertyFields(List<SystemFieldInfo> fields) {
        addSection(fields, SystemTexts.FIELD_SYSTEM_PROPS.text());
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
                addField(fields, SystemTexts.FIELD_SYSTEM_PROPS.text(), prop, value);
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
        ctx.println(Text.zhEn("=== 操作系统信息 ===", "=== OS information ===").text(), Colors.CYAN);
        ctx.println("", Colors.WHITE);
        printInfoLine(ctx, Text.zhEn("操作系统: ", "OS: ").text(), System.getProperty("os.name"));
        printInfoLine(ctx, Text.zhEn("系统版本: ", "OS version: ").text(), System.getProperty("os.version"));
        printInfoLine(ctx, Text.zhEn("架构: ", "Architecture: ").text(), System.getProperty("os.arch"));
        printInfoLine(ctx, SystemTexts.LABEL_PROCESSOR_COUNT.text(),
                String.valueOf(Runtime.getRuntime().availableProcessors()));
        ctx.println("", Colors.WHITE);

        ctx.println(Text.zhEn("=== Android信息 ===", "=== Android information ===").text(), Colors.CYAN);
        ctx.println("", Colors.WHITE);
        printInfoLine(ctx, Text.zhEn("设备制造商: ", "Manufacturer: ").text(), Build.MANUFACTURER);
        printInfoLine(ctx, Text.zhEn("设备品牌: ", "Brand: ").text(), Build.BRAND);
        printInfoLine(ctx, Text.zhEn("设备型号: ", "Model: ").text(), Build.MODEL);
        printInfoLine(ctx, Text.zhEn("设备名称: ", "Device: ").text(), Build.DEVICE);
        printInfoLine(ctx, Text.zhEn("产品名称: ", "Product: ").text(), Build.PRODUCT);
        printInfoLine(ctx, Text.zhEn("硬件名称: ", "Hardware: ").text(), Build.HARDWARE);
        printInfoLine(ctx, Text.zhEn("序列号: ", "Serial number: ").text(),
                SystemTexts.VALUE_PERMISSION_REQUIRED.text());
        printInfoLine(ctx, Text.zhEn("Android版本: ", "Android version: ").text(), Build.VERSION.RELEASE);
        printInfoLine(ctx, Text.zhEn("SDK版本: ", "SDK version: ").text(), String.valueOf(Build.VERSION.SDK_INT));
        printInfoLine(ctx, Text.zhEn("构建版本: ", "Build version: ").text(), Build.DISPLAY);
        printInfoLine(ctx, Text.zhEn("构建类型: ", "Build type: ").text(), Build.TYPE);
        printInfoLine(ctx, Text.zhEn("构建标签: ", "Build tags: ").text(), Build.TAGS);
        ctx.println("", Colors.WHITE);
    }

    private void printCpuInfo(CommandExecutor.CmdExecContext<?> ctx) {
        ctx.println(Text.zhEn("=== CPU信息 ===", "=== CPU information ===").text(), Colors.CYAN);
        ctx.println("", Colors.WHITE);
        printInfoLine(ctx, Text.zhEn("CPU架构: ", "CPU architecture: ").text(), System.getProperty("os.arch"));
        printInfoLine(ctx, Text.zhEn("CPU信息: ", "CPU information: ").text(), System.getProperty("ro.product.cpu"));
        printInfoLine(ctx, "CPU ABI: ", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown");
        printInfoLine(ctx, "CPU ABI2: ", Build.SUPPORTED_ABIS.length > 1 ? Build.SUPPORTED_ABIS[1] : "unknown");
        printInfoLine(ctx, SystemTexts.LABEL_PROCESSOR_COUNT.text(),
                String.valueOf(Runtime.getRuntime().availableProcessors()));
        ctx.println("", Colors.WHITE);

        ctx.println(Text.zhEn("=== CPU使用率 ===", "=== CPU usage ===").text(), Colors.CYAN);
        ctx.println("", Colors.WHITE);
        ctx.println(readCpuInfo(), Colors.GRAY);
    }

    private void printMemoryInfo(CommandExecutor.CmdExecContext<?> ctx) {
        ctx.println(Text.zhEn("=== 内存信息 ===", "=== Memory information ===").text(), Colors.CYAN);
        ctx.println("", Colors.WHITE);
        ctx.println(Text.zhEn("Java堆内存:", "Java heap:").text(), Colors.GREEN);
        Runtime runtime = Runtime.getRuntime();
        printInfoLine(ctx, Text.zhEn("  最大: ", "  Max: ").text(), formatBytes(runtime.maxMemory()));
        printInfoLine(ctx, Text.zhEn("  已分配: ", "  Allocated: ").text(), formatBytes(runtime.totalMemory()));
        printInfoLine(ctx, Text.zhEn("  空闲: ", "  Free: ").text(), formatBytes(runtime.freeMemory()));
        printInfoLine(ctx, Text.zhEn("  已用: ", "  Used: ").text(),
                formatBytes(runtime.totalMemory() - runtime.freeMemory()));
        ctx.println("", Colors.WHITE);

        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                ctx.println(Text.zhEn("系统内存:", "System memory:").text(), Colors.GREEN);
                printInfoLine(ctx, Text.zhEn("  可用: ", "  Available: ").text(), formatBytes(memoryInfo.availMem));
                printInfoLine(ctx, Text.zhEn("  总计: ", "  Total: ").text(), formatBytes(memoryInfo.totalMem));
                printInfoLine(ctx, Text.zhEn("  阈值: ", "  Threshold: ").text(), formatBytes(memoryInfo.threshold));
                ctx.print(Text.zhEn("  低内存: ", "  Low memory: ").text(), Colors.CYAN);
                ctx.println(memoryInfo.lowMemory ? SystemTexts.VALUE_YES.text() : SystemTexts.VALUE_NO.text(),
                        memoryInfo.lowMemory ? Colors.RED : Colors.GREEN);
                ctx.println("", Colors.WHITE);
            }
        }

        ctx.println(Text.zhEn("=== 内存详细信息 ===", "=== Detailed memory information ===").text(), Colors.CYAN);
        ctx.println("", Colors.WHITE);
        ctx.println(readMeminfo(), Colors.GRAY);
    }

    private void printSystemProperties(CommandExecutor.CmdExecContext<?> ctx) {
        ctx.println(Text.zhEn("=== 系统属性 ===", "=== System properties ===").text(), Colors.CYAN);
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
            result.append(Text.zhEn("无法读取 /proc/cpuinfo: %s\n", "Failed to read /proc/cpuinfo: %s\n")
                    .format(e.getMessage()));
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
            result.append(Text.zhEn("无法读取 /proc/meminfo: %s\n", "Failed to read /proc/meminfo: %s\n")
                    .format(e.getMessage()));
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
