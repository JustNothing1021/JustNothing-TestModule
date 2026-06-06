package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.agent.InspectionClient;
import com.justnothing.testmodule.command.output.Colors;
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
                context.println("没有在线的 InspectionAgent", Colors.YELLOW);
            } else {
                context.println("在线 InspectionAgent (" + agents.size() + "):", Colors.CYAN);
                context.println(String.format("  %-40s %-12s %s", "包名", "状态", "启动时间"), Colors.WHITE);
                context.println("  " + "-".repeat(70), Colors.DARK_GRAY);

                SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
                for (InspectionClient.AgentStatus status : agents) {
                    String statusStr = status.online() ? "ONLINE" : "DEAD";
                    byte color = status.online() ? Colors.GREEN : Colors.RED;
                String timeStr = status.startTime() > 0 ? fmt.format(new Date(status.startTime())) : "-";
                context.println(String.format("  %-40s %-12s %s",
                        status.packageName(), statusStr, timeStr), color);
                    if (!status.online() && status.error() != null) {
                        context.println("    原因: " + status.error(), Colors.RED);
                    }
                }
            }
        }

        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setResultType("agent_list");
        result.setData(agents);
        return result;
    }
}
