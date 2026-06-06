package com.justnothing.testmodule.utils.io;

import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Shell 执行器提供者，自动选择合适的 Shell 实现。
 *
 * <p>选择优先级：</p>
 * <ol>
 *   <li>如果通过 {@link #setForcedExecutor(ShellExecutor)} 强制指定了执行器 → 使用指定的</li>
 *   <li>如果 RootProcessPool 可用（有 root 权限）→ 使用 {@link RootShellExecutor}</li>
 *   <li>否则降级为 {@link LocalShellExecutor}</li>
 * </ol>
 *
 * <p>使用方式：</p>
 * <pre>
 *   // 获取当前最佳执行器
 *   ShellExecutor shell = ShellExecutorProvider.get();
 *   ProcessResult result = shell.execute("pm list packages");
 *
 *   // 测试时强制使用 Mock
 *   MockShellExecutor mock = new MockShellExecutor();
 *   mock.mockCommand("pm list packages", new ProcessResult(0, "package:com.test", ""));
 *   ShellExecutorProvider.setForcedExecutor(mock);
 * </pre>
 */
public class ShellExecutorProvider {

    private static final String TAG = "ShellExecutorProvider";
    private static final Logger logger = Logger.getLoggerForName(TAG);

    private static final AtomicReference<ShellExecutor> forcedExecutor = new AtomicReference<>();
    private static volatile ShellExecutor cachedExecutor = null;
    private static volatile long lastResolveTime = 0;
    private static final long CACHE_TTL_MS = 30000; // 30 秒缓存

    private ShellExecutorProvider() {
        // 工具类
    }

    /**
     * 获取当前的 Shell 执行器（带缓存）
     */
    public static ShellExecutor get() {
        // 强制指定的始终优先
        ShellExecutor forced = forcedExecutor.get();
        if (forced != null) {
            return forced;
        }

        // 缓存未过期则直接返回
        long now = System.currentTimeMillis();
        if (cachedExecutor != null && (now - lastResolveTime) < CACHE_TTL_MS) {
            return cachedExecutor;
        }

        return resolve();
    }

    /**
     * 强制指定执行器（通常用于测试）
     *
     * @param executor 要使用的执行器，传 null 取消强制指定
     */
    public static void setForcedExecutor(ShellExecutor executor) {
        forcedExecutor.set(executor);
        if (executor == null) {
            logger.info("已取消强制 Shell 执行器");
        } else {
            logger.info("强制指定 Shell 执行器: " + executor.getType()
                    + " (" + executor.getDescription() + ")");
        }
        // 清缓存，下次 get() 会重新解析
        cachedExecutor = null;
    }

    /**
     * 清除缓存，强制下次重新选择执行器
     */
    public static void invalidateCache() {
        cachedExecutor = null;
    }

    /**
     * 重新解析并返回最佳执行器
     */
    private static synchronized ShellExecutor resolve() {
        // 双重检查
        ShellExecutor forced = forcedExecutor.get();
        if (forced != null) {
            return forced;
        }
        if (cachedExecutor != null && (System.currentTimeMillis() - lastResolveTime) < CACHE_TTL_MS) {
            return cachedExecutor;
        }

        ShellExecutor selected = doResolve();
        cachedExecutor = selected;
        lastResolveTime = System.currentTimeMillis();

        logger.info("Shell 执行器已选定: " + selected.getType()
                + " (" + selected.getDescription() + ")");
        return selected;
    }

    private static ShellExecutor doResolve() {
        // Zygote 阶段只能用 Local（甚至 Local 都不行）
        if (BootMonitor.isZygotePhase()) {
            logger.debug("Zygote 阶段，使用 LocalShellExecutor（功能受限）");
            return new LocalShellExecutor();
        }

        // 尝试 Root
        try {
            RootProcessPool pool = RootProcessPool.getInstance();
            if (pool != null && pool.getStats().contains("total=")) {
                // 快速检测 root 是否真的可用
                IOManager.ProcessResult test = RootProcessPool.executeCommand("echo 'root_check'", 3000);
                if (test.isSuccess()) {
                    logger.debug("Root 可用，使用 RootShellExecutor");
                    return new RootShellExecutor();
                }
                logger.warn("Root 进程池存在但命令执行失败，降级到 LocalShellExecutor");
            }
        } catch (Exception e) {
            logger.debug("Root 不可用: " + e.getMessage());
        }

        // 降级到 Local
        logger.info("Root 不可用，降级到 LocalShellExecutor");
        return new LocalShellExecutor();
    }

    /**
     * 获取当前执行器的统计信息
     */
    public static String getStatus() {
        ShellExecutor current = get();
        return String.format("ShellExecutor[type=%s, description=%s, available=%s]",
                current.getType(),
                current.getDescription(),
                current.isAvailable());
    }
}
