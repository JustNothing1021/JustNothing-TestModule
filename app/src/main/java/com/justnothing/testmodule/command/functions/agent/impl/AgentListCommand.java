package com.justnothing.testmodule.command.functions.agent.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.functions.agent.inspect.InspectionClient;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentListRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AgentListCommand extends AbstractCommand<AgentListRequest, CommandResult> {

    public AgentListCommand() {
        super("agent list", AgentListRequest.class, CommandResult.class);
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<AgentListRequest> context) throws Exception {
        List<InspectionClient.AgentStatus> agents = InspectionClient.listAllAgents();

        if (context.isCli()) {
            if (agents.isEmpty()) {
                context.println(Text.zhEn("没有在线的 InspectionAgent", "No online InspectionAgent").text(), Colors.YELLOW);
            } else {
                context.println(Text.zhEn("在线 InspectionAgent (%d):", "Online InspectionAgents (%d):").format(agents.size()), Colors.CYAN);
                context.println(String.format("  %-40s %-12s %s",
                        Text.zhEn("包名", "Package").text(),
                        Text.zhEn("状态", "Status").text(),
                        Text.zhEn("启动时间", "Start time").text()), Colors.WHITE);
                context.println("  " + "-".repeat(70), Colors.DARK_GRAY);

                SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
                for (InspectionClient.AgentStatus status : agents) {
                    String statusStr = status.online() ? "ONLINE" : "DEAD";
                    byte color = status.online() ? Colors.GREEN : Colors.RED;
                String timeStr = status.startTime() > 0 ? fmt.format(new Date(status.startTime())) : "-";
                context.println(String.format("  %-40s %-12s %s",
                        status.packageName(), statusStr, timeStr), color);
                    if (!status.online() && status.error() != null) {
                        context.println(Text.zhEn("    原因: ", "    Reason: ").text() + status.error(), Colors.RED);
                    }
                }
            }
        }

        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setData(agents);
        return result;
    }
}
