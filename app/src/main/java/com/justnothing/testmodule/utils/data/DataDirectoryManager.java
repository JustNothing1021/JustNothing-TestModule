package com.justnothing.testmodule.utils.data;

import static com.justnothing.testmodule.constants.FileDirectory.METHODS_DATA_DIR;

import android.content.Context;

import com.justnothing.methodsclient.executor.AsyncChmodExecutor;
import com.justnothing.testmodule.constants.AppEnvironment;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.ui.ErrorDialog;
import com.justnothing.testmodule.utils.functions.CmdUtils;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.File;

public class DataDirectoryManager {

    public static final String TAG = "DataDirectoryManager";
    public static boolean determinedDataDirectory = false;
    public static String dataDirectory;
    public static File defaultDataDir = new File(METHODS_DATA_DIR);

    public static class DataDirectoryManagerLogger extends Logger {
        @Override
        public String getTag() {
            return TAG;
        }
    }

    public static Context context;

    public static void setContext(Context ctx) {
        context = ctx;
    }

    private static final DataDirectoryManagerLogger logger = new DataDirectoryManagerLogger();

    private static boolean hasPermission(File file) {
        if (file.isDirectory()) {
            if (!file.canRead() || !file.canWrite() || !file.canExecute()) {
                boolean status = file.setReadable(true);
                status = status && file.setWritable(true);
                status = status && file.setExecutable(true);
                return status;
            } else {
                return true;
            }
        }
        return false;
    }

    public static void deleteFallbackRecordFile() {
        File fallbackRecordFile = new File(FileDirectory.TEMP_FALLBACK_RECORD_FILE_DIR);
        if (fallbackRecordFile.isFile()) {
            if (!fallbackRecordFile.delete()) {
                if (AsyncChmodExecutor.chmodFile(fallbackRecordFile.getPath(), "777", true)) {
                    if (!fallbackRecordFile.delete()) {
                        logger.warn("无法删除临时目录记录文件");
                    }
                }
            }
        }
    }


    public static File getDataDirectory() {

        if (BootMonitor.isZygotePhase()) {
            logger.info("Zygote阶段，跳过数据目录操作，返回默认目录");
            return defaultDataDir;
        }

        if (AppEnvironment.isHookEnv()) {
            logger.info("Hook环境，跳过数据目录操作，返回默认目录");
            return defaultDataDir;
        }
        
        logger.info("尝试获取数据目录");
        
        if (determinedDataDirectory) {
            File cachedDir = new File(dataDirectory);
            if (cachedDir.exists() && cachedDir.canRead() && cachedDir.canWrite() && cachedDir.canExecute()) {
                return cachedDir;
            } else {
                logger.warn("缓存的数据目录不可用，重新查找");
                determinedDataDirectory = false;
                dataDirectory = null;
            }
        }

        logger.error("无法找到可用的数据目录");
        if (context != null) {
            try {
                ErrorDialog.showError(context,
                    "找不到地方存放程序的临时数据了...请激活模块后重试",
                    true);
            } catch (Exception e) {
                logger.error("显示错误对话框失败", e);
            }
        }
        return defaultDataDir;
    }

    public static String getMethodsCmdlineDataDirectory() {
        return METHODS_DATA_DIR;
    }


    public static boolean ensureFileDirectoryExists(File file) {
        try {
            if (file == null) return false;
            
            File parentDir = file.getParentFile();
            if (parentDir == null) return false;
            
            if (!parentDir.exists()) {
                logger.info("文件目录不存在，尝试创建: " + parentDir.getAbsolutePath());
                if (!parentDir.mkdirs()) {
                    logger.error("创建文件目录失败: " + parentDir.getAbsolutePath());
                    return false;
                }
                
                setFilePermissions(parentDir, "777", "文件目录");
            }
            
            return parentDir.exists() && parentDir.canRead() && parentDir.canWrite() && parentDir.canExecute();
        } catch (Exception e) {
            logger.error("确保文件目录存在时发生异常", e);
            return false;
        }
    }

    /**
     * 设置文件或目录权限，如果失败会尝试使用root权限
     */
    public static boolean setFilePermissions(File file, String permissions, String description) {
        if (file == null || BootMonitor.isZygotePhase()) {
            return false;
        }

        try {
            boolean chmodSuccess = AsyncChmodExecutor.chmodFile(file.getAbsolutePath(), permissions, true);
            if (chmodSuccess) {
                logger.info("为" + description + "设置权限成功, 路径: " + file.getAbsolutePath());
                return true;
            }
            
            logger.warn("AsyncChmodExecutor设置权限失败，尝试使用root权限: " + file.getAbsolutePath());
            CmdUtils.CommandOutput chmodResult = CmdUtils.runRootCommand("chmod " + permissions + " " + file.getAbsolutePath(), 5000);
            if (chmodResult.succeed()) {
                logger.info("使用root权限为" + description + "设置权限成功, 路径:  " + file.getAbsolutePath());
                return true;
            } else {
                logger.warn("为" + description + "设置权限失败, 路径:  " + file.getAbsolutePath());
                return false;
            }
        } catch (Exception e) {
            logger.error("设置文件权限时发生异常", e);
            return false;
        }
    }

    /**
     * 确保文件存在并具有正确权限
     */
    public static boolean ensureFileExistsWithPermissions(File file, String description) {
        try {
            if (file == null) {
                return false;
            }
            
            // 确保文件目录存在
            if (!ensureFileDirectoryExists(file)) {
                logger.error("无法确保文件目录存在: " + file.getAbsolutePath());
                return false;
            }
            
            // 如果文件不存在，创建文件
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    logger.error("创建文件失败: " + file.getAbsolutePath());
                    return false;
                }
                
                // 设置文件权限
                setFilePermissions(file, "777", description);
            }
            
            return file.exists() && file.canRead() && file.canWrite();
        } catch (Exception e) {
            logger.error("确保文件存在时发生异常", e);
            return false;
        }
    }

}
