package com.justnothing.testmodule.command.functions.trace;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;

import com.justnothing.testmodule.command.functions.trace.request.TraceAddRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceListRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceShowRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceExportRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceStopRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceClearRequest;
import com.justnothing.testmodule.command.functions.trace.impl.TraceManageCommand;
import com.justnothing.testmodule.command.functions.trace.impl.TraceQueryCommand;
import com.justnothing.testmodule.command.functions.trace.response.TraceResult;

@Cmd(
    name = "trace",
    description = TraceTexts.CMD_TRACE_DESC
)
@CmdRoutes({
        @CmdRoutes.Route(path = "add", request = TraceAddRequest.class, handler = TraceManageCommand.class, description = TraceTexts.ROUTE_TRACE_ADD_DESC),
        @CmdRoutes.Route(path = "list", request = TraceListRequest.class, handler = TraceQueryCommand.class, description = TraceTexts.ROUTE_TRACE_LIST_DESC),
        @CmdRoutes.Route(path = "show", request = TraceShowRequest.class, handler = TraceQueryCommand.class, description = TraceTexts.ROUTE_TRACE_SHOW_DESC),
        @CmdRoutes.Route(path = "export", request = TraceExportRequest.class, handler = TraceQueryCommand.class, description = TraceTexts.ROUTE_TRACE_EXPORT_DESC),
        @CmdRoutes.Route(path = "stop", request = TraceStopRequest.class, handler = TraceManageCommand.class, description = TraceTexts.ROUTE_TRACE_STOP_DESC),
        @CmdRoutes.Route(path = "clear", request = TraceClearRequest.class, handler = TraceManageCommand.class, description = TraceTexts.ROUTE_TRACE_CLEAR_DESC)
    })
public class TraceMain extends MainCommand<TraceResult> {

    public TraceMain() {
        super("trace", TraceResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("trace");
    }
}
