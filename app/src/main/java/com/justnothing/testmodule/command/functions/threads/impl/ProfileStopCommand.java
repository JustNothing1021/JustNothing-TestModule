package com.justnothing.testmodule.command.functions.threads.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.threads.AbstractThreadsCommand;
import com.justnothing.testmodule.command.functions.threads.ProfileManager;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileStopRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadProfileStopResult;

@SubCommandInfo(
    description = "停止当前的性能分析",
    usage = "threads profile stop",
    examples = {"threads profile stop"}
)
public class ProfileStopCommand extends AbstractThreadsCommand<ThreadProfileStopRequest, ThreadProfileStopResult> {

    public ProfileStopCommand() {
        super("threads profile stop", ThreadProfileStopRequest.class, ThreadProfileStopResult.class);
    }

    @Override
    protected ThreadProfileStopResult executeThreadsCommand(CommandExecutor.CmdExecContext<ThreadProfileStopRequest> context) throws Exception {
        ProfileManager manager = ProfileManager.getInstance();

        try {
            manager.stopProfiling();
            context.println("已停止性能分析", Colors.GREEN);
            context.println("提示: 使用 'threads profile show' 查看结果", Colors.GRAY);

            ThreadProfileStopResult result = new ThreadProfileStopResult();
            result.setSuccess(true);
            result.setMessage("成功停止");
            return result;
        } catch (IllegalStateException e) {
            context.println("当前没有正在进行的性能分析", Colors.YELLOW);
            
            ThreadProfileStopResult result = new ThreadProfileStopResult();
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        } catch (Exception e) {
            context.println("停止性能分析失败: " + e.getMessage(), Colors.RED);
            
            ThreadProfileStopResult result = new ThreadProfileStopResult();
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
    }
}
