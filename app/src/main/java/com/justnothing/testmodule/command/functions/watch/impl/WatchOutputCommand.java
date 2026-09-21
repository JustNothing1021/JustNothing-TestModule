package com.justnothing.testmodule.command.functions.watch.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.watch.util.WatchManager;
import com.justnothing.testmodule.command.functions.watch.request.WatchOutputRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchOutputResult;
import com.justnothing.testmodule.command.functions.watch.WatchTexts;

@SubCommandInfo(
    description = WatchTexts.SUB_WATCH_OUTPUT_DESC,
    usage = "watch output <id|all> [limit]",
    examples = {
        "watch output 1",
        "watch output all 50"
    }
)
public class WatchOutputCommand extends AbstractWatchCommand<WatchOutputRequest, WatchOutputResult> {

    public WatchOutputCommand() {
        super("watch output", WatchOutputRequest.class, WatchOutputResult.class);
    }

    @Override
    protected WatchOutputResult executeWatchCommand(CommandExecutor.CmdExecContext<WatchOutputRequest> context) throws Exception {
        WatchOutputRequest request = context.getCommandRequest();
        WatchManager manager = WatchManager.getInstance();
        
        String target = request.getTarget();
        Integer limit = request.getLimit();
        
        if (target == null) {
            context.println(CliMessages.ERROR_PREFIX.text() + CliMessages.ERR_NOT_ENOUGH_ARGS.text(), Colors.RED);
            context.println(CliMessages.HELP_USAGE_INLINE.text() + "watch output <id|all> [limit]", Colors.GRAY);
            context.println(Text.zhEn("选项:", "Options:").text(), Colors.CYAN);
            context.println(Text.zhEn(
                    "  - limit: 输出条数限制，默认20",
                    "  - limit: max number of output lines, 20 by default").text(), Colors.GRAY);
            return createErrorResult(Text.zhEn(
                    "参数不足: 需要目标ID或all",
                    "not enough arguments: a target ID or 'all' is required").text());
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
                context.println(CliMessages.ERROR_PREFIX.text() + Text.zhEn(
                        "ID必须是数字: %s",
                        "ID must be a number: %s").format(target), Colors.RED);
                return createErrorResult(Text.zhEn("无效ID: %s", "Invalid ID: %s").format(target));
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
