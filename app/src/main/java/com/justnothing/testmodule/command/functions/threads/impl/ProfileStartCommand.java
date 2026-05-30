package com.justnothing.testmodule.command.functions.threads.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandArgumentParser;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.threads.AbstractThreadsCommand;
import com.justnothing.testmodule.command.functions.threads.ProfileManager;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileStartRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadProfileStartResult;

@SubCommandInfo(
    description = "开始性能分析",
    usage = "threads profile start [duration: seconds]",
    examples = {
        "threads profile start",
        "threads profile start 120"
    }
)
public class ProfileStartCommand extends AbstractThreadsCommand<ThreadProfileStartRequest, ThreadProfileStartResult> {

    public ProfileStartCommand() {
        super("threads profile start", ThreadProfileStartRequest.class, ThreadProfileStartResult.class);
    }

    @Override
    protected ThreadProfileStartResult executeThreadsCommand(CommandExecutor.CmdExecContext<ThreadProfileStartRequest> context) throws Exception {
        ThreadProfileStartRequest request = context.getCommandRequest();
        Integer duration = request.getDuration();

        try {
            CommandArgumentParser.requireMin(duration != null ? duration : 60, 1, "持续时间");
        } catch (IllegalArgumentException e) {
            context.print("错误: ", Colors.RED);
            context.println(e.getMessage(), Colors.YELLOW);
            return createErrorResult(e.getMessage());
        }

        int actualDuration = duration != null ? duration : 60;
        
        try {
            ProfileManager manager = ProfileManager.getInstance();
            manager.startProfiling(actualDuration);

            context.print("开始性能分析, 持续时间: ", Colors.LIGHT_GREEN);
            context.print(String.valueOf(actualDuration), Colors.YELLOW);
            context.println("秒", Colors.WHITE);
            context.println("提示: 使用 'threads profile show' 查看结果", Colors.GRAY);
            context.println("      使用 'threads profile stop' 提前停止", Colors.GRAY);

            ThreadProfileStartResult result = new ThreadProfileStartResult();
            result.setDuration(actualDuration);
            result.setSuccess(true);

            return result;
        } catch (Exception e) {
            CommandExceptionHandler.handleException("threads profile start", e, context, "启动性能分析失败");
            throw e;
        }
    }
}
