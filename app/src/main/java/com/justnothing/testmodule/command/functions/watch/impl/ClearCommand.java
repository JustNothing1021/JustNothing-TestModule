package com.justnothing.testmodule.command.functions.watch.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.watch.AbstractWatchCommand;
import com.justnothing.testmodule.command.functions.watch.WatchManager;
import com.justnothing.testmodule.command.functions.watch.request.WatchClearRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchClearResult;

@SubCommandInfo(
    description = "清除所有监控任务",
    usage = "watch clear",
    examples = {"watch clear"}
)
public class ClearCommand extends AbstractWatchCommand<WatchClearRequest, WatchClearResult> {

    public ClearCommand() {
        super("watch clear", WatchClearRequest.class, WatchClearResult.class);
    }

    @Override
    protected WatchClearResult executeWatchCommand(CommandExecutor.CmdExecContext<WatchClearRequest> context) throws Exception {
        WatchManager manager = WatchManager.getInstance();
        int count = manager.getTaskCount();
        
        manager.clearAll();
        
        context.println("已清除所有watch任务", Colors.GREEN);
        context.print("清除数量: ", Colors.CYAN);
        context.println(String.valueOf(count), Colors.YELLOW);

        WatchClearResult result = new WatchClearResult();
        result.setClearedCount(count);
        return result;
    }
}
