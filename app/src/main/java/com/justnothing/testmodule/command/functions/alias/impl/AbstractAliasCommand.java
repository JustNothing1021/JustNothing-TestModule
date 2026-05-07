package com.justnothing.testmodule.command.functions.alias.impl;

import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.alias.request.AliasRequest;

public abstract class AbstractAliasCommand<Req extends AliasRequest, Res extends CommandResult>
        extends AbstractCommand<Req, Res> {

    protected AbstractAliasCommand(String commandName, Class<Req> requestType, Class<Res> responseType) {
        super(commandName, requestType, responseType);
    }

    protected <T extends CommandResult> T createErrorResult(String message, Class<T> type) throws Exception {
        T result = type.newInstance();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}
