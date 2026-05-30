package com.justnothing.testmodule.command.agent;

import android.app.Application;
import android.content.Context;

import com.justnothing.testmodule.hooks.HookAPI;
import com.justnothing.testmodule.hooks.PackageHook;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.constants.FileDirectory;
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

    private static final String ACTIVATION_DIR = FileDirectory.AGENT_ACTIVATION_DIR;
    private static final long SENTINEL_INTERVAL_MS = 3000;
    private static final Set<String> activePackages = ConcurrentHashMap.newKeySet();
    
    private static final ConcurrentHashMap<String, Thread> sentinelThreads = new ConcurrentHashMap<>();

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
        setHookDisplayName("Agent sentinel");
        hookCallback(param -> {
            String packageName = param.packageName;
            
            // 去重检查：如果已经为该包创建了哨兵线程，跳过
            if (sentinelThreads.containsKey(packageName)) {
                debug("Sentinel thread already exists for package: " + packageName + ", skipping");
                return true;
            }
            
            startSentinelThread(packageName, param);
            return true;
        });
    }

    private void startSentinelThread(String packageName, XC_LoadPackage.LoadPackageParam lpparam) {
        Thread sentinel = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(SENTINEL_INTERVAL_MS);
                    if (isActivatedViaFile(packageName)) {
                        logger.info(packageName + " received activation signal, preparing to inject Agent");
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
                        logger.info("Injecting InspectionAgent for " + packageName + " (direct init)");
                        boolean success = InspectionAgent.ensureInitialized(app.getApplicationContext());
                        if (success) {
                            info("InspectionAgent injected into " + packageName);
                        } else {
                            warn("InspectionAgent injection failed for " + packageName);
                        }
                    } else {
                        logger.info("Application not ready, fallback to Hook Application.onCreate");
                        hookApplicationOnCreate(packageName);
                    }
                } else {
                    logger.info("ActivityThread is null, fallback to Hook");
                    hookApplicationOnCreate(packageName);
                }
            } catch (Throwable e) {
                logger.warn("Failed to get Context directly, fallback to Hook: " + e.getMessage());
                hookApplicationOnCreate(packageName);
            }
        }, "AgentSentinel-" + packageName);
        sentinel.setPriority(Thread.MIN_PRIORITY);
        sentinel.setDaemon(true);
        
        // 记录已创建的哨兵线程（去重关键！）
        sentinelThreads.put(packageName, sentinel);
        
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
