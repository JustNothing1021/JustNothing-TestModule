package com.justnothing.testmodule.command.output;

/**
 * 交互式输入输出接口
 * 支持同步输出到多个目标，同时支持输入请求
 */
public interface IOutputHandler {
    // 原有的输出方法保持不变
    void println(String line);
    void print(String text);
    void printf(String format, Object... args);
    void printStackTrace(Throwable t);
    void flush();
    void close();
    boolean isClosed();
    void clear();
    String getString();

    // 新增的输入相关方法
    String readLineFromClient(String prompt);
    String readPasswordFromClient(String prompt);
    boolean isInteractive();
}