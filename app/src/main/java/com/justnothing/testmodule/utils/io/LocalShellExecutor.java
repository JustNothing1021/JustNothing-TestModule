package com.justnothing.testmodule.utils.io;

import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 非 Root 的本地 Shell 执行器。
 *
 * <p>在 Android 上使用 {@code /system/bin/sh} 执行命令，无需 root 权限。
 * 在桌面系统（Windows/Linux/macOS）上使用系统默认 shell。
 * 适用于不需要权限提升的场景（如读取系统属性、基本文件操作等）。</p>
 */
public class LocalShellExecutor implements ShellExecutor {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    private static final String TAG = "LocalShellExecutor";
    private static final Logger logger = Logger.getLoggerForName(TAG);
    private static final long DEFAULT_TIMEOUT_MS = 10000;

    @Override
    public IOManager.ProcessResult execute(String command) throws ShellExecutionException {
        return execute(command, DEFAULT_TIMEOUT_MS);
    }

    @Override
    public IOManager.ProcessResult execute(String command, long timeoutMs) throws ShellExecutionException {
        if (BootMonitor.isZygotePhase()) {
            throw new ShellExecutionException(command, ShellType.LOCAL,
                    "Zygote 阶段无法执行 shell 命令");
        }

        long startTime = System.currentTimeMillis();
        Process process = null;
        try {
            String[] cmdArray = IS_WINDOWS
                    ? new String[]{"cmd", "/c", command}
                    : new String[]{"/system/bin/sh", "-c", command};
            process = new ProcessBuilder(cmdArray)
                    .redirectErrorStream(true)
                    .start();

            // 读取 stdout（已合并 stderr）
            StringBuilder stdout = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (stdout.length() > 0) stdout.append('\n');
                    stdout.append(line);
                }
            }

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ShellExecutionException(command, ShellType.LOCAL,
                        "命令执行超时 (" + timeoutMs + "ms)");
            }

            int exitCode = process.exitValue();
            long duration = System.currentTimeMillis() - startTime;

            logger.debug("[local] 命令执行完成: exitCode=" + exitCode
                    + ", 耗时=" + duration + "ms");

            return new IOManager.ProcessResult(exitCode, stdout.toString(), "", duration);

        } catch (IOException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            throw new ShellExecutionException(command, ShellType.LOCAL,
                    "IO 异常: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            throw new ShellExecutionException(command, ShellType.LOCAL,
                    "命令执行被中断", e);
        }
    }

    @Override
    public CompletableFuture<IOManager.ProcessResult> executeAsync(String command) {
        return executeAsync(command, DEFAULT_TIMEOUT_MS);
    }

    @Override
    public CompletableFuture<IOManager.ProcessResult> executeAsync(String command, long timeoutMs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(command, timeoutMs);
            } catch (ShellExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        // 桌面环境始终可用；Android 上需跳过 Zygote 阶段
        return IS_WINDOWS || !BootMonitor.isZygotePhase();
    }

    @Override
    public ShellType getType() {
        return ShellType.LOCAL;
    }

    @Override
    public String getDescription() {
        String shell = IS_WINDOWS ? "cmd" : "/system/bin/sh";
        return "LocalShellExecutor[" + shell + ", 无需 root]";
    }
}
