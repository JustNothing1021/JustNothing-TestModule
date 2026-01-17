package com.justnothing.testmodule.utils.data;

import android.content.Context;
import android.os.Environment;

import com.justnothing.testmodule.constants.HookConfig;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.hooks.ClientHookConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DataExporter extends Logger {
    private static final String TAG = "DataExporter";

    private final Context context;

    public DataExporter(Context ctx) {
        this.context = ctx.getApplicationContext();
    }
    
    @Override
    public String getTag() {
        return TAG;
    }

    public String exportAllData() throws IOException, JSONException {
        JSONObject allData = new JSONObject();
        allData.put("exportTime", getCurrentTimestamp());
        allData.put("hookConfig", exportHookConfig());
        allData.put("moduleStatus", exportModuleStatus());
        allData.put("performanceData", exportPerformanceData());
        allData.put("processedPackages", exportProcessedPackages());

        return saveToFile("all_data", allData.toString());
    }

    public String exportHookConfig() throws JSONException {
        JSONObject configData = new JSONObject();
        Map<String, Boolean> hookStates = ClientHookConfig.getAllHookStates();

        JSONArray hooks = new JSONArray();
        for (Map.Entry<String, Boolean> entry : hookStates.entrySet()) {
            JSONObject hook = new JSONObject();
            hook.put("name", entry.getKey());
            hook.put("enabled", entry.getValue());
            hooks.put(hook);
        }
        configData.put("hooks", hooks);
        configData.put("exportTime", getCurrentTimestamp());

        return configData.toString();
    }

    public String exportModuleStatus() throws JSONException {
        JSONObject statusData = new JSONObject();
        ModuleStatusMonitor monitor = new ModuleStatusMonitor(context);
        ModuleStatusMonitor.ModuleStatus status = monitor.getModuleStatus();

        statusData.put(HookConfig.KEY_IS_MODULE_ACTIVE, status.isModuleActive);
        statusData.put(HookConfig.KEY_HOOK_COUNT, status.hookCount);
        statusData.put(HookConfig.KEY_PACKAGE_HOOK_COUNT, status.packageHookCount);
        statusData.put(HookConfig.KEY_ZYGOTE_HOOK_COUNT, status.zygoteHookCount);
        statusData.put("processedPackagesCount", status.processedPackages.size());

        JSONArray hookDetails = new JSONArray();
        for (ModuleStatusMonitor.HookDetail detail : status.hookDetails) {
            JSONObject hook = new JSONObject();
            hook.put(HookConfig.KEY_NAME, detail.name);
            hook.put(HookConfig.KEY_TYPE, detail.type);
            hook.put(HookConfig.KEY_IS_INITIALIZED, detail.isInitialized);
            hook.put(HookConfig.KEY_HOOK_COUNT, detail.hookCount);
            hookDetails.put(hook);
        }
        statusData.put(HookConfig.KEY_HOOK_DETAILS, hookDetails);
        statusData.put("exportTime", getCurrentTimestamp());

        return statusData.toString();
    }

    public String exportPerformanceData() throws JSONException {
        JSONObject perfData = new JSONObject();
        PerformanceMonitor monitor = new PerformanceMonitor();
        Map<String, PerformanceMonitor.HookStats> stats = monitor.getAllStats();

        JSONArray hookStats = new JSONArray();
        for (Map.Entry<String, PerformanceMonitor.HookStats> entry : stats.entrySet()) {
            JSONObject stat = new JSONObject();
            stat.put("name", entry.getValue().name);
            stat.put("callCount", entry.getValue().callCount);
            stat.put("totalTime", entry.getValue().totalTime);
            stat.put("avgTime", entry.getValue().avgTime);
            hookStats.put(stat);
        }
        perfData.put("hookStats", hookStats);
        perfData.put("monitorEnabled", monitor.isEnabled());
        perfData.put("exportTime", getCurrentTimestamp());

        return perfData.toString();
    }

    public String exportProcessedPackages() throws JSONException {
        JSONObject packagesData = new JSONObject();
        ModuleStatusMonitor monitor = new ModuleStatusMonitor(context);
        ModuleStatusMonitor.ModuleStatus status = monitor.getModuleStatus();
        List<String> packages = status.processedPackages;

        JSONArray packagesArray = new JSONArray();
        for (String pkg : packages) {
            packagesArray.put(pkg);
        }
        packagesData.put("packages", packagesArray);
        packagesData.put("count", packages.size());
        packagesData.put("exportTime", getCurrentTimestamp());

        return packagesData.toString();
    }

    public String saveToFile(String fileName, String content) throws IOException {
        File exportDir = new File(Environment.getExternalStorageDirectory(), FileDirectory.EXPORT_DIR_NAME);
        if (!exportDir.exists()) {
            if (!exportDir.mkdirs()) {
                throw new IOException("无法创建导出目录: " + exportDir.getAbsolutePath());
            }
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fullFileName = fileName + "_" + timestamp + ".json";
        File file = new File(exportDir, fullFileName);

        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();

        debug("数据已导出到: " + file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    public File getExportDirectory() {
        File exportDir = new File(Environment.getExternalStorageDirectory(), FileDirectory.EXPORT_DIR_NAME);
        if (!exportDir.exists()) {
            if (!exportDir.mkdirs()) {
                error("无法创建导出目录: " + exportDir.getAbsolutePath());
            }
        }
        return exportDir;
    }

    public List<File> getExportedFiles() {
        File exportDir = getExportDirectory();
        File[] files = exportDir.listFiles();
        if (files == null) {
            return List.of();
        }
        return List.of(files);
    }

    public boolean deleteExportedFile(File file) {
        if (file.exists() && Objects.equals(file.getParentFile(), getExportDirectory())) {
            return file.delete();
        }
        return false;
    }

    public boolean clearAllExports() {
        File exportDir = getExportDirectory();
        File[] files = exportDir.listFiles();
        if (files == null) {
            return true;
        }
        boolean allDeleted = true;
        for (File file : files) {
            if (!file.delete()) {
                allDeleted = false;
            }
        }
        return allDeleted;
    }
}
