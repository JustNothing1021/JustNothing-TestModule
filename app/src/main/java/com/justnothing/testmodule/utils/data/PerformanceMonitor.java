package com.justnothing.testmodule.utils.data;

import com.justnothing.testmodule.utils.functions.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PerformanceMonitor extends Logger {
    private static final String TAG = "PerformanceMonitor";
    private boolean enabled;
    private long warningThreshold;
    private long criticalThreshold;
    private boolean thresholdAlertsEnabled;
    private long lastUpdateTime;
    private static final long UPDATE_INTERVAL = 1000;
    private final Map<String, Long> startTime = new HashMap<>();
    private final Map<String, HookUpdate> pendingUpdates = new HashMap<>();
    private static final int MAX_PENDING_UPDATES = 50;
    private int pendingUpdateCount = 0;
    private static final long WRITE_DELAY = 300;


    private boolean configLoaded = false;
    private boolean loadConfigPending = false;

    public PerformanceMonitor() {
        enabled = true;
        warningThreshold = 50;
        criticalThreshold = 100;
        thresholdAlertsEnabled = true;
        lastUpdateTime = System.currentTimeMillis();
        scheduleConfigLoad();
    }
    
    private void scheduleConfigLoad() {
        if (loadConfigPending) {
            return;
        }
        
        loadConfigPending = true;
        new Thread(() -> {
            try {
                Thread.sleep(WRITE_DELAY);
                loadConfig();
                configLoaded = true;
            } catch (InterruptedException e) {
                error("延迟加载配置被中断", e);
            } catch (Exception e) {
                error("延迟加载配置失败", e);
            } finally {
                loadConfigPending = false;
            }
        }, "ConfigLoadThread").start();
    }

    @Override
    public String getTag() {
        return TAG;
    }

    public void startHook(String hookName) {
        if (!enabled) return;
        if (BootMonitor.isZygotePhase()) return;
        startTime.put(hookName, System.currentTimeMillis());
    }

    public void endHook(String hookName) {
        if (!enabled) return;
        if (BootMonitor.isZygotePhase()) return;
        Long startTime = this.startTime.remove(hookName);
        if (startTime == null) {
            warn("没有记录" + hookName + "的开始时间, 将会默认为1");
            startTime = System.currentTimeMillis() - 1L;
        }
        long executionTime = System.currentTimeMillis() - startTime;
        updateHookStats(hookName, executionTime);
    }

    private void updateHookStats(String hookName, long executionTime) {
        try {
            synchronized (pendingUpdates) {
                HookUpdate update = pendingUpdates.get(hookName);
                if (update == null) {
                    update = new HookUpdate(hookName);
                    pendingUpdates.put(hookName, update);
                    pendingUpdateCount++;
                }
                update.addExecution(executionTime);
            }
            
            long currentTime = System.currentTimeMillis();
            if (pendingUpdateCount >= MAX_PENDING_UPDATES || (currentTime - lastUpdateTime) >= UPDATE_INTERVAL) {
                scheduleDelayedWrite();
            }
        } catch (Exception e) {
            error("更新性能数据失败", e);
        }
    }

    private void scheduleDelayedWrite() {
        new Thread(() -> {
            try {
                Thread.sleep(WRITE_DELAY);
                flushPendingUpdates();
            } catch (InterruptedException e) {
                error("延迟写入性能数据被中断", e);
            } catch (Exception e) {
                error("延迟写入性能数据失败", e);
            }
        }, "PerformanceDataWriteThread").start();
    }
    
    private void flushPendingUpdates() {
        if (BootMonitor.isZygotePhase()) {
            synchronized (pendingUpdates) {
                pendingUpdates.clear();
                pendingUpdateCount = 0;
            }
            return;
        }
        
        try {
            Map<String, HookUpdate> updatesToFlush;
            synchronized (pendingUpdates) {
                if (pendingUpdates.isEmpty()) {
                    return;
                }
                updatesToFlush = new HashMap<>(pendingUpdates);
                pendingUpdates.clear();
                pendingUpdateCount = 0;
            }
            
            JSONObject data = DataBridge.readPerformanceData();
            JSONArray hooksArray = data.optJSONArray("hooks");
            if (hooksArray == null) {
                hooksArray = new JSONArray();
                data.put("hooks", hooksArray);
            }

            for (HookUpdate update : updatesToFlush.values()) {
                boolean found = false;
                for (int i = 0; i < hooksArray.length(); i++) {
                    JSONObject hookData = hooksArray.getJSONObject(i);
                    if (update.hookName.equals(hookData.optString("name", ""))) {
                        int callCount = hookData.optInt("callCount", 0) + update.callCount;
                        long totalTime = hookData.optLong("totalTime", 0) + update.totalTime;
                        hookData.put("callCount", callCount);
                        hookData.put("totalTime", totalTime);
                        hookData.put("avgTime", totalTime / callCount);
                        
                        if (thresholdAlertsEnabled) {
                            long avgTime = totalTime / callCount;
                            if (avgTime >= criticalThreshold) {
                                warn("Hook " + update.hookName + " 平均执行时间超过临界阈值: " + avgTime + "ms");
                            } else if (avgTime >= warningThreshold) {
                                info("Hook " + update.hookName + " 平均执行时间超过警告阈值: " + avgTime + "ms");
                            }
                        }
                        
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    JSONObject hookData = new JSONObject();
                    hookData.put("name", update.hookName);
                    hookData.put("callCount", update.callCount);
                    hookData.put("totalTime", update.totalTime);
                    hookData.put("avgTime", update.totalTime / update.callCount);
                    hooksArray.put(hookData);
                }
            }

            data.put("hooks", hooksArray);
            
            File dataDir = DataBridge.getDataDir();
            if (dataDir != null && dataDir.exists()) {
                DataBridge.writePerformanceData(data);
            } else {
                info("数据目录不存在，跳过性能数据写入");
            }
            
            lastUpdateTime = System.currentTimeMillis();
        } catch (JSONException e) {
            error("批量更新性能数据失败", e);
        }
    }

    public long getAverageExecutionTime(String hookName) {
        try {
            JSONObject data = DataBridge.readPerformanceData();
            JSONArray hooksArray = data.optJSONArray("hooks");
            if (hooksArray != null) {
                for (int i = 0; i < hooksArray.length(); i++) {
                    JSONObject hookData = hooksArray.getJSONObject(i);
                    if (hookName.equals(hookData.optString("name", ""))) {
                        return hookData.optLong("avgTime", 0);
                    }
                }
            }
        } catch (JSONException e) {
            error("获取平均执行时间失败", e);
        }
        return 0;
    }

    public int getCallCount(String hookName) {
        try {
            JSONObject data = DataBridge.readPerformanceData();
            JSONArray hooksArray = data.optJSONArray("hooks");
            if (hooksArray != null) {
                for (int i = 0; i < hooksArray.length(); i++) {
                    JSONObject hookData = hooksArray.getJSONObject(i);
                    if (hookName.equals(hookData.optString("name", ""))) {
                        return hookData.optInt("callCount", 0);
                    }
                }
            }
        } catch (JSONException e) {
            error("获取调用次数失败", e);
        }
        return 0;
    }

    public long getTotalExecutionTime(String hookName) {
        try {
            JSONObject data = DataBridge.readPerformanceData();
            JSONArray hooksArray = data.optJSONArray("hooks");
            if (hooksArray != null) {
                for (int i = 0; i < hooksArray.length(); i++) {
                    JSONObject hookData = hooksArray.getJSONObject(i);
                    if (hookName.equals(hookData.optString("name", ""))) {
                        return hookData.optLong("totalTime", 0);
                    }
                }
            }
        } catch (JSONException e) {
            error("获取总执行时间失败", e);
        }
        return 0;
    }

    public Map<String, HookStats> getAllStats() {
        Map<String, HookStats> stats = new HashMap<>();
        try {
            JSONObject data = DataBridge.readPerformanceData();
            JSONArray hooksArray = data.optJSONArray("hooks");
            if (hooksArray != null) {
                for (int i = 0; i < hooksArray.length(); i++) {
                    JSONObject hookData = hooksArray.getJSONObject(i);
                    String hookName = hookData.optString("name", "");
                    if (!hookName.isEmpty()) {
                        stats.put(hookName, new HookStats(
                            hookName,
                            hookData.optInt("callCount", 0),
                            hookData.optLong("totalTime", 0),
                            hookData.optLong("avgTime", 0)
                        ));
                    }
                }
            }
        } catch (JSONException e) {
            error("获取所有统计数据失败", e);
        }
        return stats;
    }

    public void clearStats() {
        new Thread(() -> {
            try {
                Thread.sleep(WRITE_DELAY);
                JSONObject data = new JSONObject();
                data.put("enabled", enabled);
                data.put("hooks", new JSONArray());
                DataBridge.writePerformanceData(data);
            } catch (InterruptedException e) {
                error("清除统计数据被中断", e);
            } catch (JSONException e) {
                error("清除统计数据失败", e);
            }
        }, "ClearStatsThread").start();
    }

    public boolean isEnabled() {
        try {
            JSONObject data = DataBridge.readPerformanceData();
            return data.optBoolean("enabled", true);
        } catch (Exception e) {
            return enabled;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        scheduleConfigSave();
    }
    
    public long getWarningThreshold() {
        return warningThreshold;
    }
    
    public void setWarningThreshold(long warningThreshold) {
        this.warningThreshold = warningThreshold;
        scheduleConfigSave();
    }
    
    public long getCriticalThreshold() {
        return criticalThreshold;
    }
    
    public void setCriticalThreshold(long criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
        scheduleConfigSave();
    }
    
    public boolean isThresholdAlertsEnabled() {
        return thresholdAlertsEnabled;
    }
    
    public void setThresholdAlertsEnabled(boolean enabled) {
        this.thresholdAlertsEnabled = enabled;
        scheduleConfigSave();
    }
    

    private void loadConfig() {
        try {
            JSONObject data = DataBridge.readPerformanceData();
            enabled = data.optBoolean("enabled", true);
            warningThreshold = data.optLong("warningThreshold", 50);
            criticalThreshold = data.optLong("criticalThreshold", 100);
            thresholdAlertsEnabled = data.optBoolean("thresholdAlertsEnabled", true);
        } catch (Exception e) {
            error("加载配置失败", e);
        }
    }

    private void scheduleConfigSave() {
        new Thread(() -> {
            try {
                Thread.sleep(WRITE_DELAY);
                saveConfig();
            } catch (InterruptedException e) {
                error("延迟保存配置被中断", e);
            } catch (Exception e) {
                error("延迟保存配置失败", e);
            }
        }, "ConfigSaveThread").start();
    }

    private void saveConfig() {
        try {
            JSONObject data = DataBridge.readPerformanceData();
            data.put("enabled", enabled);
            data.put("warningThreshold", warningThreshold);
            data.put("criticalThreshold", criticalThreshold);
            data.put("thresholdAlertsEnabled", thresholdAlertsEnabled);
            DataBridge.writePerformanceData(data);
        } catch (JSONException e) {
            error("保存配置失败", e);
        }
    }

    public static class HookStats {
        public String name;
        public int callCount;
        public long totalTime;
        public long avgTime;

        public HookStats(String name, int callCount, long totalTime, long avgTime) {
            this.name = name;
            this.callCount = callCount;
            this.totalTime = totalTime;
            this.avgTime = avgTime;
        }
    }
    
    private static class HookUpdate {
        String hookName;
        int callCount;
        long totalTime;
        
        HookUpdate(String hookName) {
            this.hookName = hookName;
            this.callCount = 0;
            this.totalTime = 0;
        }
        
        void addExecution(long executionTime) {
            callCount++;
            totalTime += executionTime;
        }
    }
}
