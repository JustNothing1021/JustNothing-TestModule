package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassCommandRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassCommandResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Arrays;

public abstract class AbstractClassCommand<Req extends ClassCommandRequest<?>, Res extends ClassCommandResult>
        extends AbstractCommand<Req, Res> {

    protected static final Logger logger = Logger.getLoggerForName("AbstractClassCommand");
    protected AbstractClassCommand(String commandName, Class<Req> requestType, Class<Res> responseType) {
        super(commandName, requestType, responseType);
    }

    protected <T extends CommandResult> T createErrorResult(String message, Class<T> type) throws Exception {
        T result = type.newInstance();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    protected <T extends CommandResult> T createErrorResult(String message, Throwable t, Class<T> type) throws Exception {
        T result = type.newInstance();
        result.setSuccess(false);
        result.setError(new CommandResult.ErrorInfo("EXECUTION_FAILED", message, t.toString()));
        return result;
    }

    protected Res executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception {
        String[] subArgs;
        String[] args = context.args();
        String cmdName = context.cmdName();

        if (cmdName.equals("class")) {
            if (args != null && args.length >= 1) {
                subArgs = Arrays.copyOfRange(args, 1, args.length);
            } else {
                subArgs = new String[0];
            }
        } else {
            subArgs = args != null ? args : new String[0];
        }

        ClassCommandContext<Req> ctx = new ClassCommandContext<>
                (subArgs, context.classLoader(), context.targetPackage(), context, logger);

        return executeClassCommand(ctx);
    }

    protected abstract Res executeClassCommand(ClassCommandContext<Req> context) throws Exception;

}
