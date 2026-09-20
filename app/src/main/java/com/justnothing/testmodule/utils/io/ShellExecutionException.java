package com.justnothing.testmodule.utils.io;

/**
 * Shell 命令执行异常
 */
public class ShellExecutionException extends Exception {

    private final String command;
    private final ShellType executorType;

    public ShellExecutionException(String message) {
        super(message);
        this.command = null;
        this.executorType = null;
    }

    public ShellExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.command = null;
        this.executorType = null;
    }

    public ShellExecutionException(String command, ShellType executorType, String message) {
        super("[" + executorType + "] 执行命令失败: " + command + " - " + message);
        this.command = command;
        this.executorType = executorType;
    }

    public ShellExecutionException(String command, ShellType executorType, String message, Throwable cause) {
        super("[" + executorType + "] 执行命令失败: " + command + " - " + message, cause);
        this.command = command;
        this.executorType = executorType;
    }

    public String getCommand() {
        return command;
    }

    public ShellType getExecutorType() {
        return executorType;
    }
}
