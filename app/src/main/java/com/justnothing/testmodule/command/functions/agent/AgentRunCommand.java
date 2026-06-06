package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.agent.InspectionClient;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentRunRequest;

public class AgentRunCommand extends AbstractCommand<AgentRunRequest, CommandResult> {

    public AgentRunCommand() {
        super("agent run", AgentRunRequest.class, CommandResult.class);
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<AgentRunRequest> context) throws Exception {
        String pkg = context.getRequest().getPackageName();
        String cmd = context.getRequest().getCommand();

        if (context.isCli()) {
            context.println("[代理执行] " + pkg + " → " + cmd + " (交互模式)", Colors.CYAN);
            context.println("---", Colors.DARK_GRAY);
        }

        // 使用交互式协议在目标应用上执行命令
        // 输出：用 context.print() 原样转发（服务端数据已自带 \n）
        // 输入：直接用 context.readLine(prompt)，复用现有协议栈（和普通交互式命令一样的流程）
        final boolean isCli = context.isCli();
        InspectionClient.executeInteractive(pkg, cmd, new InspectionClient.InteractiveDispatchCallback() {
            @Override
            public void onSessionStart(String command) {}

            @Override
            public void onOutput(String text) {
                if (isCli && text != null) {
                    context.print(text);
                }
            }

            @Override
            public void onColoredOutput(String text, byte color) {
                if (isCli && text != null) {
                    context.print(text, color);
                }
            }

            @Override
            public void onError(String errorText) {
                if (isCli && errorText != null) {
                    context.print("[错误] " + errorText, Colors.RED);
                }
            }

            @Override
            public String onInputRequest(String prompt) {
                // 直接复用 context 的 readLine，走标准交互式协议：
                // InteractiveOutputHandler.readLineFromClient(prompt)
                //   → 向原始客户端发 TYPE_SERVER_INPUT_REQUEST(UUID:prompt)
                //   → 原始客户端 SocketStreamReader 用 TerminalManager.readLine(prompt) 读输入
                //   → 回发 TYPE_INPUT_RESPONSE → 返回结果
                return context.readLine(prompt);
            }

            @Override
            public void onSessionEnd() {
                if (isCli) {
                    context.println("---", Colors.DARK_GRAY);
                    context.println("[完成] 代理命令执行结束", Colors.GREEN);
                }
            }
        });

        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setResultType("dispatch_interactive");
        return result;
    }
}
