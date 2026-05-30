package com.justnothing.testmodule.command.functions.watch.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.watch.AbstractWatchCommand;
import com.justnothing.testmodule.command.functions.watch.WatchManager;
import com.justnothing.testmodule.command.functions.watch.request.WatchOutputRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchOutputResult;

@SubCommandInfo(
    description = "获取监控任务的输出",
    usage = "watch output <id|all> [limit]",
    examples = {
        "watch output 1",
        "watch output all 50"
    }
)
public class OutputCommand extends AbstractWatchCommand<WatchOutputRequest, WatchOutputResult> {

    public OutputCommand() {
        super("watch output", WatchOutputRequest.class, WatchOutputResult.class);
    }

    @Override
    protected WatchOutputResult executeWatchCommand(CommandExecutor.CmdExecContext<WatchOutputRequest> context) throws Exception {
        WatchOutputRequest request = context.getCommandRequest();
        WatchManager manager = WatchManager.getInstance();
        
        String target = request.getTarget();
        Integer limit = request.getLimit();
        
        if (target == null) {
            context.println("错误: 参数不足", Colors.RED);
            context.println("用法: watch output <id|all> [limit]", Colors.GRAY);
            context.println("选项:", Colors.CYAN);
            context.println("  - limit: 输出条数限制，默认20", Colors.GRAY);
            return createErrorResult("参数不足: 需要目标ID或all");
        }

        int actualLimit = limit != null ? limit : 20;
        String output;

        if ("all".equals(target)) {
            output = manager.getAllWatchOutput(actualLimit);
        } else {
            try {
                int id = Integer.parseInt(target);
                output = manager.getTaskOutput(id, actualLimit);
            } catch (NumberFormatException e) {
                context.println("错误: ID必须是数字: " + target, Colors.RED);
                return createErrorResult("无效ID: " + target);
            }
        }

        context.println(output, Colors.WHITE);

        WatchOutputResult result = new WatchOutputResult();
        result.setOutput(output);
        result.setLimit(actualLimit);
        
        if (!"all".equals(target)) {
            try {
                result.setTaskId(Integer.parseInt(target));
            } catch (NumberFormatException ignored) {}
        }
        
        return result;
    }
}
