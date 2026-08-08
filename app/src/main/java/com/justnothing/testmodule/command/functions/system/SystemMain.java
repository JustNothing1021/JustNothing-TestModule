package com.justnothing.testmodule.command.functions.system;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SYSTEM_VER;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.script.SystemInfoRequest;
import com.justnothing.testmodule.command.functions.system.impl.SystemInfoCommand;

@Cmd(
    name = "system",
    description = "显示系统信息 (CPU, 内存, OS, 属性)",
    version = CMD_SYSTEM_VER,
    defaultResultType = SystemInfoResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "",
        request = SystemInfoRequest.class,
        handler = SystemInfoCommand.class,
        description = "显示系统信息"
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

    @Override
    public SystemInfoResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        return new SystemInfoCommand().execute(context);
    }
}
