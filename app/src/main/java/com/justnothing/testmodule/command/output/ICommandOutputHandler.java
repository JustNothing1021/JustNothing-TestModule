package com.justnothing.testmodule.command.output;

import com.justnothing.javainterpreter.api.IOutputHandler;


/**
 * 交互式输入输出接口
 * 支持同步输出到多个目标，同时支持输入请求
 */
public interface ICommandOutputHandler extends IOutputHandler {
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
    
    default void print(String text, byte color) {
        print(text);
    }
    
    default void println(String text, byte color) {
        println(text);
    }
    
    default void printf(byte color, String format, Object... args) {
        printf(format, args);
    }
    
    default void printSuccess(String text) {
        print(text);
    }
    
    default void printlnSuccess(String text) {
        println(text);
    }
    
    default void printWarning(String text) {
        print("[WARN] " + text);
    }
    
    default void printlnWarning(String text) {
        println("[WARN] " + text);
    }
    
    default void printInfo(String text) {
        print("[INFO] " + text);
    }
    
    default void printlnInfo(String text) {
        println("[INFO] " + text);
    }
    
    default void printDebug(String text) {
        print("[DEBUG] " + text);
    }
    
    default void printlnDebug(String text) {
        println("[DEBUG] " + text);
    }
    
    default void printStackTrace(Throwable t, byte color) {
        printStackTrace(t);
    }
}