package com.justnothing.testmodule.command.functions.watch.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.watch.util.WatchManager;
import com.justnothing.testmodule.command.functions.watch.request.WatchListRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchListResult;

@SubCommandInfo(
    description = "列出所有监控任务",
    usage = "watch list",
    examples = {"watch list"}
)
public class WatchListCommand extends AbstractWatchCommand<WatchListRequest, WatchListResult> {

    public WatchListCommand() {
        super("watch list", WatchListRequest.class, WatchListResult.class);
    }

    @Override
    protected WatchListResult executeWatchCommand(CommandExecutor.CmdExecContext<WatchListRequest> context) throws Exception {
        WatchManager manager = WatchManager.getInstance();
        String result = manager.getTaskListString();
        
        context.println(result, Colors.WHITE);
        
        WatchListResult watchListResult = new WatchListResult();
        return watchListResult;
    }
}
