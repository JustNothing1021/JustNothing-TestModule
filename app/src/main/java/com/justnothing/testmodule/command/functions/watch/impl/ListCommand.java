package com.justnothing.testmodule.command.functions.watch.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.watch.AbstractWatchCommand;
import com.justnothing.testmodule.command.functions.watch.WatchManager;
import com.justnothing.testmodule.command.functions.watch.request.WatchListRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchListResult;

@SubCommandInfo(
    description = "列出所有监控任务",
    usage = "watch list",
    examples = {"watch list"}
)
public class ListCommand extends AbstractWatchCommand<WatchListRequest, WatchListResult> {

    public ListCommand() {
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
