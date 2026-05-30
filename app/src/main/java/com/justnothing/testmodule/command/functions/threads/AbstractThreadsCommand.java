package com.justnothing.testmodule.command.functions.threads;

import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.threads.response.ThreadCommandResult;
import com.justnothing.testmodule.utils.logging.Logger;

public abstract class AbstractThreadsCommand<Req extends CommandRequest, Res extends ThreadCommandResult>
        extends AbstractCommand<Req, Res> {

    protected static final Logger logger = Logger.getLoggerForName("AbstractThreadsCommand");

    protected AbstractThreadsCommand(String commandName, Class<Req> requestType, Class<Res> responseType) {
        super(commandName, requestType, responseType);
    }

    protected Res createErrorResult(String message) {
        try {
            Res result = returnType.newInstance();
            result.setSuccess(false);
            result.setMessage(message);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected Res executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception {
        Req request = context.getCommandRequest();
        if (request == null) {
            throw new IllegalStateException("Request不能为null");
        }
        
        try {
            return executeThreadsCommand(context);
        } catch (Exception e) {
            logger.error("执行threads命令失败: " + commandName, e);
            throw e;
        }
    }

    protected abstract Res executeThreadsCommand(CommandExecutor.CmdExecContext<Req> context) throws Exception;
}
