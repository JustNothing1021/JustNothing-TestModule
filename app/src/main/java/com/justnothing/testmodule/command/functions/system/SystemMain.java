package com.justnothing.testmodule.command.functions.system;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SYSTEM_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.script.request.SystemInfoRequest;
import com.justnothing.testmodule.command.functions.system.impl.SystemInfoCommand;
import com.justnothing.testmodule.command.functions.system.response.SystemInfoResult;

@Cmd(
    name = "system",
    description = SystemTexts.CMD_SYSTEM_DESC,
    version = CMD_SYSTEM_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "",
        request = SystemInfoRequest.class,
        handler = SystemInfoCommand.class,
        description = SystemTexts.ROUTE_SYSTEM_DESC
    )
})
public class SystemMain extends MainCommand<SystemInfoResult> {

    public SystemMain() {
        super("system", SystemInfoResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("system");
    }
}
