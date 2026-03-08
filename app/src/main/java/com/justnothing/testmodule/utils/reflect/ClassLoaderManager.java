package com.justnothing.testmodule.utils.reflect;

import com.justnothing.testmodule.constants.HookConfig;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.functions.Logger;

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
                logger.warn("模块路径未设置，无法创建APK ClassLoader");
                return getDefaultClassLoaderStatic();
            }

            try {
                File optimizedDir = new File(DataBridge.getDataDir(), "dex_opt");
                optimizedDir.mkdirs();

                apkClassLoader = new DexClassLoader(
                    modulePath,
                    optimizedDir.getAbsolutePath(),
                    null,
                    getDefaultClassLoaderStatic()
                );

                logger.info("成功创建APK ClassLoader: " + modulePath);
                
                ClassResolver.registerClassLoader(apkClassLoader);
                
                return apkClassLoader;
            } catch (Exception e) {
                logger.error("创建APK ClassLoader失败: " + e.getMessage(), e);
                return getDefaultClassLoaderStatic();
            }
        }
    }

    private static ClassLoader getDefaultClassLoaderStatic() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) return cl;
        cl = ClassLoader.getSystemClassLoader();
        if (cl != null) return cl;
        return ClassLoaderManager.class.getClassLoader();
    }

    public static Class<?> loadClassFromApk(String className) throws ClassNotFoundException {
        ClassLoader loader = getApkClassLoader();
        if (loader == null) {
            throw new ClassNotFoundException("无法获取APK ClassLoader");
        }
        return Class.forName(className, true, loader);
    }

    public static Object createInstanceFromApk(String className) throws Exception {
        Class<?> clazz = loadClassFromApk(className);
        return clazz.getDeclaredConstructor().newInstance();
    }

    public ClassLoader getClassLoaderForPackage(String packageName) {
        if (packageName == null) {
            logger.warn("packageName为null, 将会使用默认的ClassLoader");
            return defaultClassLoader;
        }
        
        if (APK_PACKAGE_NAME.equals(packageName)) {
            logger.debug("请求APK ClassLoader: " + packageName);
            ClassLoader apkLoader = getApkClassLoader();
            if (apkLoader != null) {
                logger.debug("返回APK ClassLoader");
                return apkLoader;
            }
            logger.warn("APK ClassLoader未创建，使用默认ClassLoader");
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
        logger.warn("在本地没有找到" + packageName + "的ClassLoader, 将会使用默认的ClassLoader");
        return defaultClassLoader;
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