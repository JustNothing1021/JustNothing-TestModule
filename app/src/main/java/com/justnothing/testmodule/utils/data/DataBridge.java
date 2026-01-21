package com.justnothing.testmodule.utils.data;

import android.util.Log;

import com.justnothing.methodsclient.StreamClient;
import com.justnothing.testmodule.constants.AppEnvironment;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.hooks.HookEntry;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.hooks.ServerHookConfig;
import com.justnothing.testmodule.utils.io.IOManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataBridge {

    private static final class DataBridgeLogger extends Logger {
        @Override
        public String getTag() {
            return "DataBridge";
        }
    }

    private static final DataBridgeLogger logger = new DataBridgeLogger();

    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static File cachedDataDir = null;
    private static File cachedLogFile = null;
    private static boolean fileInitialized = false;
    private static boolean logSystemInitialized = false;

    private static final Queue<String> logBuffer = new LinkedList<>();
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

    private static final Map<String, Long> lastWarningTimes = new ConcurrentHashMap<>();
    private static final long WARNING_COOLDOWN_INTERVAL = 30000;

    private static JSONObject cachedModuleStatus = null;
    private static long moduleStatusCacheTime = 0;
    private static final long MODULE_STATUS_CACHE_TTL = 30000;

    private static JSONObject cachedPerformanceData = null;
    private static long performanceDataCacheTime = 0;
    private static final long PERFORMANCE_DATA_CACHE_TTL = 30000;
    
    private static JSONObject clientHookConfig = null;
    private static long clientHookConfigCacheTime = 0;
    private static final long CLIENT_HOOK_CONFIG_CACHE_TTL = 10000;
    
    private static JSONObject serverHookConfig = null;
    private static long serverHookConfigCacheTime = 0;
    private static final long SERVER_HOOK_CONFIG_CACHE_TTL = 10000;

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

    
    private static void warnWithCooldown(String message) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastWarningTimes.get(message);
        
        if (lastTime == null || (currentTime - lastTime) >= WARNING_COOLDOWN_INTERVAL) {
            lastWarningTimes.put(message, currentTime);
            logger.warn(message);
        }
    }

    private static boolean isCacheValid(File file, long cacheTime, long cacheTTL) {
        if (cacheTime == 0) return false;
        long currentTime = System.currentTimeMillis();
        if ((currentTime - cacheTime) >= cacheTTL) {
            return false;
        }
        long fileModified = getFileLastModified(file);
        // 如果文件在缓存时间之后被修改，缓存无效
        return fileModified <= 0 || fileModified <= cacheTime;
    }

    public static void writeServerHookStatus(JSONObject status) {
        lock.writeLock().lock();
        try {
            File file = getModuleStatusFile();
            
            if (!DataDirectoryManager.ensureFileExistsWithPermissions(file, "Hook状态文件")) {
                warnWithCooldown("无法确保Hook状态文件存在，跳过写入");
                return;
            }
            
            JSONObject mergedStatus;
            
            if (file.exists() && file.length() > 0 && cachedModuleStatus != null) {
                mergedStatus = cachedModuleStatus;
            } else {
                mergedStatus = new JSONObject();
            }
            
            java.util.Iterator<String> keys = status.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                mergedStatus.put(key, status.get(key));
            }
            
            logger.debug("开始写入模块状态文件: " + file.getAbsolutePath());
            IOManager.writeFile(file.getAbsolutePath(), mergedStatus.toString());
            logger.debug("模块状态文件写入完成");
            
            cachedModuleStatus = mergedStatus;
            moduleStatusCacheTime = System.currentTimeMillis();
            logger.debug("模块状态已写入文件");
        } catch (Exception e) {
            reportLogError("写入模块状态失败: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static JSONObject readServerHookStatus() {
        return readServerHookStatus(false);
    }

    public static JSONObject readServerHookStatus(boolean forceRefresh) {
        lock.readLock().lock();
        try {
            File file = getModuleStatusFile();
            if (!forceRefresh && isCacheValid(file, moduleStatusCacheTime, MODULE_STATUS_CACHE_TTL) && cachedModuleStatus != null) {
                logger.debug("从缓存读取模块状态");
                return new JSONObject(cachedModuleStatus.toString());
            }
            
            if (!file.exists()) {
                logger.debug("模块状态文件不存在，创建空对象");
                cachedModuleStatus = new JSONObject();
                moduleStatusCacheTime = System.currentTimeMillis();
                return cachedModuleStatus;
            }
            
            logger.debug("开始读取模块状态文件: " + file.getAbsolutePath());
            String content = IOManager.readFile(file.getAbsolutePath());
            logger.debug("模块状态文件读取完成");
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
            lock.readLock().unlock();
        }
    }

    public static void forceRefreshServerHookStatus() {
        lock.writeLock().lock();
        try {
            cachedModuleStatus = null;
            moduleStatusCacheTime = 0;
            logger.info("强制刷新Hook状态缓存");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void writePerformanceData(JSONObject data) {
        lock.writeLock().lock();
        try {
            File file = getPerformanceFile();
            
            if (!DataDirectoryManager.ensureFileExistsWithPermissions(file, "性能监测数据文件")) {
                warnWithCooldown("无法确保性能数据文件存在，跳过写入");
                return;
            }
            
            logger.debug("开始写入性能数据文件: " + file.getAbsolutePath());
            IOManager.writeFile(file.getAbsolutePath(), data.toString());
            logger.debug("性能数据文件写入完成");
            
            cachedPerformanceData = data;
            performanceDataCacheTime = System.currentTimeMillis();
            logger.debug("性能数据已写入文件");
        } catch (Exception e) {
            reportLogError("写入性能数据失败: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static JSONObject readPerformanceData() {
        lock.readLock().lock();
        try {
            File file = getPerformanceFile();
            if (isCacheValid(file, performanceDataCacheTime, PERFORMANCE_DATA_CACHE_TTL) && cachedPerformanceData != null) {
                logger.debug("从缓存读取性能数据");
                return new JSONObject(cachedPerformanceData.toString());
            }
            
            if (!file.exists()) {
                logger.debug("性能数据文件不存在，创建空对象");
                cachedPerformanceData = new JSONObject();
                performanceDataCacheTime = System.currentTimeMillis();
                return cachedPerformanceData;
            }
            
            logger.debug("开始读取性能数据文件: " + file.getAbsolutePath());
            String content = IOManager.readFile(file.getAbsolutePath());
            logger.debug("性能数据文件读取完成");
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
            lock.readLock().unlock();
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
            // 如果日志系统未初始化，则直接打印到控制台
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
        if (BootMonitor.isZygotePhase()) {
            logger.debug("系统启动阶段，跳过所有文件操作");
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

        if (logBuffer.isEmpty()) {
            return;
        }

        if (BootMonitor.isZygotePhase()) {
            logger.debug("系统启动阶段，跳过日志写入操作");
            return;
        }

        try {
            File file = getLogFile();

            if (!fileInitialized) {
                if (!ensureLogDirectoryExists()) {
                    logger.debug("日志目录不存在");
                    return;
                }

                if (!ensureLogFileExists(file)) {
                    requestLogDirPermissionFix(file);
                    logger.debug("日志目录权限不足, 已经尝试申请修复权限");
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

    public static String readLogs() {
        return readLogs(1000);
    }

    public static String readLogs(int maxLines) {
        lock.readLock().lock();
        try {
            File file = getLogFile();
            if (!file.exists()) {
                warnWithCooldown("日志文件不存在: " + file.getAbsolutePath());
                return "";
            }
            
            long fileSize = file.length();
            logger.info("读取日志文件: " + file.getAbsolutePath() + ", 大小: " + fileSize + " bytes");
            
            try {
                String content = IOManager.readFile(file.getAbsolutePath(), StandardCharsets.UTF_8, -1, maxLines);
                if (content != null && !content.isEmpty()) {
                    logger.info("成功读取 " + maxLines + " 行日志，内容长度: " + content.length());
                } else {
                    logger.warn("读取日志内容为空");
                }
                return content != null ? content : "";
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("文件过大")) {
                    logger.warn("日志文件过大，无法读取完整内容");
                    return "[日志文件过大，请使用清除日志功能]";
                }
                logger.error("读取日志异常: " + e.getMessage(), e);
                throw e;
            }
        } catch (Exception e) {
            reportLogError("读取日志失败: " + e.getMessage(), e);
            return "";
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void clearLogs() {
        lock.writeLock().lock();
        try {
            flushLogBuffer();
            
            File file = getLogFile();
            if (file.exists()) {
                if (!file.delete()) logger.warn("清除日志失败, delete返回false");
            }
        } catch (Exception e) {
            reportLogError("清除日志失败: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }


    public static void createDefaultClientHookConfig() {
        File configFile = getClientHookConfigFile();
        try {
            if (!configFile.exists()) {
                logger.debug("客户端Hook配置文件不存在，准备创建: " + configFile.getAbsolutePath());
                if (!DataDirectoryManager.ensureFileExistsWithPermissions(configFile, "客户端Hook配置文件")) {
                    warnWithCooldown("无法确保Hook配置文件存在，跳过默认配置创建");
                    return;
                }
            }
            JSONObject defaultConfig;
            if (AppEnvironment.isHookEnv()) {
                defaultConfig = HookEntry.getDefaultHookConfig();
            } else {
                defaultConfig = new JSONObject();
            }
            logger.debug("开始写入客户端默认Hook配置文件: " + configFile.getAbsolutePath());
            IOManager.writeFile(configFile.getAbsolutePath(), defaultConfig.toString());
            logger.debug("客户端默认Hook配置文件写入完成");
            logger.info("已创建客户端默认Hook配置文件");
        } catch (Exception e) {
            reportLogError("创建客户端默认Hook配置失败", e);
        }
    }

    public static void writeClientHookConfig(JSONObject config) {
        lock.writeLock().lock();
        try {
            logger.debug("写入新配置: " + config.toString());

            File file = getClientHookConfigFile();
            if (!file.exists()) {
                logger.debug("Hook客户端配置文件不存在，准备创建: " + file.getAbsolutePath());
                if (!DataDirectoryManager.ensureFileExistsWithPermissions(file, "Hook客户端配置文件")) {
                    warnWithCooldown("无法确保Hook配置文件存在，跳过配置写入");
                    return;
                }
            }
            
            logger.debug("开始写入Hook客户端配置文件: " + file.getAbsolutePath());
            IOManager.writeFile(file.getAbsolutePath(), config.toString());
            logger.debug("Hook客户端配置文件写入完成");
            
            clientHookConfig = config;
            clientHookConfigCacheTime = System.currentTimeMillis();
            logger.info("Hook客户端配置已写入文件");
        } catch (Exception e) {
            reportLogError("写入Hook客户端配置失败: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static JSONObject readClientHookConfig() {
        return readClientHookConfig(false);
    }

    public static JSONObject readClientHookConfig(boolean forceRefresh) {
        lock.readLock().lock();
        try {
            File file = getClientHookConfigFile();
            
            if (!forceRefresh && isCacheValid(file, clientHookConfigCacheTime, CLIENT_HOOK_CONFIG_CACHE_TTL) && clientHookConfig != null) {
                logger.debug("从缓存读取客户端Hook配置");
                return new JSONObject(clientHookConfig.toString());
            }
            
            if (!file.exists()) {
                logger.debug("Hook配置文件不存在，准备创建: " + file.getAbsolutePath());
                if (!DataDirectoryManager.ensureFileExistsWithPermissions(file, "Hook配置文件")) {
                    warnWithCooldown("无法确保Hook配置文件存在，使用内存配置");
                    clientHookConfig = new JSONObject();
                    clientHookConfigCacheTime = System.currentTimeMillis();
                    return clientHookConfig;
                }
                
                clientHookConfig = new JSONObject();
                clientHookConfigCacheTime = System.currentTimeMillis();
                return clientHookConfig;
            }
            
            logger.debug("开始读取Hook配置文件: " + file.getAbsolutePath());
            String content = IOManager.readFile(file.getAbsolutePath());
            logger.debug("Hook配置文件读取完成");
            if (content != null && !content.trim().isEmpty()) {
                try {
                    clientHookConfig = new JSONObject(content);
                    clientHookConfigCacheTime = System.currentTimeMillis();
                    return new JSONObject(clientHookConfig.toString());
                } catch (Exception jsonException) {
                    logger.warn("Hook配置文件JSON解析失败，使用默认配置: " + jsonException.getMessage());
                    clientHookConfig = new JSONObject();
                    clientHookConfigCacheTime = System.currentTimeMillis();
                    return clientHookConfig;
                }
            }
            clientHookConfig = new JSONObject();
            clientHookConfigCacheTime = System.currentTimeMillis();
            return clientHookConfig;
        } catch (Exception e) {
            if (!Objects.requireNonNull(e.getMessage())
                    .contains("No such file or directory") && !e.getMessage().contains("mkdirs返回false")) {
                reportLogError("读取Hook配置失败: " + e.getMessage(), e);
            }
            return new JSONObject();
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void writeServerHookConfig(JSONObject hookList) {
        lock.writeLock().lock();
        try {
            File file = getServerHookListFile();
            if (!file.exists()) {
                logger.debug("服务端Hook配置文件不存在，准备创建: " + file.getAbsolutePath());
                if (!file.createNewFile()) {
                    throw new IOException("无法服务端Hook配置创建文件: " + file.getAbsolutePath() + ", createNewFile返回false");
                }
                DataDirectoryManager.setFilePermissions(file, "777", "服务端Hook配置文件");
            }

            logger.debug("开始写入服务端Hook配置文件: " + file.getAbsolutePath());
            IOManager.writeFile(file.getAbsolutePath(), hookList.toString());
            logger.debug("服务端Hook配置文件写入完成");

            serverHookConfig = hookList;
            serverHookConfigCacheTime = System.currentTimeMillis();
            ServerHookConfig.invalidateCache();
            logger.info("服务端Hook配置已写入文件");
        } catch (Exception e) {
            reportLogError("写入服务端Hook配置失败: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static JSONObject readServerHookConfig() {
        return readServerHookConfig(false);
    }

    public static JSONObject readServerHookConfig(boolean forceRefresh) {
        lock.readLock().lock();
        try {
            File file = getServerHookListFile();
            
            if (!forceRefresh && isCacheValid(file, serverHookConfigCacheTime, SERVER_HOOK_CONFIG_CACHE_TTL) && serverHookConfig != null) {
                logger.debug("从缓存读取服务端Hook配置");
                return new JSONObject(serverHookConfig.toString());
            }
            
            if (!file.exists()) {
                logger.debug("服务端Hook配置文件不存在，创建空对象");
                serverHookConfig = new JSONObject();
                serverHookConfigCacheTime = System.currentTimeMillis();
                return serverHookConfig;
            }
            
            logger.debug("开始读取服务端Hook配置文件: " + file.getAbsolutePath());
            String content = IOManager.readFile(file.getAbsolutePath());
            logger.debug("服务端Hook配置文件读取完成");
            if (content != null) {
                serverHookConfig = new JSONObject(content);
                serverHookConfigCacheTime = System.currentTimeMillis();
                return new JSONObject(serverHookConfig.toString());
            }
            serverHookConfig = new JSONObject();
            serverHookConfigCacheTime = System.currentTimeMillis();
            return serverHookConfig;
        } catch (Exception e) {
            reportLogError("读取服务端Hook配置失败: " + e.getMessage(), e);
            return new JSONObject();
        } finally {
            lock.readLock().unlock();
        }
    }

}
