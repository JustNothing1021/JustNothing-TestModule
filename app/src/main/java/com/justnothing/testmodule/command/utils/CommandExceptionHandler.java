package com.justnothing.testmodule.command.utils;

import android.util.Log;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.output.ICommandOutputHandler;
import com.justnothing.testmodule.utils.functions.Logger;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandExceptionHandler {
    
    private static final String ERROR_SEPARATOR = "========================================";
    

    public static String handleException(
        String commandName, 
        Throwable e, 
        CommandExecutor.CmdExecContext ctx
    ) {
        printColoredError(ctx, commandName, e, null, null);
        return null;
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        CommandExecutor.CmdExecContext ctx,
        String errorHint
    ) {
        printColoredError(ctx, commandName, e, errorHint, null);
        return null;
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        CommandExecutor.CmdExecContext ctx,
        Map<String, Object> context
    ) {
        printColoredError(ctx, commandName, e, null, context);
        return null;
    }



    public static String handleException(
        String commandName, 
        Throwable e, 
        CommandExecutor.CmdExecContext ctx,
        String errorHint,
        Map<?, ?> context
    ) {
        Map<String, Object> convertedContext = context == null ? null : 
            context.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()),
                    entry -> String.valueOf(entry.getValue()),
                    (a, b) -> a,
                    java.util.LinkedHashMap::new
                ));
        printColoredError(ctx, commandName, e, errorHint, convertedContext);
        return null;
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        CommandExecutor.CmdExecContext ctx,
        Map<?, ?> context,
        String errorHint
    ) {
        Map<String, Object> convertedContext = context == null ? null : 
            context.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()),
                    entry -> String.valueOf(entry.getValue()),
                    (a, b) -> a,
                    LinkedHashMap::new
                ));
        printColoredError(ctx, commandName, e, errorHint, convertedContext);
        return null;
    }
    
    private static void printColoredError(
        CommandExecutor.CmdExecContext ctx,
        String commandName, 
        Throwable e, 
        String errorHint, 
        Map<String, Object> context
    ) {
        ctx.println(ERROR_SEPARATOR, Colors.RED);
        ctx.print("错误: 执行", Colors.RED);
        ctx.print(commandName, Colors.YELLOW);
        ctx.println("命令时发生异常", Colors.RED);
        ctx.println("----------------------------------------", Colors.RED);
        
        ctx.print("异常类型: ", Colors.CYAN);
        ctx.println(e.getClass().getSimpleName(), Colors.YELLOW);
        ctx.print("错误信息: ", Colors.CYAN);
        ctx.println(e.getMessage() != null ? e.getMessage() : "无详细信息", Colors.RED);
        
        if (errorHint != null && !errorHint.isEmpty()) {
            ctx.println("----------------------------------------", Colors.RED);
            ctx.print("错误详情: ", Colors.CYAN);
            ctx.println(errorHint, Colors.ORANGE);
        }
        
        if (context != null && !context.isEmpty()) {
            ctx.println("----------------------------------------", Colors.RED);
            ctx.println("上下文信息:", Colors.CYAN);
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                ctx.print("  " + entry.getKey() + ": ", Colors.CYAN);
                ctx.println(String.valueOf(entry.getValue()), Colors.LIGHT_GREEN);
            }
        }
        
        ctx.println("----------------------------------------", Colors.RED);
        ctx.println("堆栈追踪:", Colors.CYAN);
        String stackTrace = Log.getStackTraceString(e);
        for (String line : stackTrace.split("\n")) {
            if (line.startsWith("\t")) {
                ctx.print("  ", Colors.GRAY);
                ctx.println(line.trim(), Colors.GRAY);
            } else {
                ctx.println(line, Colors.GRAY);
            }
        }
        ctx.println(ERROR_SEPARATOR, Colors.RED);
    }

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
