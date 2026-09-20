package com.justnothing.testmodule.command.functions.watch;

import static com.justnothing.testmodule.constants.CommandServer.CMD_WATCH_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.watch.request.WatchAddRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchListRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchStopRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchClearRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchOutputRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchCommandResult;
import com.justnothing.testmodule.command.functions.watch.impl.WatchAddCommand;
import com.justnothing.testmodule.command.functions.watch.impl.WatchListCommand;
import com.justnothing.testmodule.command.functions.watch.impl.WatchStopCommand;
import com.justnothing.testmodule.command.functions.watch.impl.WatchClearCommand;
import com.justnothing.testmodule.command.functions.watch.impl.WatchOutputCommand;

@Cmd(
    name = "watch",
    description = WatchTexts.CMD_WATCH_DESC,
    version = CMD_WATCH_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "add",
        request = WatchAddRequest.class,
        handler = WatchAddCommand.class,
        description = WatchTexts.ROUTE_WATCH_ADD_DESC
    ),
    @CmdRoutes.Route(
        path = "list",
        request = WatchListRequest.class,
        handler = WatchListCommand.class,
        description = WatchTexts.ROUTE_WATCH_LIST_DESC
    ),
    @CmdRoutes.Route(
        path = "stop",
        request = WatchStopRequest.class,
        handler = WatchStopCommand.class,
        description = WatchTexts.ROUTE_WATCH_STOP_DESC
    ),
    @CmdRoutes.Route(
        path = "clear",
        request = WatchClearRequest.class,
        handler = WatchClearCommand.class,
        description = WatchTexts.ROUTE_WATCH_CLEAR_DESC
    ),
    @CmdRoutes.Route(
        path = "output",
        request = WatchOutputRequest.class,
        handler = WatchOutputCommand.class,
        description = WatchTexts.ROUTE_WATCH_OUTPUT_DESC
    )
})
public class WatchMain extends MainCommand<WatchCommandResult> {

    public WatchMain() {
        super("Watch", WatchCommandResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("watch");
    }
}
