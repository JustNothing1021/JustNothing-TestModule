package com.justnothing.testmodule.command.functions.watch.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.watch.AbstractWatchCommand;
import com.justnothing.testmodule.command.functions.watch.WatchManager;
import com.justnothing.testmodule.command.functions.watch.request.WatchStopRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchStopResult;

@SubCommandInfo(
    description = "停止指定的监控任务",
    usage = "watch stop <id>",
    examples = {"watch stop 1"}
)
public class StopCommand extends AbstractWatchCommand<WatchStopRequest, WatchStopResult> {

    public StopCommand() {
        super("watch stop", WatchStopRequest.class, WatchStopResult.class);
    }

    @Override
    protected WatchStopResult executeWatchCommand(CommandExecutor.CmdExecContext<WatchStopRequest> context) throws Exception {
        WatchStopRequest request = context.getCommandRequest();
        WatchManager manager = WatchManager.getInstance();
        
        Integer watchId = request.getWatchId();
        
        if (watchId == null) {
            context.println("错误: 参数不足", Colors.RED);
            context.println("用法: watch stop <id>", Colors.GRAY);
            return createErrorResult("参数不足: 需要watch ID");
        }

        boolean success = manager.removeTask(watchId);
        
        if (success) {
            context.println("已停止watch任务", Colors.GREEN);
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(watchId), Colors.YELLOW);
        } else {
            context.println("错误: 未找到watch任务", Colors.RED);
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(watchId), Colors.YELLOW);
        }

        WatchStopResult result = new WatchStopResult();
        result.setTaskId(watchId);
        result.setSuccess(success);
        return result;
    }
}
