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
    description = "监控字段或方法的变化, 非阻塞执行.",
    version = CMD_WATCH_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "add",
        request = WatchAddRequest.class,
        handler = WatchAddCommand.class,
        description = "添加字段或方法监控任务"
    ),
    @CmdRoutes.Route(
        path = "list",
        request = WatchListRequest.class,
        handler = WatchListCommand.class,
        description = "列出所有监控任务"
    ),
    @CmdRoutes.Route(
        path = "stop",
        request = WatchStopRequest.class,
        handler = WatchStopCommand.class,
        description = "停止指定的监控任务"
    ),
    @CmdRoutes.Route(
        path = "clear",
        request = WatchClearRequest.class,
        handler = WatchClearCommand.class,
        description = "清除所有监控任务"
    ),
    @CmdRoutes.Route(
        path = "output",
        request = WatchOutputRequest.class,
        handler = WatchOutputCommand.class,
        description = "获取监控任务的输出"
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
