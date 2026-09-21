package com.justnothing.testmodule.command.functions.agent.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.functions.agent.inspect.InspectionClient;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentStopRequest;

public class AgentStopCommand extends AbstractCommand<AgentStopRequest, CommandResult> {

    public AgentStopCommand() {
        super("agent stop", AgentStopRequest.class, CommandResult.class);
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<AgentStopRequest> context) throws Exception {
        String pkg = context.getRequest().getPackageName();
        boolean success = InspectionClient.requestStop(pkg);

        if (context.isCli()) {
            if (success) {
                context.println(Text.zhEn("已停止: ", "Stopped: ").text() + pkg, Colors.GREEN);
                context.println(Text.zhEn("(ServerSocket 已关闭, .info 文件已清理)", "(ServerSocket closed, .info file cleaned up)").text(), Colors.YELLOW);
            } else {
                context.println(Text.zhEn("停止失败: ", "Stop failed: ").text() + pkg, Colors.RED);
            }
        }

        CommandResult result = new CommandResult();
        result.setSuccess(true); // stop 本身总是成功（即使 agent 不在线）
        result.setMessage(Text.zhEn("stop 请求已完成", "Stop request completed").text());
        return result;
    }
}
