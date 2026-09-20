package com.justnothing.testmodule.command.functions.breakpoint;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BREAKPOINT_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.breakpoint.impl.BreakpointManageCommand;
import com.justnothing.testmodule.command.functions.breakpoint.impl.BreakpointQueryCommand;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointAddRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointListRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointEnableRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointDisableRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointRemoveRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointClearRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointHitsRequest;
import com.justnothing.testmodule.command.functions.breakpoint.response.BreakpointResult;

@Cmd(
    name = "breakpoint",
    description = BreakpointTexts.CMD_BREAKPOINT_DESC,
    version = CMD_BREAKPOINT_VER
)
@CmdRoutes({
    @CmdRoutes.Route(path = "add", request = BreakpointAddRequest.class, handler = BreakpointManageCommand.class, description = BreakpointTexts.ROUTE_BREAKPOINT_ADD_DESC),
    @CmdRoutes.Route(path = "list", request = BreakpointListRequest.class, handler = BreakpointQueryCommand.class, description = BreakpointTexts.ROUTE_BREAKPOINT_LIST_DESC),
    @CmdRoutes.Route(path = "enable", request = BreakpointEnableRequest.class, handler = BreakpointManageCommand.class, description = BreakpointTexts.ROUTE_BREAKPOINT_ENABLE_DESC),
    @CmdRoutes.Route(path = "disable", request = BreakpointDisableRequest.class, handler = BreakpointManageCommand.class, description = BreakpointTexts.ROUTE_BREAKPOINT_DISABLE_DESC),
    @CmdRoutes.Route(path = "remove", request = BreakpointRemoveRequest.class, handler = BreakpointManageCommand.class, description = BreakpointTexts.ROUTE_BREAKPOINT_REMOVE_DESC),
    @CmdRoutes.Route(path = "clear", request = BreakpointClearRequest.class, handler = BreakpointManageCommand.class, description = BreakpointTexts.ROUTE_BREAKPOINT_CLEAR_DESC),
    @CmdRoutes.Route(path = "hits", request = BreakpointHitsRequest.class, handler = BreakpointQueryCommand.class, description = BreakpointTexts.ROUTE_BREAKPOINT_HITS_DESC)
})
public class BreakpointMain extends MainCommand<BreakpointResult> {

    public BreakpointMain() {
        super("Breakpoint", BreakpointResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("breakpoint");
    }
}
