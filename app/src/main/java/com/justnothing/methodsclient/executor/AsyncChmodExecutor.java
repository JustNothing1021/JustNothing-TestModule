package com.justnothing.methodsclient.executor;

import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.io.ShellExecutionException;
import com.justnothing.testmodule.utils.io.ShellExecutorProvider;
import com.justnothing.testmodule.utils.logging.Logger;

/**
 * 权限修正工具，在系统启动阶段延迟执行 chmod 操作。
 *
 * <p>Xposed 模块在 Zygote 阶段执行时，文件系统可能尚未完全挂载，
 * 直接执行 chmod 会导致系统启动失败。此类通过检测启动阶段，
 * 在 Zygote 阶段跳过执行，在系统启动完成后同步执行。</p>
 *
 * <p>本质上就是 {@code ShellExecutorProvider.get().executeWithResult("chmod ...")} 的包装，
 * 加上启动阶段保护逻辑。</p>
 */
public class AsyncChmodExecutor {

    private static final Logger logger = Logger.getLoggerForName("AsyncChmodExecutor");

    private AsyncChmodExecutor() {}

    /**
     * 执行 chmod 操作，带启动阶段保护。
     *
     * <p>在 Zygote 阶段或系统启动完成前跳过执行，避免因文件系统未就绪导致系统崩溃。</p>
     *
     * @param targetPath  目标路径
     * @param permissions 权限字符串（八进制，如 "755"）
     * @param recursive   是否递归
     * @return true 表示执行成功或在启动阶段跳过；false 表示执行失败
     */
    public static boolean chmodFile(String targetPath, String permissions, boolean recursive) {
        if (targetPath == null || targetPath.isEmpty()) {
            logger.warn("chmodFile: 目标路径为空");
            return false;
        }

        // 启动阶段保护：Zygote 阶段或系统未启动完成时跳过执行
        if (BootMonitor.isZygotePhase()) {
            logger.warn("Zygote 阶段，跳过 chmod: " + targetPath + " (" + permissions + ")");
            return true; // 跳过但不视为失败
        }

        String chmodCmd = recursive
                ? String.format("chmod -R %s %s", permissions, targetPath)
                : String.format("chmod %s %s", permissions, targetPath);

        logger.info("执行 chmod: " + chmodCmd);

        try {
            IOManager.ProcessResult result = ShellExecutorProvider.get().execute(chmodCmd, 10000);
            if (result.isSuccess()) {
                logger.info("chmod 成功: " + targetPath);
                return true;
            } else {
                logger.warn("chmod 失败 (退出码 " + result.exitCode() + "): " + result.stderr());
                return false;
            }
        } catch (ShellExecutionException e) {
            logger.warn("chmod 执行异常: " + e.getMessage());
            return false;
        }
    }
}