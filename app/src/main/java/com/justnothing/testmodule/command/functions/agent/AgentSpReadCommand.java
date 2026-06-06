package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.agent.InspectionClient;
import com.justnothing.testmodule.command.agent.SpReadResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentSpReadRequest;

import java.util.Map;

public class AgentSpReadCommand extends AbstractCommand<AgentSpReadRequest, SpReadResult> {

    public AgentSpReadCommand() {
        super("agent sp-read", AgentSpReadRequest.class, SpReadResult.class);
    }

    @Override
    protected SpReadResult executeInternal(CommandExecutor.CmdExecContext<AgentSpReadRequest> context) throws Exception {
        AgentSpReadRequest req = context.getRequest();
        SpReadResult result = InspectionClient.executeSpRead(
                req.getPackageName(), req.getSpName(), req.getKeyFilter());

        if (context.isCli()) {
            context.println("[" + req.getPackageName() + "] SP: " + req.getSpName(), Colors.CYAN);
            if (req.getKeyFilter() != null && !req.getKeyFilter().isEmpty()) {
                Object val = result.getEntries().get(req.getKeyFilter());
                context.println("  " + req.getKeyFilter() + " = " + val, Colors.WHITE);
            } else {
                for (Map.Entry<String, Object> entry : result.getEntries().entrySet()) {
                    context.println("  " + entry.getKey() + " = " + entry.getValue(), Colors.WHITE);
                }
            }
        }

        return result;
    }
}
