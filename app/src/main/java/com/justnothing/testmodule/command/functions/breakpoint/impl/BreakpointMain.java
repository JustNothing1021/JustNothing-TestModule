package com.justnothing.testmodule.command.functions.breakpoint.impl;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BREAKPOINT_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
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
    description = "设置和管理断点",
    version = CMD_BREAKPOINT_VER
)
@CmdRoutes({
    @CmdRoutes.Route(path = "add", request = BreakpointAddRequest.class, handler = BreakpointManageCommand.class, description = "添加断点"),
    @CmdRoutes.Route(path = "list", request = BreakpointListRequest.class, handler = BreakpointQueryCommand.class, description = "列出所有断点"),
    @CmdRoutes.Route(path = "enable", request = BreakpointEnableRequest.class, handler = BreakpointManageCommand.class, description = "启用断点"),
    @CmdRoutes.Route(path = "disable", request = BreakpointDisableRequest.class, handler = BreakpointManageCommand.class, description = "禁用断点"),
    @CmdRoutes.Route(path = "remove", request = BreakpointRemoveRequest.class, handler = BreakpointManageCommand.class, description = "移除断点"),
    @CmdRoutes.Route(path = "clear", request = BreakpointClearRequest.class, handler = BreakpointManageCommand.class, description = "清除所有断点"),
    @CmdRoutes.Route(path = "hits", request = BreakpointHitsRequest.class, handler = BreakpointQueryCommand.class, description = "显示断点命中统计")
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
