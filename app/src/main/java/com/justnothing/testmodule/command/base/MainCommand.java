package com.justnothing.testmodule.command.base;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.utils.logging.Logger;

public abstract class MainCommand<Req extends CommandRequest, Res extends CommandResult> {

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

    public MainCommand(String commandName, Class<Res> type) {
        logger = new CommandLogger(commandName);
        this.responseType = type;
    }

    public abstract String getHelpText();

    public abstract Res runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception;

    public String getCommandName() {
        return logger.getTag();
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

    protected boolean shouldReturnStructuredData(CommandExecutor.CmdExecContext context) {
        return context.isGui() || context.isAgent();
    }
}
