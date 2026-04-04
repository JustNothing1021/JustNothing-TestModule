package com.justnothing.testmodule.command.output;

/**
 * 交互式输入输出接口
 * 支持同步输出到多个目标，同时支持输入请求
 */
public interface IOutputHandler extends com.justnothing.javainterpreter.api.IOutputHandler {
    default String readLineFromClient(String prompt) {
        throw new RuntimeException(getClass().getName() + " 并不支持readLineFromClient...");
    }
    default String readPasswordFromClient(String prompt) {
        throw new RuntimeException(getClass().getName() + " 并不支持readPasswordFromClient...");
    }
    
    @Override
    default String readLine(String prompt) {
        return readLineFromClient(prompt);
    }
    @Override
    default String readPassword(String prompt) {
        return readPasswordFromClient(prompt);
    }
}