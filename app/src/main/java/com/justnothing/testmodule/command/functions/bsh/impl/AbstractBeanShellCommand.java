package com.justnothing.testmodule.command.functions.bsh.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.bsh.response.BeanShellResult;
import com.justnothing.testmodule.command.functions.bsh.util.BeanShellExecutor;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBeanShellCommand<Req extends CommandRequest>
        extends AbstractCommand<Req, BeanShellResult> {

    protected static final Logger logger = Logger.getLoggerForName("BeanShellExecutor");

    static final ConcurrentHashMap<ClassLoader, BeanShellExecutor>
            beanShellExecutors = new ConcurrentHashMap<>();
    static final BeanShellExecutor systemBeanShellExecutor = new BeanShellExecutor(null);

    static final Map<String, Object> beanShellExecutionContext = new HashMap<>();

    protected AbstractBeanShellCommand(String commandName, Class<Req> requestType) {
        super(commandName, requestType, BeanShellResult.class);
    }

    @Override
    public BeanShellResult execute(CommandExecutor.CmdExecContext<? extends CommandRequest> context) {
        try {
            return executeInternal((CommandExecutor.CmdExecContext<Req>) context);
        } catch (Exception e) {
            logger.error("执行 bsh 命令失败", e);
            CommandExceptionHandler.handleException(commandName, e, context, "执行命令失败");
            return buildErrorResult("执行命令失败: " + e.getMessage());
        }
    }

    @Override
    protected abstract BeanShellResult executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception;

    protected void out(CommandExecutor.CmdExecContext<?> context, String message) {
        out(context, message, Colors.DEFAULT);
    }

    protected void out(CommandExecutor.CmdExecContext<?> context, String message, byte color) {
        context.println(message, color);
    }

    protected BeanShellResult buildSuccessResult(String action, String output) {
        BeanShellResult result = new BeanShellResult(java.util.UUID.randomUUID().toString());
        result.setSuccess(true);
        result.setAction(action);
        result.setOutput(output);
        return result;
    }

    protected BeanShellResult buildErrorResult(String message) {
        BeanShellResult result = new BeanShellResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    protected BeanShellResult buildVariableResult(String action, ClassLoader classLoader) {
        BeanShellResult result = new BeanShellResult(java.util.UUID.randomUUID().toString());
        result.setSuccess(true);
        result.setAction(action);
        Map<String, Object> vars = getBeanShellExecutor(classLoader).getVariables();
        if (vars != null && !vars.isEmpty()) {
            List<BeanShellResult.VariableInfo> varList = new java.util.ArrayList<>();
            for (Map.Entry<String, Object> entry : vars.entrySet()) {
                varList.add(new BeanShellResult.VariableInfo(entry.getKey(), entry.getValue()));
            }
            result.setVariables(varList);
        }
        return result;
    }

    static BeanShellExecutor getBeanShellExecutor(ClassLoader cl) {
        if (cl == null) return systemBeanShellExecutor;
        return beanShellExecutors.computeIfAbsent(cl, BeanShellExecutor::new);
    }

    static File getBeanShellScriptFile(String scriptName) {
        File scriptsDir = DataBridge.getScriptsDirectory();
        return new File(scriptsDir, scriptName + ".bsh");
    }

    static boolean isValidScriptName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]+$");
    }

    static String extractScriptName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
