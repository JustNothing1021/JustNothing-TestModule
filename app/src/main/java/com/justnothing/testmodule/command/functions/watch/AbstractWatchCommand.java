package com.justnothing.testmodule.command.functions.watch;

import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.watch.response.WatchCommandResult;
import com.justnothing.testmodule.utils.logging.Logger;

public abstract class AbstractWatchCommand<Req extends CommandRequest, Res extends WatchCommandResult>
        extends AbstractCommand<Req, Res> {

    protected static final Logger logger = Logger.getLoggerForName("AbstractWatchCommand");

    protected AbstractWatchCommand(String commandName, Class<Req> requestType, Class<Res> responseType) {
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
            return executeWatchCommand(context);
        } catch (Exception e) {
            logger.error("执行watch命令失败: " + commandName, e);
            throw e;
        }
    }

    protected abstract Res executeWatchCommand(CommandExecutor.CmdExecContext<Req> context) throws Exception;
}
