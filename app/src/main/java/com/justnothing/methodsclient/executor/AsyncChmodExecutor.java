package com.justnothing.methodsclient.executor;

import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.functions.CmdUtils;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.io.RootProcessPool;
import com.justnothing.testmodule.utils.io.IOManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

// AI写的，用于异步执行chmod操作
// 其实感觉有点拆东墙补西墙。。。
public class AsyncChmodExecutor extends Logger {
    private static final int MAX_THREAD_POOL_SIZE = 2;
    private static final long SYNC_TIMEOUT_MS = 10000;
    private static ExecutorService executorService;
    private static final ConcurrentHashMap<String, Future<?>> pendingTasks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> completedTasks = new ConcurrentHashMap<>();
    private static final AtomicInteger threadCounter = new AtomicInteger(0);
    private static final AsyncChmodExecutor instance = new AsyncChmodExecutor();
    private static boolean async = true;
    
    // 缓存系统启动状态，避免频繁检查
    private static Boolean cachedBootCompleted = null;
    private static long lastBootCheckTime = 0;
    private static final long BOOT_CHECK_CACHE_DURATION = 5000; // 缓存5秒

    private AsyncChmodExecutor() {
        super();
    }
    
    @Override
    public String getTag() {
        return "AsyncChmodExecutor";
    }
    
    private static void logInfo(String message) {
        instance.info(message);
    }
    
    private static void logWarn(String message) {
        instance.warn(message);
    }
    
    private static void logError(String message) {
        instance.error(message);
    }
    
    private static void logError(String message, Throwable e) {
        instance.error(message, e);
    }
    
    private static void logDebug(String message) {
        instance.debug(message);
    }
    
    public static void initialize() {
        if (!isSystemBootCompleted()) {
            logInfo("系统启动未完成，跳过异步chmod执行器初始化");
            return;
        }
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE, r -> {
                Thread thread = new Thread(r, "AsyncChmod-" + threadCounter.incrementAndGet());
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            });
            logInfo("异步chmod执行器已初始化");
        }
    }

    public static void setAsync(boolean stat) {
        async = stat;
    }

    public static void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
            logInfo("异步chmod执行器已关闭");
        }
    }
    

    private static boolean isSystemBootCompleted() {
        long currentTime = System.currentTimeMillis();
        if (cachedBootCompleted != null && (currentTime - lastBootCheckTime) < BOOT_CHECK_CACHE_DURATION) {
            return cachedBootCompleted;
        }
        ThreadLocal<Boolean> inProgress = ThreadLocal.withInitial(() -> false);
        if (Boolean.TRUE.equals(inProgress.get())) return false;
        inProgress.set(true);
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method getMethod = systemPropertiesClass.getMethod("get", String.class);
            
            String bootCompleted = (String) getMethod.invoke(null, "sys.boot_completed");
            boolean isCompleted = "1".equals(bootCompleted);
            cachedBootCompleted = isCompleted;
            lastBootCheckTime = currentTime;
            
            if (isCompleted) {
                logInfo("系统启动已完成 (sys.boot_completed=1)");
            } else {
                logInfo("系统启动未完成 (sys.boot_completed=" + bootCompleted + ")");
            }
            return isCompleted;
        } catch (ClassNotFoundException e) {
            // 如果SystemProperties类不存在，回退到使用getprop命令
            logWarn("SystemProperties类不存在，回退到getprop命令");
            return isSystemBootCompletedFallbackNoLog();
        } catch (Exception e) {
            logWarn("AsyncChmodExecutor: 通过反射获取sys.boot_completed失败: " + e.getMessage());
            return isSystemBootCompletedFallbackNoLog();
        } finally {
            inProgress.set(false);
        }
    }

    private static boolean isSystemBootCompletedFallbackNoLog() {
        try {
            CmdUtils.CommandOutput result = CmdUtils.runRootCommand("getprop sys.boot_completed", 5000);
            
            if (result.succeed()) {
                String output = result.stdout().trim();
                return "1".equals(output);
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean chmodFile(String targetPath, String permissions, boolean recursive) {
        if (targetPath == null || targetPath.isEmpty()) {
            logWarn("chmodFile: 目标路径为空");
            return false;
        }

        String taskKey = targetPath + ":" + permissions + ":" + recursive;

        if (completedTasks.containsKey(taskKey)) {
            logDebug("chmodFile: 任务已完成，跳过执行: " + taskKey);
            return true;
        }

        if (pendingTasks.containsKey(taskKey)) {
            logDebug("chmodFile: 任务正在执行中: " + taskKey);
            return true;
        }

        boolean isZygotePhase = BootMonitor.isZygotePhase();
        boolean isBootCompleted = isSystemBootCompleted();

        if (isZygotePhase || !isBootCompleted) {
            logWarn("系统启动阶段，跳过chmod操作: " + targetPath +
                    " (Zygote阶段: " + isZygotePhase + ", 启动完成: " + isBootCompleted + ")");
            return isZygotePhase;
        }

        if (!async) {
            boolean result = executeChmodSync(targetPath, permissions, recursive);
            if (result) {
                completedTasks.put(taskKey, true);
            }
            return result;
        } else {
            if (executorService == null) initialize();
            return executeChmodAsync(targetPath, permissions, recursive, taskKey);
        }
    }

    private static boolean executeChmodSync(String targetPath, String permissions, boolean recursive) {
        long timeoutMs = SYNC_TIMEOUT_MS;
        int maxRetries = 2;
        int retryDelayMs = 500;
        
        String chmodCmd;
        if (recursive) {
            chmodCmd = String.format("chmod -R %s %s", permissions, targetPath);
        } else {
            chmodCmd = String.format("chmod %s %s", permissions, targetPath);
        }
        
        logInfo("同步执行chmod命令: " + chmodCmd + " (超时: " + timeoutMs + "ms, 重试: " + maxRetries + "次)");
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                IOManager.ProcessResult result = RootProcessPool.executeCommand(chmodCmd, timeoutMs);
                
                if (result.isSuccess()) {
                    logInfo("chmod执行成功, 命令内容: " + chmodCmd + ", 尝试: " + attempt);
                    return true;
                } else {
                    int exitCode = result.exitCode;
                    String stdout = result.stdout;
                    String stderr = result.stderr;
                    
                    if (exitCode == 10 && attempt < maxRetries) {
                        logWarn("chmod尝试 " + attempt + " 失败 (权限问题), 退出码: " + exitCode + 
                                ", 将重试...");
                    } else {
                        logWarn("chmod尝试 " + attempt + " 失败, 退出码: " + exitCode + 
                                ", stdout: " + stdout + 
                                ", stderr: " + stderr);
                    }
                }
            } catch (InterruptedException e) {
                logWarn("chmod尝试 " + attempt + " 超时: " + e.getMessage());
                if (attempt < maxRetries) {
                    logInfo("将重试chmod命令...");
                }
            } catch (Exception e) {
                logError("chmod执行失败: " + e.getMessage() + ", 命令: " + chmodCmd, e);
            }
            
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        logError("chmod命令[" + chmodCmd + "]所有尝试均失败");
        return false;
    }

    private static boolean executeChmodAsync(String targetPath, String permissions, boolean recursive, String taskKey) {
        String chmodCmd;
        if (recursive) {
            chmodCmd = String.format("chmod -R %s %s", permissions, targetPath);
        } else {
            chmodCmd = String.format("chmod %s %s", permissions, targetPath);
        }
        
        logInfo("异步执行chmod命令: " + chmodCmd);
        
        try {
            Future<?> future = executorService.submit(() -> {
                try {
                    boolean result = executeChmodSync(targetPath, permissions, recursive);
                    if (result) {
                        completedTasks.put(taskKey, true);
                    }
                    pendingTasks.remove(taskKey);
                } catch (Exception e) {
                    logError("异步chmod执行异常: " + e.getMessage(), e);
                    pendingTasks.remove(taskKey);
                }
            });
            
            pendingTasks.put(taskKey, future);
            return true;
        } catch (Exception e) {
            logError("提交异步chmod任务失败: " + e.getMessage(), e);
            return false;
        }
    }
}
