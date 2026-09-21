package com.justnothing.testmodule.command.functions.watch.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.watch.util.WatchManager;
import com.justnothing.testmodule.command.functions.watch.request.WatchStopRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchStopResult;
import com.justnothing.testmodule.command.functions.watch.WatchTexts;

@SubCommandInfo(
    description = WatchTexts.SUB_WATCH_STOP_DESC,
    usage = "watch stop <id>",
    examples = {"watch stop 1"}
)
public class WatchStopCommand extends AbstractWatchCommand<WatchStopRequest, WatchStopResult> {

    public WatchStopCommand() {
        super("watch stop", WatchStopRequest.class, WatchStopResult.class);
    }

    @Override
    protected WatchStopResult executeWatchCommand(CommandExecutor.CmdExecContext<WatchStopRequest> context) throws Exception {
        WatchStopRequest request = context.getCommandRequest();
        WatchManager manager = WatchManager.getInstance();
        
        Integer watchId = request.getWatchId();
        
        if (watchId == null) {
            context.println(CliMessages.ERROR_PREFIX.text() + CliMessages.ERR_NOT_ENOUGH_ARGS.text(), Colors.RED);
            context.println(CliMessages.HELP_USAGE_INLINE.text() + "watch stop <id>", Colors.GRAY);
            return createErrorResult(Text.zhEn(
                    "参数不足: 需要watch ID",
                    "not enough arguments: a watch ID is required").text());
        }

        boolean success = manager.removeTask(watchId);
        
        if (success) {
            context.println(Text.zhEn("已停止watch任务", "Watch task stopped").text(), Colors.GREEN);
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(watchId), Colors.YELLOW);
        } else {
            context.println(CliMessages.ERROR_PREFIX.text()
                    + Text.zhEn("未找到watch任务", "watch task not found").text(), Colors.RED);
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(watchId), Colors.YELLOW);
        }

        WatchStopResult result = new WatchStopResult();
        result.setTaskId(watchId);
        result.setSuccess(success);
        return result;
    }
}
