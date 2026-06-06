package com.justnothing.testmodule.utils.io;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock Shell 执行器，用于单元测试。
 *
 * <p>不执行任何真实命令，返回预设结果。支持两种模式：</p>
 * <ul>
 *   <li><b>预设模式</b>：通过 {@link #mockCommand(String, ProcessResult)} 注册预设</li>
 *   <li><b>默认模式</b>：未注册的命令返回成功（exitCode=0）或可配置的默认响应</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 *   MockShellExecutor mock = new MockShellExecutor();
 *   mock.mockCommand("pm list packages",
 *       new ProcessResult(0, "package:com.example.app\n", ""));
 *   mock.execute("pm list packages"); // 返回预设结果
 * </pre>
 */
public class MockShellExecutor implements ShellExecutor {

    private final Map<String, IOManager.ProcessResult> mockedCommands = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> executionCount = new ConcurrentHashMap<>();
    private IOManager.ProcessResult defaultResult = new IOManager.ProcessResult(0, "", "");
    private boolean defaultSuccess = true;

    /**
     * 注册预设的命令响应
     */
    public void mockCommand(String command, IOManager.ProcessResult result) {
        mockedCommands.put(command, result);
    }

    /**
     * 注册预设的命令响应（模糊匹配：只要包含此字符串就返回）
     */
    public void mockCommandContains(String commandPart, IOManager.ProcessResult result) {
        mockedCommands.put("__contains__:" + commandPart, result);
    }

    /**
     * 设置未匹配命令的默认响应
     */
    public void setDefaultResult(IOManager.ProcessResult result) {
        this.defaultResult = result;
    }

    /**
     * 设置未匹配命令是否默认返回成功
     */
    public void setDefaultSuccess(boolean success) {
        this.defaultSuccess = success;
        this.defaultResult = success
                ? new IOManager.ProcessResult(0, "", "")
                : new IOManager.ProcessResult(1, "", "mock: no matching rule");
    }

    /**
     * 获取某条命令的被执行次数
     */
    public int getExecutionCount(String command) {
        return executionCount.getOrDefault(command, 0);
    }

    /**
     * 获取总执行次数
     */
    public int getTotalExecutionCount() {
        return executionCount.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 清除所有预设和统计
     */
    public void reset() {
        mockedCommands.clear();
        executionCount.clear();
    }

    @Override
    public IOManager.ProcessResult execute(String command) throws ShellExecutionException {
        return execute(command, 5000);
    }

    @Override
    public IOManager.ProcessResult execute(String command, long timeoutMs) throws ShellExecutionException {
        // 记录执行次数
        executionCount.merge(command, 1, Integer::sum);

        // 精确匹配
        IOManager.ProcessResult exact = mockedCommands.get(command);
        if (exact != null) {
            return exact;
        }

        // 模糊匹配
        for (Map.Entry<String, IOManager.ProcessResult> entry : mockedCommands.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("__contains__:") && command.contains(key.substring("__contains__:".length()))) {
                return entry.getValue();
            }
        }

        return defaultResult;
    }

    @Override
    public CompletableFuture<IOManager.ProcessResult> executeAsync(String command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(command);
            } catch (ShellExecutionException e) {
                throw new RuntimeException(e);
            }
        });
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
        return true;
    }

    @Override
    public ShellType getType() {
        return ShellType.MOCK;
    }

    @Override
    public String getDescription() {
        return "MockShellExecutor[预设=" + mockedCommands.size()
                + ", 总执行=" + getTotalExecutionCount() + "]";
    }
}
