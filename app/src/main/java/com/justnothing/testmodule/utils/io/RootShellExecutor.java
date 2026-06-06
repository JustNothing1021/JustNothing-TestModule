package com.justnothing.testmodule.utils.io;

import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 基于 RootProcessPool 的 Root Shell 执行器。
 *
 * <p>通过 {@code su} 命令维持进程池，复用 root shell 进程以减少
 * 进程创建开销。所有命令通过已有的 su 会话执行。</p>
 */
public class RootShellExecutor implements ShellExecutor {

    private static final String TAG = "RootShellExecutor";
    private static final Logger logger = Logger.getLoggerForName(TAG);
    private static final long DEFAULT_TIMEOUT_MS = 30000;

    @Override
    public IOManager.ProcessResult execute(String command) throws ShellExecutionException {
        return execute(command, DEFAULT_TIMEOUT_MS);
    }

    @Override
    public IOManager.ProcessResult execute(String command, long timeoutMs) throws ShellExecutionException {
        if (BootMonitor.isZygotePhase()) {
            throw new ShellExecutionException(command, ShellType.ROOT,
                    "Zygote 阶段无法执行 root 命令");
        }

        try {
            return RootProcessPool.executeCommand(command, timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShellExecutionException(command, ShellType.ROOT,
                    "命令执行被中断 (" + timeoutMs + "ms 超时)", e);
        } catch (IOException e) {
            throw new ShellExecutionException(command, ShellType.ROOT,
                    "IO 异常: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<IOManager.ProcessResult> executeAsync(String command) {
        return executeAsync(command, DEFAULT_TIMEOUT_MS);
    }

    @Override
    public CompletableFuture<IOManager.ProcessResult> executeAsync(String command, long timeoutMs) {
        if (BootMonitor.isZygotePhase()) {
            CompletableFuture<IOManager.ProcessResult> failed = new CompletableFuture<>();
            failed.completeExceptionally(new ShellExecutionException(command, ShellType.ROOT,
                    "Zygote 阶段无法执行 root 命令"));
            return failed;
        }
        return RootProcessPool.executeCommandAsync(command, timeoutMs);
    }

    @Override
    public boolean isAvailable() {
        if (BootMonitor.isZygotePhase()) {
            return false;
        }
        RootProcessPool pool = RootProcessPool.getInstance();
        return pool != null && pool.getStats().contains("total=");
    }

    @Override
    public ShellType getType() {
        return ShellType.ROOT;
    }

    @Override
    public String getDescription() {
        RootProcessPool pool = RootProcessPool.getInstance();
        return pool != null ? pool.getStats() : "RootShellExecutor[未初始化]";
    }
}
