package com.justnothing.testmodule.command.functions.packages;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.packages.impl.PackagesCommand;
import com.justnothing.testmodule.command.functions.packages.request.PackagesRequest;
import com.justnothing.testmodule.command.functions.packages.response.PackagesResult;

@Cmd(
    name = "packages",
    description = PackagesTexts.CMD_PACKAGES_DESC
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "list",
        request = PackagesRequest.class,
        handler = PackagesCommand.class,
        description = PackagesTexts.ROUTE_PACKAGES_LIST_DESC
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
