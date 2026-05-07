package com.justnothing.testmodule.command.base;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.CommandInfo;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.command.SubCommands;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.classcmd.SubClassCommandRequest;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("rawtypes")
public abstract class MainCommand<Res extends CommandResult> {

    public static class CommandLogger extends Logger {
        private final String tag;

        public CommandLogger(String tag) {
            this.tag = tag;
        }

        @Override
        public String getTag() {
            return tag;
        }
    }

    protected final Class<Res> responseType;

    protected CommandLogger logger;

    private final Map<Class<? extends AbstractCommand>, AbstractCommand> commandInstanceCache = new ConcurrentHashMap<>();

    public MainCommand(String commandName, Class<Res> type) {
        logger = new CommandLogger(commandName);
        this.responseType = type;
    }

    public String getHelpText() {
        CommandInfo info = this.getClass().getAnnotation(CommandInfo.class);
        if (info != null && !info.helpText().isEmpty()) {
            String helpText = info.helpText();
            if (!info.version().isEmpty() && helpText.contains("%")) {
                return String.format(Locale.getDefault(), helpText, info.version());
            }
            return helpText;
        }

        return "用法: " + getCommandName() + " [args...]\n" +
               "输入 " + getCommandName() + " --help 查看详细帮助";
    }

    public abstract Res runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception;

    public String getCommandName() {
        return logger.getTag();
    }

    protected AbstractCommand<?, ?> resolveSubCommandFromRequest(CommandRequest request) {
        SubCommands subCommandsAnnotation = this.getClass().getAnnotation(SubCommands.class);
        if (subCommandsAnnotation != null) {
            for (SubCommand subCmd : subCommandsAnnotation.value()) {
                if (subCmd.request() != CommandRequest.class && subCmd.request().isInstance(request)) {
                    if (!subCmd.command().getName().equals(AbstractCommand.class.getName())) {
                        return getOrCreateCommand(subCmd.command());
                    }
                    break;
                }
            }
        }

        SubClassCommandRequest legacyAnnotation = request.getClass().getAnnotation(SubClassCommandRequest.class);
        if (legacyAnnotation != null) {
            String subCmdName = legacyAnnotation.value();
            return findAndCreateCommandByName(subCmdName);
        }

        String commandType = request.getCommandType();
        logger.warn("Request类 " + request.getClass().getSimpleName() +
                " 缺少@SubCommand.command注解，尝试从commandType推导: " + commandType);

        return findAndCreateCommandByName(commandType.toLowerCase());
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractCommand> T getOrCreateCommand(Class<T> commandClass) {
        return (T) commandInstanceCache.computeIfAbsent(commandClass, clazz -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create command instance: " + clazz.getSimpleName(), e);
            }
        });
    }

    private AbstractCommand<?, ?> findAndCreateCommandByName(String name) {
        SubCommands subCommandsAnnotation = this.getClass().getAnnotation(SubCommands.class);
        if (subCommandsAnnotation != null) {
            for (SubCommand subCmd : subCommandsAnnotation.value()) {
                if (subCmd.value().equals(name) && !subCmd.command().getName().equals(AbstractCommand.class.getName())) {
                    return getOrCreateCommand(subCmd.command());
                }
            }
        }
        return null;
    }

    protected Res createErrorResult(String message) throws Exception {
        Res result = responseType.newInstance();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    protected Res createErrorResult(String message, Throwable t) throws Exception {
        Res result = responseType.newInstance();
        result.setSuccess(false);
        result.setError(new CommandResult.ErrorInfo("EXECUTION_FAILED", message, t.toString()));
        return result;
    }

    protected Res createSuccessResult(String message) throws Exception {
        Res result = responseType.newInstance();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    protected boolean shouldReturnStructuredData(CommandExecutor.CmdExecContext<?> context) {
        return context.isGui() || context.isAgent();
    }
}
