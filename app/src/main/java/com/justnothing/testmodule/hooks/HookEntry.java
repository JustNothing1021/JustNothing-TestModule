package com.justnothing.testmodule.hooks;

import static com.justnothing.testmodule.constants.HookConfig.KEY_DESCRIPTION;
import static com.justnothing.testmodule.constants.HookConfig.KEY_DISPLAY_NAME;
import static com.justnothing.testmodule.constants.HookConfig.KEY_ENABLED;
import static com.justnothing.testmodule.constants.HookConfig.KEY_HOOK_COUNT;
import static com.justnothing.testmodule.constants.HookConfig.KEY_HOOK_DETAILS;
import static com.justnothing.testmodule.constants.HookConfig.KEY_IS_INITIALIZED;
import static com.justnothing.testmodule.constants.HookConfig.KEY_IS_MODULE_ACTIVE;
import static com.justnothing.testmodule.constants.HookConfig.KEY_NAME;
import static com.justnothing.testmodule.constants.HookConfig.KEY_PACKAGE_HOOK_COUNT;
import static com.justnothing.testmodule.constants.HookConfig.KEY_PROCESSED_PACKAGES;
import static com.justnothing.testmodule.constants.HookConfig.KEY_PROCESSED_PACKAGE_COUNT;
import static com.justnothing.testmodule.constants.HookConfig.KEY_TAG;
import static com.justnothing.testmodule.constants.HookConfig.KEY_TYPE;
import static com.justnothing.testmodule.constants.HookConfig.KEY_ZYGOTE_HOOK_COUNT;


import com.justnothing.testmodule.constants.AppEnvironment;
import com.justnothing.testmodule.hooks.service.ShellServiceHook;
import com.justnothing.testmodule.service.ShellService;
import com.justnothing.testmodule.hooks.agent.InspectionAgentHook;
import com.justnothing.testmodule.hooks.api.LoadPackageInfo;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.data.PerformanceMonitor;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static final String TAG = "HookEntry";
    private static final Logger logger = Logger.getLoggerForName(TAG);
    public static List<PackageHook> packageHooks = new ArrayList<>();
    public static List<ZygoteHook> zygoteHooks = new ArrayList<>();
    public static ShellService mService;
    public static boolean hooksSetup = false;
    private static boolean zygoteInitCompleted = false;
    private static final ConcurrentHashMap<String, ClassLoader> classLoaders = new ConcurrentHashMap<>();
    private static XC_LoadPackage.LoadPackageParam lastLoadPackageParam;
    private PerformanceMonitor performanceMonitor = null;
    private static final long FILE_OPERATION_DELAY = 1000;
    private static final long FILE_OPERATION_THROTTLE = 10000;
    private static long lastFileOperationTime = 0;
    private static final long HOOK_TIMEOUT_MS = 3000;
    private static final long HOOK_WARNING_MS = 500;

    static {
        packageHooks.add(new ShellServiceHook());
        packageHooks.add(new InspectionAgentHook());
        // 清单是编译期生成好的常量（HookPackRegistry），这里一个文件都不读。
        // ⚠ 别改回 getResources("hookpack.list")：这个静态块跑在 zygote 里，读资源会把
        // 模块自己的 APK 打开并留下 fd，重装模块后整机新进程全崩。详见 HookPackLoader 的类注释。
        HookPackLoader.register(packageHooks, zygoteHooks);
        AppEnvironment.setHookEnv();
        logger.info("HookEntry已加载");
    }

    private void recordHookLoad(String name, long begin, long end) {
        if (performanceMonitor == null) performanceMonitor = new PerformanceMonitor();
        performanceMonitor.markHookLoad(name, begin, end);
    }

    private static void setupHooks() {
        if (hooksSetup) return;
        hooksSetup = true;
        for (XposedBasicHook<?> hook : packageHooks)
            hook.setupHooks();
        for (XposedBasicHook<?> hook : zygoteHooks)
            hook.setupHooks();
    }

    public static JSONObject getDefaultHookConfig() {
        JSONObject data = new JSONObject();
        try {
            for (PackageHook hook : packageHooks) {
                data.put(hook.getHookName(), hook.getDefaultEnable());
            }

            for (ZygoteHook hook : zygoteHooks) {
                data.put(hook.getHookName(), hook.getDefaultEnable());
            }
        } catch (JSONException e) {
            logger.error("创建默认Hook配置失败", e);
            return new JSONObject();
        }
        return data;
    }

    private static void writeHookConfig() {
        try {
            
            JSONObject hookConfig = new JSONObject();

            for (PackageHook hook : packageHooks) {
                JSONObject hookInfo = new JSONObject();
                hookInfo.put(KEY_NAME, hook.getHookName());
                hookInfo.put(KEY_DISPLAY_NAME, hook.getHookDisplayName());
                hookInfo.put(KEY_DESCRIPTION, hook.getHookDescription());
                hookInfo.put(KEY_TYPE, "PackageHook");
                hookInfo.put(KEY_TAG, hook.getTag());
                hookInfo.put(KEY_ENABLED, hook.isHookEnabled());
                hookConfig.put(hook.getHookName(), hookInfo);
            }

            for (ZygoteHook hook : zygoteHooks) {
                JSONObject hookInfo = new JSONObject();
                hookInfo.put(KEY_NAME, hook.getHookName());
                hookInfo.put(KEY_DISPLAY_NAME, hook.getHookDisplayName());
                hookInfo.put(KEY_DESCRIPTION, hook.getHookDescription());
                hookInfo.put(KEY_TYPE, "ZygoteHook");
                hookInfo.put(KEY_TAG, hook.getTag());
                hookInfo.put(KEY_ENABLED, hook.isHookEnabled());
                hookConfig.put(hook.getHookName(), hookInfo);
            }

            DataBridge.writeServerHookConfig(hookConfig);
            logger.info("服务端Hook配置已写入文件，共 " + hookConfig.length() + " 个Hook");
        } catch (Exception e) {
            logger.error("服务端Hook配置写入失败，但不影响Hook功能，请检查目录权限", e);
        }
    }

    public static void updateHookStatus() {
        ThreadPoolManager.submitFastRunnable(() -> {
            try {
                JSONObject existingStatus = DataBridge.readServerHookStatus();
                
                JSONObject status = new JSONObject();
                status.put(KEY_IS_MODULE_ACTIVE, true);
                status.put(KEY_PACKAGE_HOOK_COUNT, packageHooks.size());
                status.put(KEY_ZYGOTE_HOOK_COUNT, zygoteHooks.size());
                
                JSONArray existingPackages = existingStatus.optJSONArray(KEY_PROCESSED_PACKAGES);
                if (existingPackages == null) {
                    existingPackages = new JSONArray();
                }
                
                Set<String> existingPackageSet = new HashSet<>();
                for (int i = 0; i < existingPackages.length(); i++) {
                    existingPackageSet.add(existingPackages.getString(i));
                }
                
                List<String> localPackages = getLocalPackages();
                for (String pkg : localPackages) {
                    if (!existingPackageSet.contains(pkg)) {
                        existingPackages.put(pkg);
                        existingPackageSet.add(pkg);
                    }
                }
                
                status.put(KEY_PROCESSED_PACKAGES, existingPackages);

                JSONArray existingHookDetails = existingStatus.optJSONArray(KEY_HOOK_DETAILS);
                Map<String, Integer> existingProcessedCounts = new HashMap<>();
                if (existingHookDetails != null) {
                    for (int i = 0; i < existingHookDetails.length(); i++) {
                        JSONObject hookJson = existingHookDetails.getJSONObject(i);
                        String name = hookJson.optString(KEY_NAME, "");
                        int count = hookJson.optInt(KEY_PROCESSED_PACKAGE_COUNT, 0);
                        existingProcessedCounts.put(name, count);
                    }
                }

                JSONArray hookDetails = new JSONArray();
                for (PackageHook hook : packageHooks) {
                    JSONObject detail = new JSONObject();
                    detail.put(KEY_NAME, hook.getHookName());
                    detail.put(KEY_DISPLAY_NAME, hook.getHookDisplayName());
                    detail.put(KEY_DESCRIPTION, hook.getHookDescription());
                    detail.put(KEY_TYPE, "PackageHook");
                    detail.put(KEY_IS_INITIALIZED, hook.isInitialized());
                    detail.put(KEY_HOOK_COUNT, hook.getHookCount());
                    String hookName = hook.getHookName();
                    int currentProcessed = hook.hookSucceedPackages + hook.hookFailurePackages;
                    // 按道理来说其实根本不需要requireNonNull，这里以防万一吧
                    int existingProcessed = Objects.requireNonNull(existingProcessedCounts.getOrDefault(hookName, 0));
                    detail.put(KEY_PROCESSED_PACKAGE_COUNT, existingProcessed + currentProcessed);
                    hookDetails.put(detail);
                }

                for (ZygoteHook hook : zygoteHooks) {
                    JSONObject detail = new JSONObject();
                    detail.put(KEY_NAME, hook.getHookName());
                    detail.put(KEY_DISPLAY_NAME, hook.getHookDisplayName());
                    detail.put(KEY_DESCRIPTION, hook.getHookDescription());
                    detail.put(KEY_TYPE, "ZygoteHook");
                    detail.put(KEY_IS_INITIALIZED, hook.isInitialized());
                    detail.put(KEY_HOOK_COUNT, hook.getHookCount());
                    detail.put(KEY_PROCESSED_PACKAGE_COUNT, 0);
                    hookDetails.put(detail);
                }
                status.put(KEY_HOOK_DETAILS, hookDetails);
                DataBridge.writeServerHookStatus(status);

            } catch (JSONException e) {
                logger.error("更新Hook状态失败", e);
            }
        });
    }

    public static void clearProcessedPackages() {
        ThreadPoolManager.submitFastRunnable(() -> {
            try {
                JSONObject existingStatus = DataBridge.readServerHookStatus();
                existingStatus.put(KEY_PROCESSED_PACKAGES, new JSONArray());
                DataBridge.writeServerHookStatus(existingStatus);
                logger.info("已清空已处理包列表");
            } catch (JSONException e) {
                logger.error("清空已处理包列表失败", e);
            }
        });
    }

    public static boolean executeFileOperations() {
        if (BootMonitor.isZygotePhase()) {
            logger.info("Zygote阶段，跳过文件操作");
            return false;
        }
        
        File dataDir = DataBridge.getDataDir();
        if (dataDir == null || !dataDir.exists()) {
            logger.info("数据目录不存在，跳过文件操作");
            return false;
        }
        
        // logger.info("执行服务端Hook数据文件保存操作");
        try {
            // logger.info("开始写入服务端Hook配置");
            writeHookConfig();
            // logger.info("服务端Hook配置写入完成");
        } catch (Exception e) {
            // logger.error("写入服务端Hook配置失败", e);
        }
        try {
            // logger.info("开始更新服务端Hook状态");
            updateHookStatus();
            // logger.info("服务端Hook状态更新完成");
        } catch (Exception e) {
            // logger.error("更新服务端Hook状态失败", e);
        }
        // logger.info("服务端Hook数据更新完成");
        return true;
    }

    private static void scheduleFileOperations() {
        // logger.info("延迟执行文件操作");
        ThreadPoolManager.schedule(HookEntry::executeFileOperations, FILE_OPERATION_DELAY, TimeUnit.MILLISECONDS);
    }


    // 研究半天得到的教训: 不要在initZygote的时候创建线程, 会因为不完整的系统加载爆掉
    // F DEBUG   : signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x8
    // F DEBUG   : Cause: null pointer dereference
    @Override
    public void initZygote(StartupParam startupParam) {
        if (zygoteInitCompleted) {
            logger.info("initZygote已执行过，跳过重复执行");
            return;
        }
        zygoteInitCompleted = true;
        
        DataBridge.setModulePath(startupParam.modulePath);

        BootMonitor.markZygoteInitStarted();
        logger.info("启动于initZygote阶段");
        
        logger.info("设备信息 - CPU核心数: " + Runtime.getRuntime().availableProcessors() +
                ", 最大内存: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB" +
                ", 可用内存: " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB");
        
        ThreadPoolManager.initialize();
        
        clearProcessedPackages();
        
        double begin = System.currentTimeMillis() / 1000.0f;
        try {
            logger.info("开始设置initZygote阶段的Hook...");
            int successCount = 0;
            int failCount = 0;


            for (ZygoteHook hook : zygoteHooks) {
                hook.setupHooks();
            }

            for (ZygoteHook hook : zygoteHooks) {
                String hookName = hook.getHookName();
                if (!hook.isHookEnabled()) {
                    logger.debug("Hook " + hookName + " 已被禁用，跳过安装");
                    continue;
                }
                boolean succeed = true;
                long hookBeginLong = System.currentTimeMillis();
                double hookBegin = hookBeginLong / 1000.0f;

                try {
                    if (hook.shouldLoad(startupParam)) {
                        succeed = hook.installHooks(startupParam);
                    }
                } catch (Throwable e) {
                    // Xposed 丢的是 MethodNotFoundError，是 Xposed 自己的，不 catch Throwable 会爆掉
                    logger.error("安装Zygote Hook " + hookName + " 时发生异常: " + e.getMessage(), e);
                    succeed = false;
                }
                long hookEndLong = System.currentTimeMillis();
                double hookEnd = hookEndLong / 1000.0f;

                recordHookLoad(hookName, hookBeginLong, hookEndLong);

                if (succeed) {
                    successCount++;
                    logger.debug("initZygote阶段的hook " + hook.getTag() + " 安装成功");
                } else {
                    failCount++;
                    logger.warn("initZygote阶段加载hook " + hook.getTag() + "出现错误");
                }

                logger.debug("initZygote阶段的hook " + hook.getTag() + "处理完成，耗时:" +
                        String.format(Locale.getDefault(), "%.3f", hookEnd - hookBegin));
            }

            logger.info("Zygote Hooks安装完成, 成功: " + successCount + ", 失败: " + failCount);
            BootMonitor.markZygoteInitCompleted();

        } catch (Throwable e) {
            logger.error("initZygote阶段发生严重错误", e);
        }

        double end = System.currentTimeMillis() / 1000.0f;
        logger.info("initZygote处理完成，耗时:" +
                String.format(Locale.getDefault(), "%.3f", end - begin));

        BootMonitor.logBootStatus();
        warnIfOwnApkHeldOpen();
    }

    /**
     * zygote 阶段的自检：fd 表里不该有我们自己的 APK。
     *
     * <p>zygote 一旦打开模块 APK 并留住 fd（典型来源：从 classloader 读资源），重装模块后
     * 那条 fd 会悬空，之后每个新进程都在 fork 的 specialize 阶段 SIGABRT —— 那是 native 层、
     * 在任何 Java 执行之前，打日志和 try/catch 都拦不住，现场只留一句 {@code Failed open}。
     * 详见 {@link HookPackLoader} 的类注释。</p>
     *
     * <p>这里主动查一遍，把"平时完全静默的炸弹"变成启动时就能在日志里看见的 error。
     * 此刻我们就在 zygote 进程里，所以 {@code /proc/self/fd} 就是 zygote 的 fd 表。</p>
     */
    private void warnIfOwnApkHeldOpen() {
        String ownApk = DataBridge.getModulePath();
        if (ownApk == null || ownApk.isEmpty()) {
            return;
        }
        File[] fds = new File("/proc/self/fd").listFiles();
        if (fds == null) {
            return;
        }
        for (File fd : fds) {
            try {
                Path target = Files.readSymbolicLink(fd.toPath());
                if (ownApk.equals(target.toString())) {
                    logger.error("zygote 正持有自己的 APK fd（" + fd.getName() + " -> " + target + "）。"
                            + "重装模块后这条 fd 会悬空，之后每个新进程启动都会 SIGABRT。"
                            + "zygote 阶段（静态块 / initZygote）不要碰 APK，也不要从 classloader 读资源。");
                }
            } catch (IOException e) {
                // 有几种 fd 读不了 targets（socket / anon_inode 等），跳过就是
            }
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        logger.info("通过handleLoadPackage处理" + param.packageName);
        lastLoadPackageParam = param;
        BootMonitor.markPackageLoadStarted(param.packageName, param.processName);
        boolean packageLoadSuccess = true;
        
        long packageStartTime = System.currentTimeMillis();
        
        try {
            classLoaders.put(param.packageName, param.classLoader);
            setupHooks();
            
            // logger.debug("线程池状态: " + ThreadPoolManager.getPoolStats());
            
            long hookBeginLong = System.currentTimeMillis();

            int successCount = 0;
            int failCount = 0;
            long maxHookDuration = 0;
            String slowestHookName = "";
            int timeoutCount = 0;
            int warningCount = 0;
            
            for (PackageHook hook : packageHooks) {
                String hookName = hook.getHookName();
                if (!hook.isHookEnabled()) {
                    logger.debug("Hook " + hookName + " 已被禁用，跳过安装");
                    continue;
                }
                boolean succeed = true;
                long hookStartTime = System.currentTimeMillis();
                try {
                    if (hook.shouldLoad(param)) {
                        succeed = hook.installHooks(param);
                    }
                } catch (Throwable e) {
                    // 同上
                    logger.error("安装Package Hook " + hookName + " 时发生异常: " + e.getMessage(), e);
                    succeed = false;
                }
                long hookEndTime = System.currentTimeMillis();
                long hookDuration = hookEndTime - hookStartTime;

                recordHookLoad(hookName, hookBeginLong, hookEndTime);
                
                if (hookDuration > maxHookDuration) {
                    maxHookDuration = hookDuration;
                    slowestHookName = hookName;
                }

                if (hookDuration > HOOK_TIMEOUT_MS) {
                    timeoutCount++;
                    logger.error(param.packageName + "加载" + hook.getTag() + "超时！" + 
                            String.format(Locale.getDefault(), "(%.3f秒)", hookDuration / 1000.0f) +
                            "，这可能导致应用卡死");
                } else if (hookDuration > HOOK_WARNING_MS) {
                    warningCount++;
                    logger.warn(param.packageName
                            + "加载" + hook.getTag() + "时间过长"
                            + String.format(Locale.getDefault(), "(%.3f秒)", hookDuration / 1000.0f));
                }

                if (succeed) {
                    successCount++;
                } else {
                    failCount++;
                    logger.warn(param.packageName + "加载" + hook.getTag() + "出现错误");
                }
            }
            
            long packageDuration = System.currentTimeMillis() - packageStartTime;
            
            logger.info(param.packageName + "处理完成，总耗时:" +
                    String.format(Locale.getDefault(), "%.3f", packageDuration / 1000.0f) + "秒" +
                    ", 成功: " + successCount + ", 失败: " + failCount +
                    ", 超时: " + timeoutCount + ", 警告: " + warningCount);
            
            if (maxHookDuration > HOOK_WARNING_MS) {
                logger.info(param.packageName + " 最耗时的Hook: " + slowestHookName + 
                        String.format(Locale.getDefault(), " (%.3f秒)", maxHookDuration / 1000.0f));
            }
            
            if (packageDuration > 5000) {
                logger.warn(param.packageName + " 总加载时间过长，可能影响用户体验");
                ThreadPoolManager.logDetailedPoolStats();
            }
            
            if (failCount > 0) {
                packageLoadSuccess = false;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFileOperationTime >= FILE_OPERATION_THROTTLE) {
                scheduleFileOperations();
                lastFileOperationTime = currentTime;
            }

        } catch (Throwable e) {
            long packageDuration = System.currentTimeMillis() - packageStartTime;
            logger.error("加载hook失败，总耗时:" + packageDuration + "ms，详细信息:\n", e);
            packageLoadSuccess = false;
        }
        
        BootMonitor.recordPackageLoad(packageLoadSuccess);
        BootMonitor.markPackageLoadCompleted(param.packageName, param.processName);
    }

    public static List<String> getLocalPackages() {
        return new ArrayList<>(classLoaders.keySet());
    }

    public static XC_LoadPackage.LoadPackageParam getLastLoadPackageParam() {
        return lastLoadPackageParam;
    }

    /**
     * 获取最后加载的包信息（不依赖 Xposed 类型）。
     * <p>命令层和脚本引擎应优先使用此方法。</p>
     */
    public static LoadPackageInfo getLastLoadPackageInfo() {
        XC_LoadPackage.LoadPackageParam p = lastLoadPackageParam;
        if (p == null) return null;
        return new LoadPackageInfo(
                p.packageName, p.processName,
                p.classLoader, p.appInfo,
                p.isFirstApplication
        );
    }

}