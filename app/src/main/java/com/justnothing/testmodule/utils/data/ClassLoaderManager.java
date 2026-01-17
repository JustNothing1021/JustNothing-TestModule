package com.justnothing.testmodule.utils.data;

import com.justnothing.testmodule.constants.HookConfig;
import com.justnothing.testmodule.utils.functions.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ClassLoaderManager {
    private final ClassLoader defaultClassLoader;

    private static final class ManagerLogger extends Logger {
        @Override
        public String getTag() {
            return "ClassLoaderManager";
        }
    }

    private static final ManagerLogger logger = new ManagerLogger();

    public ClassLoaderManager() {
        defaultClassLoader = getDefaultClassLoader();
    }

    public ClassLoader getDefaultClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) return cl;
        cl = ClassLoader.getSystemClassLoader();
        if (cl != null) return cl;
        return getClass().getClassLoader();
    }

    public ClassLoader getClassLoaderForPackage(String packageName) {
        if (packageName == null) {
            logger.warn("packageName为null, 将会使用默认的ClassLoader");
            return defaultClassLoader;
        }
        
        try {
            JSONObject status = DataBridge.readServerHookStatus();
            JSONArray packages = status.optJSONArray(HookConfig.KEY_PROCESSED_PACKAGES);
            if (packages != null) {
                for (int i = 0; i < packages.length(); i++) {
                    if (packages.getString(i).equals(packageName)) {
                        logger.debug("在本地找到了" + packageName + "的ClassLoader");
                        return defaultClassLoader;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("读取模块状态失败", e);
        }

        return defaultClassLoader;
    }

    public static List<String> getAllKnownPackages() {
        List<String> localPackages = new ArrayList<>();
        try {
            JSONObject status = DataBridge.readServerHookStatus();
            JSONArray packages = status.optJSONArray(HookConfig.KEY_PROCESSED_PACKAGES);
            if (packages != null) {
                for (int i = 0; i < packages.length(); i++) {
                    localPackages.add(packages.getString(i));
                }
            }
        } catch (Exception e) {
            logger.error("读取模块状态失败", e);
        }
        logger.info("getAllKnownPackages返回: " + localPackages.size() + " 个包名");
        return localPackages;
    }


}