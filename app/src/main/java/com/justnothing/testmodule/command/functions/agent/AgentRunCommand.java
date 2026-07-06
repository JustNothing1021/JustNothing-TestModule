package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.base.AbstractCommand;
import com.justnothing.testmodule.command.framework.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.agent.handlers.InspectionClient;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentRunRequest;

public class AgentRunCommand extends AbstractCommand<AgentRunRequest, CommandResult> {

    public AgentRunCommand() {
        super("agent run", AgentRunRequest.class, CommandResult.class);
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<AgentRunRequest> context) throws Exception {
        String pkg = context.getRequest().getPackageName();
        String cmd = context.getRequest().getCommand();

        context.println("[代理执行] " + pkg + " → " + cmd + " (交互模式)", Colors.CYAN);
        context.println("---", Colors.DARK_GRAY);

        // 使用交互式协议在目标应用上执行命令
        // 输出：用 context.print() 原样转发（服务端数据已自带 \n）
        // 输入：直接用 context.readLine(prompt)，复用现有协议栈（和普通交互式命令一样的流程）
        final boolean isCli = context.isCli();
        InspectionClient.executeInteractive(pkg, cmd, new InspectionClient.InteractiveDispatchCallback() {
            @Override
            public void onSessionStart(String command) {}

            @Override
            public void onOutput(String text) {
                if (text != null) {
                    context.print(text);
                }
            }

            @Override
            public void onColoredOutput(String text, byte color) {
                if (text != null) {
                    context.print(text, color);
                }
            }

            @Override
            public void onError(String errorText) {
                if (errorText != null) {
                    context.print("[错误] " + errorText, Colors.RED);
                }
            }

            @Override
            public String onInputRequest(String prompt) {
                return context.readLine(prompt);
            }

            @Override
            public void onSessionEnd() {
                context.println("---", Colors.DARK_GRAY);
                context.println("[完成] 代理命令执行结束", Colors.GREEN);
            }
        });

        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setResultType("dispatch_interactive");
        return result;
    }
}
