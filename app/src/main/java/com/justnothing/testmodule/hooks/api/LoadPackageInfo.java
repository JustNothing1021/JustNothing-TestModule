package com.justnothing.testmodule.hooks.api;

import android.content.pm.ApplicationInfo;

/**
 * 加载包信息，包装 {@code XC_LoadPackage.LoadPackageParam} 的字段。
 * <p>
 * 命令层和脚本引擎通过此类型访问加载包信息，无需直接依赖 Xposed API。
 * 字段名与 {@code LoadPackageParam} 保持一致，确保脚本兼容。
 * </p>
 */
public class LoadPackageInfo {

    private final String packageName;
    private final String processName;
    private final ClassLoader classLoader;
    private final ApplicationInfo appInfo;
    private final boolean isFirstApplication;

    public LoadPackageInfo(String packageName, String processName,
                           ClassLoader classLoader, ApplicationInfo appInfo,
                           boolean isFirstApplication) {
        this.packageName = packageName;
        this.processName = processName;
        this.classLoader = classLoader;
        this.appInfo = appInfo;
        this.isFirstApplication = isFirstApplication;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getProcessName() {
        return processName;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ApplicationInfo getAppInfo() {
        return appInfo;
    }

    public boolean isFirstApplication() {
        return isFirstApplication;
    }

    @Override
    public String toString() {
        return "LoadPackageInfo{packageName='" + packageName + "', processName='" + processName + "'}";
    }
}