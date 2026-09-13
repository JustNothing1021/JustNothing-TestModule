package com.justnothing.testmodule.command.framework.utils;

import android.util.Log;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.output.Colors;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandExceptionHandler {
    
    private static final String ERROR_SEPARATOR = "========================================";
    

    public static String handleException(
        String commandName, 
        Throwable e, 
        CommandExecutor.CmdExecContext<?> ctx
    ) {
        return printColoredError(ctx, commandName, e, null, null);
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        CommandExecutor.CmdExecContext<?> ctx,
        String errorHint
    ) {
        return printColoredError(ctx, commandName, e, errorHint, null);
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        CommandExecutor.CmdExecContext<?> ctx,
        Map<String, Object> context
    ) {
        return printColoredError(ctx, commandName, e, null, context);
    }



    public static String handleException(
        String commandName, 
        Throwable e, 
        CommandExecutor.CmdExecContext<?> ctx,
        String errorHint,
        Map<?, ?> context
    ) {
        Map<String, Object> convertedContext = context == null ? null : 
            context.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()),
                    entry -> String.valueOf(entry.getValue()),
                    (a, b) -> a,
                    LinkedHashMap::new
                ));
        return printColoredError(ctx, commandName, e, errorHint, convertedContext);
    }
    
    public static String handleException(
        String commandName, 
        Throwable e, 
        CommandExecutor.CmdExecContext<?> ctx,
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
        return printColoredError(ctx, commandName, e, errorHint, convertedContext);
    }
    
    /**
     * 打印彩色错误信息，并返回与打印内容一致的纯文本错误串（非 null）。
     */
    private static String printColoredError(
        CommandExecutor.CmdExecContext<?> ctx,
        String commandName, 
        Throwable e, 
        String errorHint, 
        Map<String, Object> context
    ) {
        StringBuilder errorText = new StringBuilder();

        ctx.println(ERROR_SEPARATOR, Colors.RED);
        errorText.append(ERROR_SEPARATOR).append("\n");

        ctx.print("错误: 执行", Colors.RED);
        ctx.print(commandName, Colors.YELLOW);
        ctx.println("命令时发生异常", Colors.RED);
        errorText.append("错误: 执行").append(commandName).append("命令时发生异常\n");

        ctx.println("----------------------------------------", Colors.RED);
        errorText.append("----------------------------------------\n");
        
        ctx.print("异常类型: ", Colors.CYAN);
        ctx.println(e.getClass().getSimpleName(), Colors.YELLOW);
        errorText.append("异常类型: ").append(e.getClass().getSimpleName()).append("\n");

        String errorMessage = e.getMessage() != null ? e.getMessage() : "无详细信息";
        ctx.print("错误信息: ", Colors.CYAN);
        ctx.println(errorMessage, Colors.RED);
        errorText.append("错误信息: ").append(errorMessage).append("\n");
        
        if (errorHint != null && !errorHint.isEmpty()) {
            ctx.println("----------------------------------------", Colors.RED);
            ctx.print("错误详情: ", Colors.CYAN);
            ctx.println(errorHint, Colors.ORANGE);
            errorText.append("----------------------------------------\n");
            errorText.append("错误详情: ").append(errorHint).append("\n");
        }
        
        if (context != null && !context.isEmpty()) {
            ctx.println("----------------------------------------", Colors.RED);
            ctx.println("上下文信息:", Colors.CYAN);
            errorText.append("----------------------------------------\n");
            errorText.append("上下文信息:\n");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                ctx.print("  " + entry.getKey() + ": ", Colors.CYAN);
                ctx.println(String.valueOf(entry.getValue()), Colors.LIGHT_GREEN);
                errorText.append("  ").append(entry.getKey()).append(": ")
                        .append(entry.getValue()).append("\n");
            }
        }
        
        ctx.println("----------------------------------------", Colors.RED);
        ctx.println("堆栈追踪:", Colors.CYAN);
        errorText.append("----------------------------------------\n");
        errorText.append("堆栈追踪:\n");

        String stackTrace = Log.getStackTraceString(e);
        for (String line : stackTrace.split("\n")) {
            if (line.startsWith("\t")) {
                ctx.print("  ", Colors.GRAY);
                ctx.println(line.trim(), Colors.GRAY);
                errorText.append("  ").append(line.trim()).append("\n");
            } else {
                ctx.println(line, Colors.GRAY);
                errorText.append(line).append("\n");
            }
        }
        ctx.println(ERROR_SEPARATOR, Colors.RED);
        errorText.append(ERROR_SEPARATOR);

        return errorText.toString();
    }
}
