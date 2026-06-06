package com.justnothing.testmodule.command.functions.breakpoint.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.functions.breakpoint.util.BreakpointManager;
import com.justnothing.testmodule.command.functions.breakpoint.response.BreakpointResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.logging.Logger;

public abstract class AbstractBreakpointCommand<REQUEST extends CommandRequest, RESULT extends CommandResult>
        extends AbstractCommand<REQUEST, RESULT> {

    protected static final Logger logger = Logger.getLoggerForName("BreakpointCmd");

    protected final BreakpointManager manager = BreakpointManager.getInstance();
    protected CommandExecutor.CmdExecContext<?> context;

    protected AbstractBreakpointCommand(String commandName, Class<REQUEST> requestType, Class<RESULT> resultType) {
        super(commandName, requestType, resultType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RESULT execute(CommandExecutor.CmdExecContext<? extends CommandRequest> context) {
        this.context = context;
        try {
            return executeInternal((CommandExecutor.CmdExecContext<REQUEST>) context);
        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "breakpoint", e, context, "执行断点命令失败");
            return (RESULT) createErrorResult(e.getMessage());
        }
    }

    protected abstract RESULT executeInternal(CommandExecutor.CmdExecContext<REQUEST> request) throws Exception;

    protected void out(String text) { context.println(text, Colors.WHITE); }
    protected void out(String text, byte color) { context.println(text, color); }

    protected BreakpointResult createSuccessResult(String msg) {
        BreakpointResult r = new BreakpointResult();
        r.setSuccess(true);
        r.setMessage(msg);
        return r;
    }

    protected BreakpointResult createErrorResult(String msg) {
        BreakpointResult r = new BreakpointResult();
        r.setSuccess(false);
        r.setMessage(msg);
        return r;
    }
}
