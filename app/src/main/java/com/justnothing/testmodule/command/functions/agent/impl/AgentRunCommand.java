package com.justnothing.testmodule.command.functions.agent.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.functions.agent.inspect.InspectionClient;
import com.justnothing.testmodule.command.framework.output.ClientRequirements;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.output.ICommandOutputHandler;
import com.justnothing.testmodule.command.framework.output.InteractiveOutputHandler;
import com.justnothing.testmodule.command.framework.protocol.TerminalRpcChannel;
import com.justnothing.testmodule.command.functions.agent.request.AgentRunRequest;

public class AgentRunCommand extends AbstractCommand<AgentRunRequest, CommandResult> {

    public AgentRunCommand() {
        super("agent run", AgentRunRequest.class, CommandResult.class);
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<AgentRunRequest> context) throws Exception {
        String pkg = context.getRequest().getPackageName();
        String cmd = context.getRequest().getCommand();

        context.println(Text.zhEn("[代理执行] %s → %s (交互模式)", "[Proxy] %s → %s (interactive mode)")
                .format(pkg, cmd), Colors.CYAN);
        context.println("---", Colors.DARK_GRAY);

        // 目标进程没有自己的终端，RichConsole 渲染要靠客户端真实能力（宽高 / ANSI）——
        // 不透传的话它拿到的是 0x0 尺寸，渲染结果就是一片空白。
        ClientRequirements requirements = context.requirements();
        // 目标进程的渲染消息是发给我们（它的 RPC 对端）的，出口是 CLI 自己通往客户端的通道。
        // 拿不到通道（GUI/JSON 模式）就只能丢，等于维持"渲染不可达"。
        TerminalRpcChannel cliChannel = resolveRichChannel(context);

        // 输出：用 context.print() 原样转发（服务端数据已自带 \n）
        // 输入：直接用 context.readLine(prompt)，复用现有协议栈（和普通交互式命令一样的流程）
        InspectionClient.executeInteractive(pkg, cmd, requirements,
                (method, params) -> {
                    if (cliChannel != null) {
                        cliChannel.sendNotification(method, params);
                    }
                },
                new InspectionClient.InteractiveDispatchCallback() {
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
                    context.print(Text.zhEn("[错误] ", "[Error] ").text() + errorText, Colors.RED);
                }
            }

            @Override
            public String onInputRequest(String prompt) {
                return context.readLine(prompt);
            }

            @Override
            public void onSessionEnd() {
                context.println("---", Colors.DARK_GRAY);
                context.println(Text.zhEn("[完成] 代理命令执行结束", "[Done] Proxied command finished").text(), Colors.GREEN);
            }
        });

        CommandResult result = new CommandResult();
        result.setSuccess(true);
        return result;
    }

    /**
     * 取 CLI 自己那条通往最终客户端的 RPC 通道。
     *
     * <p>只有 {@link InteractiveOutputHandler} 才有通道；GUI / JSON 模式下拿不到，
     * 此时目标进程的渲染消息只能丢弃 —— 等于维持"渲染不可达"，但纯文本输出照常。</p>
     */
    private static TerminalRpcChannel resolveRichChannel(CommandExecutor.CmdExecContext<?> context) {
        ICommandOutputHandler handler = context.output();
        return handler instanceof InteractiveOutputHandler
                ? ((InteractiveOutputHandler) handler).getRpcChannel()
                : null;
    }
}
