package com.justnothing.testmodule.command.utils;

import android.util.Log;
import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.utils.functions.Logger;

import java.util.Locale;
import java.util.Map;

public class CommandExceptionHandler {
    
    private static final String ERROR_SEPARATOR = "========================================";
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        Logger logger
    ) {
        return handleException(commandName, e, logger, null, null, null);
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        Logger logger,
        IOutputHandler output
    ) {
        return handleException(commandName, e, logger, output, null, null);
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        Logger logger,
        String errorHint
    ) {
        return handleException(commandName, e, logger, null, errorHint, null);
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        Logger logger,
        Map<String, Object> context
    ) {
        return handleException(commandName, e, logger, null, null, context);
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        Logger logger,
        IOutputHandler output,
        String errorHint
    ) {
        return handleException(commandName, e, logger, output, errorHint, null);
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        Logger logger,
        Map<String, Object> context,
        String errorHint
    ) {
        return handleException(commandName, e, logger, null, errorHint, context);
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        Logger logger,
        IOutputHandler output,
        String errorHint,
        Map<String, Object> context
    ) {
        logger.error("执行" + commandName + "命令失败", e);
        
        String errorMsg = formatError(commandName, e, errorHint, context);
        
        if (output != null) {
            output.println("[ERROR] " + errorMsg);
        }
        
        return errorMsg;
    }
    
    public static String handleUserError(
        String commandName, 
        String message, 
        Logger logger
    ) {
        logger.debug("用户输入错误: " + message);
        return String.format(
            Locale.getDefault(),
            "错误: %s\n" +
            "提示: 输入 '%s help' 查看帮助",
            message,
            commandName
        );
    }
    
    public static String handleUserError(
        String commandName, 
        String message, 
        Logger logger,
        String errorHint
    ) {
        logger.debug("用户输入错误: " + message);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.getDefault(), "错误: %s\n", message));
        if (errorHint != null && !errorHint.isEmpty()) {
            sb.append(String.format(Locale.getDefault(), "详情: %s\n", errorHint));
        }
        sb.append(String.format(Locale.getDefault(), "提示: 输入 '%s help' 查看帮助", commandName));
        return sb.toString();
    }
    
    // private static String formatError(String commandName, Throwable e) {
    //     return formatError(commandName, e, null, null);
    // }
    
    // private static String formatError(String commandName, Throwable e, String errorHint) {
    //     return formatError(commandName, e, errorHint, null);
    // }
    
    private static String formatError(String commandName, Throwable e, String errorHint, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append(ERROR_SEPARATOR).append("\n");
        sb.append(String.format(Locale.getDefault(), "错误: 执行%s命令时发生异常\n", commandName));
        sb.append("----------------------------------------\n");
        sb.append(String.format(Locale.getDefault(), "异常类型: %s\n", e.getClass().getSimpleName()));
        sb.append(String.format(Locale.getDefault(), "错误信息: %s\n", e.getMessage()));
        
        if (errorHint != null && !errorHint.isEmpty()) {
            sb.append("----------------------------------------\n");
            sb.append(String.format(Locale.getDefault(), "错误详情: %s\n", errorHint));
        }
        
        if (context != null && !context.isEmpty()) {
            sb.append("----------------------------------------\n");
            sb.append("上下文信息:\n");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                sb.append(String.format(Locale.getDefault(), "  %s: %s\n", entry.getKey(), entry.getValue()));
            }
        }
        
        sb.append("----------------------------------------\n");
        sb.append("堆栈追踪:\n").append(Log.getStackTraceString(e)).append("\n");
        sb.append(ERROR_SEPARATOR);
        return sb.toString();
    }
}
