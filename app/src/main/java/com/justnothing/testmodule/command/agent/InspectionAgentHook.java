package com.justnothing.testmodule.command.agent;

import android.app.Application;
import android.content.Context;

import com.justnothing.testmodule.hooks.HookAPI;
import com.justnothing.testmodule.hooks.PackageHook;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.methodsclient.executor.AsyncChmodExecutor;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class InspectionAgentHook extends PackageHook {

    private static final Logger logger = Logger.getLoggerForName("InspectionAgentHook");

    private static final String ACTIVATION_DIR = "/data/local/tmp/methods/agent/activated";
    private static final long SENTINEL_INTERVAL_MS = 3000;
    private static final Set<String> activePackages = ConcurrentHashMap.newKeySet();

    public static boolean requestActivation(String packageName) {
        if (activePackages.contains(packageName)) return true;
        try {
            File dir = new File(ACTIVATION_DIR);
            dir.mkdirs();
            AsyncChmodExecutor.chmodFile(ACTIVATION_DIR, "777", true);
            File marker = new File(dir, packageName);
            if (!marker.exists()) {
                new FileOutputStream(marker).close();
                AsyncChmodExecutor.chmodFile(marker.getAbsolutePath(), "644", false);
            }
        } catch (Exception e) {
            logger.warn("写入激活标记失败: " + e.getMessage());
        }
        activePackages.add(packageName);
        logger.info("已请求激活 InspectionAgent: " + packageName);
        return false;
    }

    public static void deactivate(String packageName) {
        activePackages.remove(packageName);
        File marker = new File(ACTIVATION_DIR, packageName);
        if (marker.exists()) marker.delete();
        logger.info("已停用 InspectionAgent: " + packageName);
    }

    public static boolean isActive(String packageName) {
        return activePackages.contains(packageName) || isActivatedViaFile(packageName);
    }

    public static Set<String> getActivePackages() {
        return Collections.unmodifiableSet(activePackages);
    }

    private static boolean isActivatedViaFile(String packageName) {
        return new File(ACTIVATION_DIR, packageName).exists();
    }

    @Override
    protected void hookImplements() {
        hookCallback(param -> {
            startSentinelThread(param.packageName, param);
            return true;
        });
    }

    private void startSentinelThread(String packageName, XC_LoadPackage.LoadPackageParam lpparam) {
        Thread sentinel = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(SENTINEL_INTERVAL_MS);
                    if (isActivatedViaFile(packageName)) {
                        logger.info(packageName + " 收到激活信号, 准备注入 Agent");
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            try {
                Object activityThread = Class.forName("android.app.ActivityThread")
                        .getMethod("currentActivityThread").invoke(null);
                if (activityThread != null) {
                    Application app = (Application) activityThread.getClass()
                            .getMethod("getApplication").invoke(activityThread);
                    if (app != null) {
                        logger.info("正在为 " + packageName + " 注入 InspectionAgent (直接初始化)");
                        boolean success = InspectionAgent.ensureInitialized(app.getApplicationContext());
                        if (success) {
                            info("InspectionAgent 已注入 " + packageName);
                        } else {
                            warn("InspectionAgent 注入失败 " + packageName);
                        }
                    } else {
                        logger.info("Application 尚未就绪, 回退到 Hook Application.onCreate");
                        hookApplicationOnCreate(packageName);
                    }
                } else {
                    logger.info("ActivityThread 为空, 回退到 Hook");
                    hookApplicationOnCreate(packageName);
                }
            } catch (Throwable e) {
                logger.warn("直接获取 Context 失败, 回退到 Hook: " + e.getMessage());
                hookApplicationOnCreate(packageName);
            }
        }, "AgentSentinel-" + packageName);
        sentinel.setPriority(Thread.MIN_PRIORITY);
        sentinel.setDaemon(true);
        sentinel.start();
    }

    private void hookApplicationOnCreate(String packageName) {
        try {
            HookAPI.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Application app = (Application) param.thisObject;
                    Context ctx = app.getApplicationContext();
                    boolean success = InspectionAgent.ensureInitialized(ctx);
                    if (success) {
                        info("InspectionAgent 已注入 " + packageName);
                    } else {
                        warn("InspectionAgent 注入失败 " + packageName);
                    }
                }
            });
        } catch (Throwable e) {
            error("Hook Application.onCreate 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getHookName() {
        return "InspectionAgent";
    }

    @Override
    public String getTag() {
        return "agent";
    }
}
