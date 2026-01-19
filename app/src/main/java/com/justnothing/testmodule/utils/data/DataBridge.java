package com.justnothing.testmodule.utils.data;

import android.util.Log;

import com.justnothing.methodsclient.StreamClient;
import com.justnothing.testmodule.constants.AppEnvironment;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.hooks.HookEntry;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.io.IOManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class DataBridge {

    private static final class DataBridgeLogger extends Logger {
        @Override
        public String getTag() {
            return "DataBridge";
        }
    }

    private static final DataBridgeLogger logger = new DataBridgeLogger();

    private static final ReentrantLock lock = new ReentrantLock();

    private static File cachedDataDir = null;
    private static File cachedLogFile = null;
    private static boolean fileInitialized = false;
    private static boolean logSystemInitialized = false;

    private static final int LOG_BUFFER_SIZE = 200;
    private static final int MAX_LOG_LINE_LENGTH = 65536;
    private static final Queue<String> logBuffer = new LinkedList<>();
    private static int logBufferCount = 0;
    private static long totalBufferSize = 0;
    private static final long LOG_FLUSH_INTERVAL = 1000;
    private static final long MAX_BUFFER_SIZE = 100 * 1024; // 100KB
    private static long lastFlushTime = System.currentTimeMillis();
    private static String lastLogError = null;
    private static long lastLogErrorTime = 0;
    private static final long LOG_ERROR_REPORT_INTERVAL = 60000;
    private static String lastPermissionError = null;
    private static long lastPermissionErrorTime = 0;
    private static final long PERMISSION_ERROR_REPORT_INTERVAL = 120000;
    private static ExecutorService permissionFixExecutor = null;
    
    private static boolean logDirPermissionIssue = false;
    private static long lastPermissionCheckTime = 0;
    private static final long PERMISSION_CHECK_INTERVAL = 30000;
    
    private static final java.util.Map<String, Long> lastWarningTimes = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long WARNING_COOLDOWN_INTERVAL = 30000;

    private static JSONObject cachedModuleStatus = null;
    private static long moduleStatusCacheTime = 0;
    private static final long MODULE_STATUS_CACHE_TTL = 30000;
    
    private static JSONObject cachedPerformanceData = null;
    private static long performanceDataCacheTime = 0;
    private static final long PERFORMANCE_DATA_CACHE_TTL = 30000;
    private static JSONObject clientHookConfig = null;
    private static JSONObject serverHookConfig = null;

    public static File getDataDir() {
        if (cachedDataDir != null) {
            return cachedDataDir;
        }
        File dir = DataDirectoryManager.getDataDirectory();
        cachedDataDir = dir;
        return dir;
    }

    private static File getModuleStatusFile() {
        return new File(getDataDir(), FileDirectory.MODULE_STATUS_FILE_NAME);
    }

    private static File getPerformanceFile() {
        return new File(getDataDir(), FileDirectory.PERFORMANCE_DATA_FILE_NAME);
    }

    public static File getLogFile() {
        if (cachedLogFile != null) {
            return cachedLogFile;
        }
        cachedLogFile = new File(getDataDir(), FileDirectory.MODULE_LOG_FILE_NAME);
        return cachedLogFile;
    }

    private static File getClientHookConfigFile() {
        return new File(getDataDir(), FileDirectory.CLIENT_HOOK_CONFIG_FILE_NAME);
    }

    private static File getServerHookListFile() {
        return new File(getDataDir(), FileDirectory.SERVER_HOOK_LIST_CONFIG_NAME);
    }

    private static long getFileLastModified(File file) {
        if (file != null && file.exists()) {
            return file.lastModified();
        }
        return 0;
    }

    
    private static void logWithCooldown(String level, String message) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastWarningTimes.get(message);
        
        if (lastTime == null || (currentTime - lastTime) >= WARNING_COOLDOWN_INTERVAL) {
            lastWarningTimes.put(message, currentTime);
            switch (level) {
                case "warn":
                    logger.warn(message);
                    break;
                case "error":
                    logger.error(message);
                    break;
                case "info":
                    logger.info(message);
                    break;
                case "debug":
                    logger.debug(message);
                    break;
            }
        }
    }

    private static boolean isCacheValid(File file, long cacheTime, long cacheTTL) {
        if (cacheTime == 0) return false;
        long currentTime = System.currentTimeMillis();
        if ((currentTime - cacheTime) >= cacheTTL) {
            return false;
        }
        long fileModified = getFileLastModified(file);
        return fileModified <= 0 || fileModified <= cacheTime;
    }

    public static void writeServerHookStatus(JSONObject status) {
        lock.lock();
        try {
            File file = getModuleStatusFile();
            
            if (!DataDirectoryManager.ensureFileExistsWithPermissions(file, "Hook状态文件")) {
                logWithCooldown("warn", "无法确保Hook状态文件存在，跳过写入");
                return;
            }
            
            JSONObject mergedStatus = new JSONObject();
            
            if (file.exists() && file.length() > 0) {
                try {
                    String existingJson = IOManager.readFile(file.getAbsolutePath());
                    if (existingJson != null && !existingJson.trim().isEmpty()) {
                        try {
                            JSONObject existingStatus = new JSONObject(existingJson);
                            java.util.Iterator<String> keys = existingStatus.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                mergedStatus.put(key, existingStatus.get(key));
                            }
                        } catch (Exception e) {
                            logWithCooldown("warn", "解析现有模块状态失败: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logWithCooldown("warn", "读取现有模块状态失败: " + e.getMessage());
                }
            }
            
            java.util.Iterator<String> keys = status.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                mergedStatus.put(key, status.get(key));
            }
            
            IOManager.writeFile(file.getAbsolutePath(), mergedStatus.toString());
            
            cachedModuleStatus = mergedStatus;
            moduleStatusCacheTime = System.currentTimeMillis();
            logger.debug("模块状态已写入文件");
        } catch (Exception e) {
            reportLogError("写入模块状态失败: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public static JSONObject readServerHookStatus() {
        return readServerHookStatus(false);
    }

    public static JSONObject readServerHookStatus(boolean forceRefresh) {
        lock.lock();
        try {
            File file = getModuleStatusFile();
            if (!forceRefresh && isCacheValid(file, moduleStatusCacheTime, MODULE_STATUS_CACHE_TTL) && cachedModuleStatus != null) {
                return new JSONObject(cachedModuleStatus.toString());
            }
            
            if (!file.exists()) {
                cachedModuleStatus = new JSONObject();
                moduleStatusCacheTime = System.currentTimeMillis();
                return cachedModuleStatus;
            }
            
            String content = IOManager.readFile(file.getAbsolutePath());
            if (content != null) {
                cachedModuleStatus = new JSONObject(content);
                moduleStatusCacheTime = System.currentTimeMillis();
                return new JSONObject(cachedModuleStatus.toString());
            }
            return new JSONObject();
        } catch (Exception e) {
            reportLogError("读取模块状态失败: " + e.getMessage(), e);
            return new JSONObject();
        } finally {
            lock.unlock();
        }
    }

    public static void forceRefreshServerHookStatus() {
        lock.lock();
        try {
            cachedModuleStatus = null;
            moduleStatusCacheTime = 0;
            logger.info("强制刷新Hook状态缓存");
        } finally {
            lock.unlock();
        }
    }

    public static void writePerformanceData(JSONObject data) {
        lock.lock();
        try {
            File file = getPerformanceFile();
            
            if (!DataDirectoryManager.ensureFileExistsWithPermissions(file, "性能监测数据文件")) {
                logWithCooldown("warn", "无法确保性能数据文件存在，跳过写入");
                return;
            }
            
            IOManager.writeFile(file.getAbsolutePath(), data.toString());
            
            cachedPerformanceData = data;
            performanceDataCacheTime = System.currentTimeMillis();
            logger.debug("性能数据已写入文件");
        } catch (Exception e) {
            reportLogError("写入性能数据失败: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public static JSONObject readPerformanceData() {
        lock.lock();
        try {
            File file = getPerformanceFile();
            if (isCacheValid(file, performanceDataCacheTime, PERFORMANCE_DATA_CACHE_TTL) && cachedPerformanceData != null) {
                return new JSONObject(cachedPerformanceData.toString());
            }
            
            if (!file.exists()) {
                cachedPerformanceData = new JSONObject();
                performanceDataCacheTime = System.currentTimeMillis();
                return cachedPerformanceData;
            }
            
            String content = IOManager.readFile(file.getAbsolutePath());
            if (content != null) {
                cachedPerformanceData = new JSONObject(content);
                performanceDataCacheTime = System.currentTimeMillis();
                return new JSONObject(cachedPerformanceData.toString());
            }
            return new JSONObject();
        } catch (Exception e) {
            reportLogError("读取性能数据失败: " + e.getMessage(), e);
            return new JSONObject();
        } finally {
            lock.unlock();
        }
    }

    public static void forceRefreshPerformanceData() {
        lock.lock();
        try {
            cachedPerformanceData = null;
            performanceDataCacheTime = 0;
            logger.info("强制刷新性能数据缓存");
        } finally {
            lock.unlock();
        }
    }

    private static void reportLogError(String error, Exception e) {
        long currentTime = System.currentTimeMillis();
        
        if (error != null && (error.contains("Permission denied") || error.contains("权限不足"))) {
            if (error.equals(lastPermissionError) && (currentTime - lastPermissionErrorTime) < PERMISSION_ERROR_REPORT_INTERVAL) {
                return;
            }
            lastPermissionError = error;
            lastPermissionErrorTime = currentTime;
        }

        if (error != null && error.equals(lastLogError) && (currentTime - lastLogErrorTime) < LOG_ERROR_REPORT_INTERVAL) {
            return;
        }
        
        lastLogError = error;
        lastLogErrorTime = currentTime;
        
        if (logSystemInitialized) {
            logger.error(error, e);
        } else {
            Log.e("JustNothing[DataBridge]", error);
            if (e != null) {
                Log.e("JustNothing[DataBridge]", Log.getStackTraceString(e));
            }
        }
    }

    private static boolean ensureLogDirectoryExists() {
        File dir = getDataDir();
        if (!dir.exists()) {
            try {
                if (!dir.mkdirs()) {
                    reportLogError("创建日志目录失败, mkdirs返回false", null);
                    return false;
                }
                
                if (!BootMonitor.isZygotePhase()) {
                    DataDirectoryManager.setFilePermissions(dir, "777", "日志目录");
                }
            } catch (Exception e) {
                reportLogError("创建日志目录异常: " + e.getMessage(), e);
                return false;
            }
        }
        
        if (!BootMonitor.isZygotePhase() && (!dir.canRead() || !dir.canWrite() || !dir.canExecute())) {
            if (!DataDirectoryManager.setFilePermissions(dir, "777", "日志目录")) {
                reportLogError("日志目录权限不足", null);
                return false;
            }
        }
        
        return true;
    }

    private static boolean ensureLogFileExists(File file) {
        // 在系统启动阶段完全跳过文件操作，避免系统卡死
        if (BootMonitor.isZygotePhase()) {
            System.out.println("DataBridge: 系统启动阶段，跳过所有文件操作");
            return false; // 返回false，让调用方知道文件操作被跳过
        }
        
        // 如果文件已经存在且有权限，直接返回true
        if (file.exists() && file.canRead() && file.canWrite()) {
            return true;
        }
        
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    reportLogError("创建日志文件失败: createNewFile返回false", null);
                    return false;
                }
                
                boolean result = file.setReadable(true, false);
                result = result && file.setWritable(true, false);
                if (!result) logWithCooldown("warn", "为日志文件设置权限失败");
            } catch (Exception e) {
                reportLogError("创建日志文件异常: " + e.getMessage(), e);
                return false;
            }
        }
        
        if (!file.canRead() || !file.canWrite()) {
            DataDirectoryManager.setFilePermissions(file, "777", "日志文件");
        }
        
        return true;
    }

    private static void requestLogDirPermissionFix(File file) {
        if (BootMonitor.isZygotePhase()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (logDirPermissionIssue && (currentTime - lastPermissionCheckTime) < PERMISSION_CHECK_INTERVAL) {
            return;
        }

        logDirPermissionIssue = true;
        lastPermissionCheckTime = currentTime;

        final String dirPath = getDataDir().getAbsolutePath();
        final String filePath = file.getAbsolutePath();

        if (permissionFixExecutor == null) {
            permissionFixExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "DataBridge-PermissionFixThread");
                t.setDaemon(true);
                return t;
            });
        }

        permissionFixExecutor.submit(() -> {
            try {
                boolean dirSuccess = StreamClient.requestChmod(dirPath, "777", true);
                boolean fileSuccess = StreamClient.requestChmod(filePath, "777", false);

                if (dirSuccess && fileSuccess) {
                    logDirPermissionIssue = false;
                    logger.info("日志文件权限修复成功");
                } else {
                    reportLogError("修复日志文件权限失败", null);
                }
            } catch (Exception e) {
                reportLogError("修复日志文件权限异常: " + e.getMessage(), e);
            }
        });
    }

    private static void flushLogBuffer() {
        lastFlushTime = System.currentTimeMillis();

        if (logBuffer.isEmpty()) {
            return;
        }
        
        if (BootMonitor.isZygotePhase()) {
            logger.warn("系统启动阶段，跳过日志写入操作");
            logBufferCount = 0;
            return;
        }

        try {
            File file = getLogFile();

            if (!fileInitialized) {
                if (!ensureLogDirectoryExists()) {
                    logger.warn("日志目录不存在");
                    return;
                }

                if (!ensureLogFileExists(file)) {
                    requestLogDirPermissionFix(file);
                    logger.warn("日志目录权限不足, 已经尝试申请修复权限");
                    return;
                }

                fileInitialized = true;
            }

            StringBuilder sb = new StringBuilder();
            while (!logBuffer.isEmpty()) {
                String log = logBuffer.poll();
                sb.append(log).append("\n");
            }
            
            IOManager.appendFile(file.getAbsolutePath(), sb.toString());
            
            logBufferCount = 0;
            totalBufferSize = 0;
            lastFlushTime = System.currentTimeMillis();
            
            if (!logSystemInitialized) {
                logSystemInitialized = true;
            }
        } catch (Exception e) {
            String errorMsg = "写入日志失败: " + e.getMessage();
            reportLogError(errorMsg, e);
            if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
                File file = getLogFile();
                requestLogDirPermissionFix(file);
            }
        }
    }

    public static void appendLog(String log) {
        lock.lock();
        try {
            // 截断过长的日志行
            if (log != null && log.length() > MAX_LOG_LINE_LENGTH) {
                log = log.substring(0, MAX_LOG_LINE_LENGTH) + "... [TRUNCATED]";
            }
            
            // 计算日志行的大小
            long logSize = log != null ? log.length() * 2L : 0;
            
            // 如果缓冲区大小或数量超过限制，移除最旧的条目
            while ((logBufferCount >= LOG_BUFFER_SIZE || totalBufferSize + logSize > MAX_BUFFER_SIZE) && !logBuffer.isEmpty()) {
                String oldest = logBuffer.poll();
                logBufferCount--;
                totalBufferSize -= oldest != null ? oldest.length() * 2L : 0;
            }
            
            logBuffer.add(log);
            logBufferCount++;
            totalBufferSize += logSize;
            
            long currentTime = System.currentTimeMillis();
            boolean shouldFlush = logBufferCount >= LOG_BUFFER_SIZE || 
                                 totalBufferSize >= MAX_BUFFER_SIZE ||
                                 (currentTime - lastFlushTime) >= LOG_FLUSH_INTERVAL;
            
            
            if (shouldFlush) {
                flushLogBuffer();
                lastFlushTime = currentTime;
            }
        } finally {
            lock.unlock();
        }
    }

    public static String readLogs() {
        lock.lock();
        try {
            flushLogBuffer();
            
            File file = getLogFile();
            if (!file.exists()) {
                logWithCooldown("warn", "日志文件不存在");
                return "";
            }
            
            String content = IOManager.readFile(file.getAbsolutePath());
            return content != null ? content : "";
        } catch (Exception e) {
            reportLogError("读取日志失败: " + e.getMessage(), e);
            return "";
        } finally {
            lock.unlock();
        }
    }

    public static void clearLogs() {
        lock.lock();
        try {
            flushLogBuffer();
            
            File file = getLogFile();
            if (file.exists()) {
                if (!file.delete()) logWithCooldown("warn", "清除日志失败, delete返回false");
            }
        } catch (Exception e) {
            reportLogError("清除日志失败: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }


    public static void createDefaultClientHookConfig() {
        File configFile = getClientHookConfigFile();
        FileOutputStream fos = null;
        try {
            if (!configFile.exists()) {
                // 使用统一的方法确保文件存在并具有正确权限
                if (!DataDirectoryManager.ensureFileExistsWithPermissions(configFile, "客户端Hook配置文件")) {
                    logWithCooldown("warn", "无法确保Hook配置文件存在，跳过默认配置创建");
                    return;
                }
            }
            JSONObject defaultConfig;
            if (AppEnvironment.isHookEnv()) {
                defaultConfig = HookEntry.getDefaultHookConfig();
            } else {
                defaultConfig = new JSONObject();
            }
            IOManager.writeFile(configFile.getAbsolutePath(), defaultConfig.toString());
            logger.info("已创建客户端默认Hook配置文件");
        } catch (Exception e) {
            reportLogError("创建客户端默认Hook配置失败", e);
        }
    }

    public static void writeClientHookConfig(JSONObject config) {
        lock.lock();
        try {
            logger.debug("写入新配置: " + config.toString());

            File file = getClientHookConfigFile();
            if (!file.exists()) {
                if (!DataDirectoryManager.ensureFileExistsWithPermissions(file, "Hook客户端配置文件")) {
                    logWithCooldown("warn", "无法确保Hook配置文件存在，跳过配置写入");
                    return;
                }
            }
            
            IOManager.writeFile(file.getAbsolutePath(), config.toString());
            
            clientHookConfig = config;
            logger.info("Hook客户端配置已写入文件");
        } catch (Exception e) {
            reportLogError("写入Hook客户端配置失败: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public static JSONObject readClientHookConfig() {
        lock.lock();
        try {
            File file = getClientHookConfigFile();
            if (!file.exists()) {
                if (!DataDirectoryManager.ensureFileExistsWithPermissions(file, "Hook配置文件")) {
                    logWithCooldown("warn", "无法确保Hook配置文件存在，使用内存配置");
                    clientHookConfig = new JSONObject();
                    return clientHookConfig;
                }
                
                clientHookConfig = new JSONObject();
                return clientHookConfig;
            }
            
            String content = IOManager.readFile(file.getAbsolutePath());
            if (content != null) {
                clientHookConfig = new JSONObject(content);
                return new JSONObject(clientHookConfig.toString());
            }
            clientHookConfig = new JSONObject();
            return clientHookConfig;
        } catch (Exception e) {
            if (!Objects.requireNonNull(e.getMessage())
                    .contains("No such file or directory") && !e.getMessage().contains("mkdirs返回false")) {
                reportLogError("读取Hook配置失败: " + e.getMessage(), e);
            }
            return new JSONObject();
        } finally {
            lock.unlock();
        }
    }

    public static void writeServerHookConfig(JSONObject hookList) {
        lock.lock();
        try {
            File file = getServerHookListFile();
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new IOException("无法服务端Hook配置创建文件: " + file.getAbsolutePath() + ", createNewFile返回false");
                }
                DataDirectoryManager.setFilePermissions(file, "777", "服务端Hook配置文件");
            }

            IOManager.writeFile(file.getAbsolutePath(), hookList.toString());

            serverHookConfig = hookList;
            logger.info("服务端Hook配置已写入文件");
        } catch (Exception e) {
            reportLogError("写入服务端Hook配置失败: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public static JSONObject readServerHookConfig() {
        lock.lock();
        try {
            File file = getServerHookListFile();
            if (!file.exists()) {
                serverHookConfig = new JSONObject();
                return serverHookConfig;
            }
            
            String content = IOManager.readFile(file.getAbsolutePath());
            if (content != null) {
                serverHookConfig = new JSONObject(content);
                return new JSONObject(serverHookConfig.toString());
            }
            serverHookConfig = new JSONObject();
            return serverHookConfig;
        } catch (Exception e) {
            reportLogError("读取服务端Hook配置失败: " + e.getMessage(), e);
            return new JSONObject();
        } finally {
            lock.unlock();
        }
    }

    public static boolean clearAllData() {
        lock.lock();
        try {
            File dir = getDataDir();
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.delete()) logWithCooldown("warn", "清除数据时删除文件" + file.getAbsolutePath() + "失败");
                    }
                }
            }
            return true;
        } catch (Exception e) {
            reportLogError("清除数据失败: " + e.getMessage(), e);
            return false;
        } finally {
            lock.unlock();
        }
    }

}
