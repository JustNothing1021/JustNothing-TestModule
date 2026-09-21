package com.justnothing.testmodule.command.functions.threads.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.threads.ThreadsTexts;
import com.justnothing.testmodule.command.framework.utils.CommandArgumentParser;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.threads.util.ProfileManager;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileStartRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadProfileStartResult;

@SubCommandInfo(
    description = ThreadsTexts.SUB_THREADS_PROFILE_START_DESC,
    usage = "threads profile start [duration: seconds]",
    examples = {
        "threads profile start",
        "threads profile start 120"
    }
)
public class ThreadProfileStartCommand extends AbstractThreadsCommand<ThreadProfileStartRequest, ThreadProfileStartResult> {

    public ThreadProfileStartCommand() {
        super("threads profile start", ThreadProfileStartRequest.class, ThreadProfileStartResult.class);
    }

    @Override
    protected ThreadProfileStartResult executeThreadsCommand(CommandExecutor.CmdExecContext<ThreadProfileStartRequest> context) throws Exception {
        ThreadProfileStartRequest request = context.getCommandRequest();
        Integer duration = request.getDuration();

        try {
            CommandArgumentParser.requireMin(duration != null ? duration : 60, 1, "持续时间");
        } catch (IllegalArgumentException e) {
            context.print(CliMessages.ERROR_PREFIX.text(), Colors.RED);
            context.println(e.getMessage(), Colors.YELLOW);
            return createErrorResult(e.getMessage());
        }

        int actualDuration = duration != null ? duration : 60;
        
        try {
            ProfileManager manager = ProfileManager.getInstance();
            manager.startProfiling(actualDuration);

            context.print(Text.zhEn("开始性能分析, 持续时间: ", "Started profiling, duration: ").text(), Colors.LIGHT_GREEN);
            context.print(String.valueOf(actualDuration), Colors.YELLOW);
            context.println(Text.zhEn("秒", "s").text(), Colors.WHITE);
            context.println(ThreadsTexts.HINT_VIEW_RESULTS.text(), Colors.GRAY);
            context.println(Text.zhEn("      使用 'threads profile stop' 提前停止",
                    "      run 'threads profile stop' to stop early").text(), Colors.GRAY);

            ThreadProfileStartResult result = new ThreadProfileStartResult();
            result.setDuration(actualDuration);
            result.setSuccess(true);

            return result;
        } catch (Exception e) {
            CommandExceptionHandler.handleException("threads profile start", e, context,
                    Text.zhEn("启动性能分析失败", "Failed to start profiling").text());
            throw e;
        }
    }
}
