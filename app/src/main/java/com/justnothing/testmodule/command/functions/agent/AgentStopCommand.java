package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.agent.InspectionClient;
import com.justnothing.testmodule.command.output.Colors;
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
                context.println("已停止: " + pkg, Colors.GREEN);
                context.println("(ServerSocket 已关闭, .info 文件已清理)", Colors.YELLOW);
            } else {
                context.println("停止失败: " + pkg, Colors.RED);
            }
        }

        CommandResult result = new CommandResult();
        result.setSuccess(true); // stop 本身总是成功（即使 agent 不在线）
        result.setMessage("stop 请求已完成");
        return result;
    }
}
