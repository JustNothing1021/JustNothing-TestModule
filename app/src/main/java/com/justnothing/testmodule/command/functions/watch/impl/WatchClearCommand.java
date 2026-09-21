package com.justnothing.testmodule.command.functions.watch.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.watch.util.WatchManager;
import com.justnothing.testmodule.command.functions.watch.request.WatchClearRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchClearResult;
import com.justnothing.testmodule.command.functions.watch.WatchTexts;

@SubCommandInfo(
    description = WatchTexts.SUB_WATCH_CLEAR_DESC,
    usage = "watch clear",
    examples = {"watch clear"}
)
public class WatchClearCommand extends AbstractWatchCommand<WatchClearRequest, WatchClearResult> {

    public WatchClearCommand() {
        super("watch clear", WatchClearRequest.class, WatchClearResult.class);
    }

    @Override
    protected WatchClearResult executeWatchCommand(CommandExecutor.CmdExecContext<WatchClearRequest> context) throws Exception {
        WatchManager manager = WatchManager.getInstance();
        int count = manager.getTaskCount();
        
        manager.clearAll();
        
        context.println(Text.zhEn("已清除所有watch任务", "All watch tasks cleared").text(), Colors.GREEN);
        context.print(Text.zhEn("清除数量: ", "Cleared: ").text(), Colors.CYAN);
        context.println(String.valueOf(count), Colors.YELLOW);

        WatchClearResult result = new WatchClearResult();
        result.setClearedCount(count);
        return result;
    }
}
