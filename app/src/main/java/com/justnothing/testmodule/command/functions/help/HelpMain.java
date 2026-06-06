package com.justnothing.testmodule.command.functions.help;

import static com.justnothing.testmodule.constants.CommandServer.CMD_HELP_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.help.impl.HelpCommand;

@Cmd(
    version = CMD_HELP_VER,
    name = "help",
    description = "获取命令帮助信息",
    defaultResultType = CommandResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "",
        request = CommandRequest.class,
        handler = HelpCommand.class,
        description = "显示帮助信息"
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

    @Override
    public CommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        return new HelpCommand().execute(context);
    }
}
