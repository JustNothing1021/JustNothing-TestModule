package com.justnothing.testmodule.command.functions.help;

import static com.justnothing.testmodule.constants.CommandServer.CMD_HELP_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.framework.model.NoArgRequest;
import com.justnothing.testmodule.command.functions.help.impl.HelpCommand;

@Cmd(
    version = CMD_HELP_VER,
    name = "help",
    description = HelpTexts.CMD_HELP_DESC
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "",
        request = NoArgRequest.class,
        handler = HelpCommand.class,
        description = HelpTexts.ROUTE_HELP_DESC
    )
})
public class HelpMain extends MainCommand<CommandResult> {

    public HelpMain() {
        super("help", CommandResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("help");
    }
}
