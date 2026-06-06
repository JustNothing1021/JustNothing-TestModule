package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.agent.InspectionClient;
import com.justnothing.testmodule.command.agent.SpWriteResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentSpWriteRequest;

public class AgentSpWriteCommand extends AbstractCommand<AgentSpWriteRequest, SpWriteResult> {

    public AgentSpWriteCommand() {
        super("agent sp-write", AgentSpWriteRequest.class, SpWriteResult.class);
    }

    @Override
    protected SpWriteResult executeInternal(CommandExecutor.CmdExecContext<AgentSpWriteRequest> context) throws Exception {
        AgentSpWriteRequest req = context.getRequest();
        SpWriteResult result = InspectionClient.executeSpWrite(
                req.getPackageName(), req.getSpName(), req.getKey(),
                req.getValue(), req.getValueType());

        if (context.isCli()) {
            byte statusColor = result.isCommitted() ? Colors.GREEN : Colors.RED;
            context.println("[" + req.getPackageName() +"] SP Write: " + req.getSpName()
                    + "." + req.getKey() + " = " + req.getValue() + " ["
                    + (result.isCommitted() ? "SUCCESS" : "FAILED") + "]", statusColor);
        }

        return result;
    }
}
