package com.justnothing.testmodule.utils.io;

import java.util.concurrent.CompletableFuture;

/**
 * 统一 Shell 命令执行接口。
 *
 * <p>所有需要执行系统命令的地方都应该通过此接口，而不是直接使用
 * {@code Runtime.exec()} 或 {@code ProcessBuilder}。这样做的优势：</p>
 * <ul>
 *   <li>统一入口，便于日志、监控、统计</li>
 *   <li>支持多种后端（Root / Local / Mock / ADB）</li>
 *   <li>测试时可替换为 Mock 实现</li>
 *   <li>自动处理超时、进程池复用等细节</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <pre>
 *   ShellExecutor shell = ShellExecutorProvider.get();
 *   ProcessResult result = shell.execute("pm list packages");
 * </pre>
 */
public interface ShellExecutor {

    /**
     * 执行命令（使用默认超时）
     */
    IOManager.ProcessResult execute(String command) throws ShellExecutionException;

    /**
     * 执行命令（指定超时毫秒）
     */
    IOManager.ProcessResult execute(String command, long timeoutMs) throws ShellExecutionException;

    /**
     * 异步执行命令
     */
    CompletableFuture<IOManager.ProcessResult> executeAsync(String command);

    /**
     * 异步执行命令（指定超时）
     */
    CompletableFuture<IOManager.ProcessResult> executeAsync(String command, long timeoutMs);

    /**
     * 当前执行器是否可用
     */
    boolean isAvailable();

    /**
     * 获取执行器类型标识
     *
     * @return 执行器类型枚举（ROOT / LOCAL / MOCK / ADB）
     */
    ShellType getType();

    /**
     * 获取执行器描述信息（用于调试/状态展示）
     */
    String getDescription();
}
