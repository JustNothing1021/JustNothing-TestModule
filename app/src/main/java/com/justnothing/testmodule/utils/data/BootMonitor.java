package com.justnothing.testmodule.utils.data;

import com.justnothing.testmodule.constants.AppEnvironment;
import com.justnothing.testmodule.utils.functions.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class BootMonitor {
    private static final String TAG = "BootMonitor";
    private static final Logger logger = Logger.getLoggerForName(TAG);
    private static final AtomicBoolean zygoteInitStarted = new AtomicBoolean(false);
    private static final AtomicBoolean zygoteInitCompleted = new AtomicBoolean(false);
    private static final AtomicLong zygoteInitStartTime = new AtomicLong(0);
    private static final AtomicLong zygoteInitEndTime = new AtomicLong(0);
    
    private static final AtomicBoolean hooksSetupCompleted = new AtomicBoolean(false);
    private static final AtomicLong hooksSetupStartTime = new AtomicLong(0);
    private static final AtomicLong hooksSetupEndTime = new AtomicLong(0);
    
    private static final AtomicBoolean packageLoadStarted = new AtomicBoolean(false);
    private static final AtomicBoolean packageLoadCompleted = new AtomicBoolean(false);
    private static final AtomicBoolean firstLoadPackageStarted = new AtomicBoolean(false);
    private static final AtomicLong packageLoadStartTime = new AtomicLong(0);
    private static final AtomicLong packageLoadEndTime = new AtomicLong(0);
    
    private static int totalPackagesLoaded = 0;
    private static int successfulPackageLoads = 0;
    private static int failedPackageLoads = 0;

    
    public static void markZygoteInitStarted() {
        if (zygoteInitStarted.compareAndSet(false, true)) {
            zygoteInitStartTime.set(System.currentTimeMillis());
            logger.info("=========== Zygote初始化开始");
        }
    }
    
    public static void markZygoteInitCompleted() {
        if (zygoteInitCompleted.compareAndSet(false, true)) {
            zygoteInitEndTime.set(System.currentTimeMillis());
            long duration = zygoteInitEndTime.get() - zygoteInitStartTime.get();
            logger.info("============== Zygote初始化完成，耗时: " + duration + "ms");
        }
    }


    public static void markPackageLoadStarted(String pkgName, String procName) {
        firstLoadPackageStarted.set(true);
        if (packageLoadStarted.compareAndSet(false, true)) {
            packageLoadStartTime.set(System.currentTimeMillis());
            logger.info("============ 处理包 " + pkgName + ", 进程名称" + procName);
        }
    }
    
    public static void markPackageLoadCompleted(String pkgName, String procName) {
        if (packageLoadCompleted.compareAndSet(false, true)) {
            packageLoadEndTime.set(System.currentTimeMillis());
            long duration = packageLoadEndTime.get() - packageLoadStartTime.get();
            logger.info("========= 包 " + pkgName + " 的进程 " + procName + " 加载完成，耗时: " + duration + "ms");
        }
    }
    
    public static boolean isZygotePhase() {
        if (!AppEnvironment.isHookEnv()) return false;
        return (zygoteInitStarted.get() && !zygoteInitCompleted.get());
    }
    
    public static void recordPackageLoad(boolean success) {
        totalPackagesLoaded++;
        if (success) {
            successfulPackageLoads++;
        } else {
            failedPackageLoads++;
        }
    }
    
    public static void logBootStatus() {
        logger.info("================= 当前启动状态");
        logger.info("Zygote初始化: " + (zygoteInitCompleted.get() ? "完成" : "未完成"));
        if (zygoteInitCompleted.get()) {
            long duration = zygoteInitEndTime.get() - zygoteInitStartTime.get();
            logger.info("  耗时: " + duration + "ms");
        }
        
        logger.info("Hooks设置: " + (hooksSetupCompleted.get() ? "完成" : "未完成"));
        if (hooksSetupCompleted.get()) {
            long duration = hooksSetupEndTime.get() - hooksSetupStartTime.get();
            logger.info("  耗时: " + duration + "ms");
        }
        
        logger.info("包加载: " + (packageLoadCompleted.get() ? "完成" : "进行中"));
        logger.info("  总包数: " + totalPackagesLoaded);
        logger.info("  成功: " + successfulPackageLoads);
        logger.info("  失败: " + failedPackageLoads);
        if (packageLoadCompleted.get()) {
            long duration = packageLoadEndTime.get() - packageLoadStartTime.get();
            logger.info("  耗时: " + duration + "ms");
        }
    }
    
}