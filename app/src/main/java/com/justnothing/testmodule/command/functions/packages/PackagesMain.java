package com.justnothing.testmodule.command.functions.packages;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.packages.request.PackagesRequest;

@Cmd(
    name = "packages",
    description = "列出当前进程的所有已知包名"
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "list",
        request = PackagesRequest.class,
        handler = PackagesCommand.class,
        description = "列出所有包名"
    )
})
public class PackagesMain extends MainCommand<PackagesResult> {

    public PackagesMain() {
        super("packages", PackagesResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("packages");
    }
}
