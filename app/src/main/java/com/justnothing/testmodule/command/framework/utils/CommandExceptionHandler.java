package com.justnothing.testmodule.command.framework.utils;

import android.util.Log;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
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

        String headPrefix = CliMessages.ERR_HEAD_PREFIX.text();
        String headSuffix = CliMessages.ERR_HEAD_SUFFIX.text();
        ctx.print(headPrefix, Colors.RED);
        ctx.print(commandName, Colors.YELLOW);
        ctx.println(headSuffix, Colors.RED);
        errorText.append(headPrefix).append(commandName).append(headSuffix).append("\n");

        ctx.println("----------------------------------------", Colors.RED);
        errorText.append("----------------------------------------\n");

        String exceptionType = CliMessages.ERR_EXCEPTION_TYPE.text();
        ctx.print(exceptionType, Colors.CYAN);
        ctx.println(e.getClass().getSimpleName(), Colors.YELLOW);
        errorText.append(exceptionType).append(e.getClass().getSimpleName()).append("\n");

        String errorMessage = e.getMessage() != null ? e.getMessage() : CliMessages.ERR_NO_MESSAGE.text();
        String messageLabel = CliMessages.ERR_MESSAGE.text();
        ctx.print(messageLabel, Colors.CYAN);
        ctx.println(errorMessage, Colors.RED);
        errorText.append(messageLabel).append(errorMessage).append("\n");

        if (errorHint != null && !errorHint.isEmpty()) {
            ctx.println("----------------------------------------", Colors.RED);
            String detailsLabel = CliMessages.ERR_DETAILS.text();
            ctx.print(detailsLabel, Colors.CYAN);
            ctx.println(errorHint, Colors.ORANGE);
            errorText.append("----------------------------------------\n");
            errorText.append(detailsLabel).append(errorHint).append("\n");
        }

        if (context != null && !context.isEmpty()) {
            ctx.println("----------------------------------------", Colors.RED);
            String contextLabel = CliMessages.ERR_CONTEXT.text();
            ctx.println(contextLabel, Colors.CYAN);
            errorText.append("----------------------------------------\n");
            errorText.append(contextLabel).append("\n");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                ctx.print("  " + entry.getKey() + ": ", Colors.CYAN);
                ctx.println(String.valueOf(entry.getValue()), Colors.LIGHT_GREEN);
                errorText.append("  ").append(entry.getKey()).append(": ")
                        .append(entry.getValue()).append("\n");
            }
        }

        String stackTraceLabel = CliMessages.ERR_STACK_TRACE.text();
        ctx.println("----------------------------------------", Colors.RED);
        ctx.println(stackTraceLabel, Colors.CYAN);
        errorText.append("----------------------------------------\n");
        errorText.append(stackTraceLabel).append("\n");

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
