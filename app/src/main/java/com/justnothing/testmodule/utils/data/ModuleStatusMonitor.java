package com.justnothing.testmodule.utils.data;

import android.content.Context;
import android.content.pm.PackageManager;

import com.justnothing.testmodule.constants.HookConfig;
import com.justnothing.testmodule.utils.functions.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ModuleStatusMonitor extends Logger {
    private static final String TAG = "ModuleStatusMonitor";
    
    private final Context context;

    public ModuleStatusMonitor(Context ctx) {
        this.context = ctx.getApplicationContext();
    }
    
    @Override
    public String getTag() {
        return TAG;
    }

    public ModuleStatus getModuleStatus() {
        return getModuleStatus(false);
    }

    public ModuleStatus getModuleStatus(boolean forceRefresh) {
        ModuleStatus status = new ModuleStatus();
        
        try {
            JSONObject statusJson = DataBridge.readServerHookStatus(forceRefresh);
            
            status.isModuleActive = statusJson.optBoolean(HookConfig.KEY_IS_MODULE_ACTIVE, false);
            status.hookCount = statusJson.optInt(HookConfig.KEY_PACKAGE_HOOK_COUNT, 0) + statusJson.optInt(HookConfig.KEY_ZYGOTE_HOOK_COUNT, 0);
            status.packageHookCount = statusJson.optInt(HookConfig.KEY_PACKAGE_HOOK_COUNT, 0);
            status.zygoteHookCount = statusJson.optInt(HookConfig.KEY_ZYGOTE_HOOK_COUNT, 0);
            
            status.processedPackages = new ArrayList<>();
            JSONArray packagesArray = statusJson.optJSONArray(HookConfig.KEY_PROCESSED_PACKAGES);
            if (packagesArray != null) {
                for (int i = 0; i < packagesArray.length(); i++) {
                    status.processedPackages.add(packagesArray.getString(i));
                }
            }
            
            status.hookDetails = new ArrayList<>();
            JSONArray hooksArray = statusJson.optJSONArray(HookConfig.KEY_HOOK_DETAILS);
            
            // 如果hookDetails为空或数量与总hook数不匹配，尝试修复数据
            if (hooksArray == null || hooksArray.length() == 0 || hooksArray.length() != status.hookCount) {
                info("Hook详细信息不完整，尝试修复数据");
                
                // 如果hook总数大于0但hookDetails为空，说明数据不一致
                if (status.hookCount > 0 && (hooksArray == null || hooksArray.length() == 0)) {
                    warn("检测到hook总数(" + status.hookCount + ")与hook详细信息数量(" + 
                         (hooksArray != null ? hooksArray.length() : 0) + ")不匹配");
                    
                    // 这里无法直接调用HookEntry.updateHookStatus()，但可以记录日志提醒
                    error("Hook状态数据不一致，请确保HookEntry.executeFileOperations()被正确调用");
                }
            }
            
            if (hooksArray != null) {
                for (int i = 0; i < hooksArray.length(); i++) {
                    JSONObject hookJson = hooksArray.getJSONObject(i);
                    HookDetail detail = new HookDetail();
                    detail.name = hookJson.optString(HookConfig.KEY_NAME, "");
                    detail.displayName = hookJson.optString(HookConfig.KEY_DISPLAY_NAME, "");
                    detail.description = hookJson.optString(HookConfig.KEY_DESCRIPTION, "");
                    detail.type = hookJson.optString(HookConfig.KEY_TYPE, "");
                    detail.isInitialized = hookJson.optBoolean(HookConfig.KEY_IS_INITIALIZED, false);
                    detail.hookCount = hookJson.optInt(HookConfig.KEY_HOOK_COUNT, 0);
                    detail.processedPackageCount = hookJson.optInt(HookConfig.KEY_PROCESSED_PACKAGE_COUNT, 0);
                    status.hookDetails.add(detail);
                }
            }
        } catch (JSONException e) {
            status.isModuleActive = false;
            status.hookCount = 0;
            status.packageHookCount = 0;
            status.zygoteHookCount = 0;
            status.processedPackages = new ArrayList<>();
            status.hookDetails = new ArrayList<>();
        }
        
        return status;
    }

    public void updateModuleStatus(boolean isActive, int packageHookCount, int zygoteHookCount) {
        try {
            JSONObject statusJson = DataBridge.readServerHookStatus();
            statusJson.put(HookConfig.KEY_IS_MODULE_ACTIVE, isActive);
            statusJson.put(HookConfig.KEY_PACKAGE_HOOK_COUNT, packageHookCount);
            statusJson.put(HookConfig.KEY_ZYGOTE_HOOK_COUNT, zygoteHookCount);
            statusJson.put(HookConfig.KEY_HOOK_COUNT, packageHookCount + zygoteHookCount);
            DataBridge.writeServerHookStatus(statusJson);
        } catch (JSONException e) {
            error("更新模块状态失败", e);
        }
    }

    public void addProcessedPackage(String packageName) {
        try {
            JSONObject statusJson = DataBridge.readServerHookStatus();
            JSONArray packagesArray = statusJson.optJSONArray(HookConfig.KEY_PROCESSED_PACKAGES);
            if (packagesArray == null) {
                packagesArray = new JSONArray();
                statusJson.put(HookConfig.KEY_PROCESSED_PACKAGES, packagesArray);
            }
            
            boolean found = false;
            for (int i = 0; i < packagesArray.length(); i++) {
                if (packageName.equals(packagesArray.getString(i))) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                packagesArray.put(packageName);
            }
            
            DataBridge.writeServerHookStatus(statusJson);
        } catch (JSONException e) {
            error("添加处理包失败", e);
        }
    }

    public void addHookDetail(String name, String displayName, String description, String type, boolean isInitialized, int hookCount, int processedPackageCount) {
        try {
            JSONObject statusJson = DataBridge.readServerHookStatus();
            JSONArray hooksArray = statusJson.optJSONArray(HookConfig.KEY_HOOK_DETAILS);
            if (hooksArray == null) {
                hooksArray = new JSONArray();
                statusJson.put(HookConfig.KEY_HOOK_DETAILS, hooksArray);
            }
            
            boolean found = false;
            for (int i = 0; i < hooksArray.length(); i++) {
                JSONObject hookJson = hooksArray.getJSONObject(i);
                if (name.equals(hookJson.optString(HookConfig.KEY_NAME, ""))) {
                    hookJson.put(HookConfig.KEY_DISPLAY_NAME, displayName);
                    hookJson.put(HookConfig.KEY_DESCRIPTION, description);
                    hookJson.put(HookConfig.KEY_TYPE, type);
                    hookJson.put(HookConfig.KEY_IS_INITIALIZED, isInitialized);
                    hookJson.put(HookConfig.KEY_HOOK_COUNT, hookCount);
                    hookJson.put(HookConfig.KEY_PROCESSED_PACKAGE_COUNT, processedPackageCount);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                JSONObject hookJson = new JSONObject();
                hookJson.put(HookConfig.KEY_NAME, name);
                hookJson.put(HookConfig.KEY_DISPLAY_NAME, displayName);
                hookJson.put(HookConfig.KEY_DESCRIPTION, description);
                hookJson.put(HookConfig.KEY_TYPE, type);
                hookJson.put(HookConfig.KEY_IS_INITIALIZED, isInitialized);
                hookJson.put(HookConfig.KEY_HOOK_COUNT, hookCount);
                hookJson.put(HookConfig.KEY_PROCESSED_PACKAGE_COUNT, processedPackageCount);
                hooksArray.put(hookJson);
            }
            
            DataBridge.writeServerHookStatus(statusJson);
        } catch (JSONException e) {
            error("添加Hook详情失败", e);
        }
    }

    private boolean isXposedModuleActive() {
        try {
            context.getPackageManager().getPackageInfo("de.robv.android.xposed.installer", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static class ModuleStatus {
        public boolean isModuleActive;
        public int hookCount;
        public int packageHookCount;
        public int zygoteHookCount;
        public List<String> processedPackages;
        public List<HookDetail> hookDetails;
    }

    public static class HookDetail {
        public String name;
        public String displayName;
        public String description;
        public String type;
        public boolean isInitialized;
        public Integer hookCount;
        public Integer processedPackageCount;
    }
}
