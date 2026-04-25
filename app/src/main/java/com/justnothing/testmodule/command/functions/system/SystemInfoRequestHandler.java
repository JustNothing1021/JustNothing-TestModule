package com.justnothing.testmodule.command.functions.system;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import com.justnothing.testmodule.protocol.json.handler.RequestHandler;
import com.justnothing.testmodule.protocol.json.model.SystemFieldInfo;
import com.justnothing.testmodule.protocol.json.request.SystemInfoRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.SystemInfoResult;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SystemInfoRequestHandler implements RequestHandler<SystemInfoRequest, SystemInfoResult> {

    private static final Logger logger = Logger.getLoggerForName("SystemInfoHandler");

    @Override
    public String getCommandType() {
        return "SystemInfo";
    }

    @Override
    public SystemInfoRequest parseRequest(JSONObject obj) {
        return new SystemInfoRequest().fromJson(obj);
    }

    @Override
    public SystemInfoResult createResult(String requestId) {
        return new SystemInfoResult(requestId);
    }

    @Override
    public SystemInfoResult handle(SystemInfoRequest request) {
        logger.debug("处理系统信息请求");

        SystemInfoResult result = new SystemInfoResult(request.getRequestId());

        try {
            List<SystemFieldInfo> fields = new ArrayList<>();

            collectOsInfo(fields);
            collectCpuInfo(fields);
            collectMemoryInfo(fields);
            collectSystemProperties(fields);

            result.setFields(fields);
            logger.info("系统信息收集完成, 共 " + fields.size() + " 个字段");
        } catch (Exception e) {
            logger.error("获取系统信息失败", e);
            result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "获取系统信息失败: " + e.getMessage()));
        }

        return result;
    }

    private void collectOsInfo(List<SystemFieldInfo> fields) {
        addField(fields, "os_info", "OS Name", System.getProperty("os.name"));
        addField(fields, "os_info", "OS Version", System.getProperty("os.version"));
        addField(fields, "os_info", "Architecture", System.getProperty("os.arch"));
        addField(fields, "os_info", "Processor Count", String.valueOf(Runtime.getRuntime().availableProcessors()));
        addField(fields, "os_info", "Manufacturer", Build.MANUFACTURER);
        addField(fields, "os_info", "Brand", Build.BRAND);
        addField(fields, "os_info", "Model", Build.MODEL);
        addField(fields, "os_info", "Device", Build.DEVICE);
        addField(fields, "os_info", "Product", Build.PRODUCT);
        addField(fields, "os_info", "Hardware", Build.HARDWARE);
        addField(fields, "os_info", "Serial Number", "Requires Permission");
        addField(fields, "os_info", "Android Version", Build.VERSION.RELEASE);
        addField(fields, "os_info", "SDK Version", String.valueOf(Build.VERSION.SDK_INT));
        addField(fields, "os_info", "Build Display", Build.DISPLAY);
        addField(fields, "os_info", "Build Type", Build.TYPE);
        addField(fields, "os_info", "Build Tags", Build.TAGS);
    }

    private void collectCpuInfo(List<SystemFieldInfo> fields) {
        addField(fields, "cpu_info", "CPU Architecture", System.getProperty("os.arch"));
        addField(fields, "cpu_info", "CPU Info", System.getProperty("ro.product.cpu"));
        addField(fields, "cpu_info", "CPU ABI", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown");
        addField(fields, "cpu_info", "CPU ABI2", Build.SUPPORTED_ABIS.length > 1 ? Build.SUPPORTED_ABIS[1] : "unknown");
        addField(fields, "cpu_info", "Processor Count", String.valueOf(Runtime.getRuntime().availableProcessors()));

        String cpuInfoRaw = readCpuInfo();
        if (cpuInfoRaw != null && !cpuInfoRaw.isEmpty()) {
            addField(fields, "cpu_info", "/proc/cpuinfo", cpuInfoRaw.trim());
        }
    }

    private void collectMemoryInfo(List<SystemFieldInfo> fields) {
        Runtime runtime = Runtime.getRuntime();
        addField(fields, "memory_info", "Java Heap Max", formatBytes(runtime.maxMemory()));
        addField(fields, "memory_info", "Java Heap Total", formatBytes(runtime.totalMemory()));
        addField(fields, "memory_info", "Java Heap Free", formatBytes(runtime.freeMemory()));
        addField(fields, "memory_info", "Java Heap Used", formatBytes(runtime.totalMemory() - runtime.freeMemory()));

        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                addField(fields, "memory_info", "System Memory Available", formatBytes(memoryInfo.availMem));
                addField(fields, "memory_info", "System Memory Total", formatBytes(memoryInfo.totalMem));
                addField(fields, "memory_info", "System Memory Threshold", formatBytes(memoryInfo.threshold));
                addField(fields, "memory_info", "Low Memory", memoryInfo.lowMemory ? "Yes" : "No");
            }
        }

        String meminfoRaw = readMeminfo();
        if (meminfoRaw != null && !meminfoRaw.isEmpty()) {
            addField(fields, "memory_info", "/proc/meminfo", meminfoRaw.trim());
        }
    }

    private void collectSystemProperties(List<SystemFieldInfo> fields) {
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
            if (value != null && !value.isEmpty()) {
                addField(fields, "system_props", prop, value);
            }
        }
    }

    private void addField(List<SystemFieldInfo> fields, String category, String label, String value) {
        if (value != null && !value.isEmpty()) {
            fields.add(new SystemFieldInfo(category, label, value));
        }
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
            result.append("Unable to read /proc/cpuinfo: ").append(e.getMessage()).append("\n");
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
            result.append("Unable to read /proc/meminfo: ").append(e.getMessage()).append("\n");
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
            @SuppressLint("PrivateApi")
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
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
