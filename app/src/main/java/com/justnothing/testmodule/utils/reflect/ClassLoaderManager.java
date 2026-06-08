package com.justnothing.testmodule.utils.reflect;

import com.justnothing.testmodule.constants.HookConfig;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.io.IOManager;

import dalvik.system.DexClassLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClassLoaderManager {

    public static final String TAG = "ClassLoaderManager";
    private final ClassLoader defaultClassLoader;
    private static DexClassLoader apkClassLoader = null;
    private static final Object classLoaderLock = new Object();
    private static final String APK_PACKAGE_NAME = "com.justnothing.testmodule";
    private static final Logger logger = Logger.getLoggerForName(TAG);


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

    public static ClassLoader getApkClassLoader() {
        if (apkClassLoader != null) {
            return apkClassLoader;
        }

        synchronized (classLoaderLock) {
            if (apkClassLoader != null) {
                return apkClassLoader;
            }

            String modulePath = DataBridge.getModulePath();
            if (modulePath == null || modulePath.isEmpty()) {
                logger.debug("模块路径未设置，无法创建APK ClassLoader");
                return null;
            }

            try {
                File optimizedDir = new File(DataBridge.getDataDir(), "dex_opt");
                IOManager.createDirectory(optimizedDir);

                apkClassLoader = new DexClassLoader(
                    modulePath,
                    optimizedDir.getAbsolutePath(),
                    null,
                    getDefaultClassLoaderStatic()
                );

                logger.info("成功创建APK ClassLoader: " + modulePath);
                
                ClassResolver.registerApkClassLoader(apkClassLoader);
                
                return apkClassLoader;
            } catch (Exception e) {
                logger.error("创建APK ClassLoader失败: " + e.getMessage(), e);
                return null;
            }
        }
    }

    private static ClassLoader getDefaultClassLoaderStatic() {
        ClassLoader cl = ClassLoaderManager.class.getClassLoader();
        if (cl != null) return cl;
        cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) return cl;
        return ClassLoader.getSystemClassLoader();
    }

    public static List<String> getAllKnownPackages() {
        List<String> localPackages = new ArrayList<>();
        
        localPackages.add(APK_PACKAGE_NAME);
        
        try {
            JSONObject status = DataBridge.readServerHookStatus();
            JSONArray packages = status.optJSONArray(HookConfig.KEY_PROCESSED_PACKAGES);
            if (packages != null) {
                for (int i = 0; i < packages.length(); i++) {
                    String pkg = packages.getString(i);
                    if (!pkg.equals(APK_PACKAGE_NAME)) {
                        localPackages.add(pkg);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("读取模块状态失败", e);
        }
        logger.info("getAllKnownPackages返回: " + localPackages.size() + " 个包名");
        return localPackages;
    }


}