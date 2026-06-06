package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.agent.InspectionClient;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentStartRequest;

public class AgentStartCommand extends AbstractCommand<AgentStartRequest, CommandResult> {

    public AgentStartCommand() {
        super("agent start", AgentStartRequest.class, CommandResult.class);
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<AgentStartRequest> context) throws Exception {
        String pkg = context.getRequest().getPackageName();
        boolean success = InspectionClient.requestStart(pkg);

        if (context.isCli()) {
            if (success) {
                context.println("已发送启动请求: " + pkg, Colors.GREEN);
                context.println("(目标应用需要已运行且被 Xposed 注入)", Colors.YELLOW);
            } else {
                context.println("启动请求失败: " + pkg, Colors.RED);
            }
        }

        CommandResult result = new CommandResult();
        result.setSuccess(success);
        result.setMessage(success ? "启动请求已发送" : "启动请求失败");
        return result;
    }
}
