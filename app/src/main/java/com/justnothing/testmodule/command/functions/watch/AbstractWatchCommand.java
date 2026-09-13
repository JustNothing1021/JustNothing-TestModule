package com.justnothing.testmodule.command.functions.watch;

import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.functions.watch.response.WatchCommandResult;
import com.justnothing.testmodule.utils.logging.Logger;

public abstract class AbstractWatchCommand<Req extends CommandRequest<?>, Res extends WatchCommandResult>
        extends AbstractCommand<Req, Res> {

    protected static final Logger logger = Logger.getLoggerForName("AbstractWatchCommand");

    protected AbstractWatchCommand(String commandName, Class<Req> requestType, Class<Res> responseType) {
        super(commandName, requestType, responseType);
    }

    // createErrorResult 由 AbstractCommand 统一提供（原先这里有一份会在失败时返回 null 的副本）

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
