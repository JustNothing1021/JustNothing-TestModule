package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.functions.agent.handlers.InspectionClient;
import com.justnothing.testmodule.command.functions.agent.handlers.SpListResult;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentSpListRequest;

public class AgentSpListCommand extends AbstractCommand<AgentSpListRequest, SpListResult> {

    public AgentSpListCommand() {
        super("agent sp-list", AgentSpListRequest.class, SpListResult.class);
    }

    @Override
    protected SpListResult executeInternal(CommandExecutor.CmdExecContext<AgentSpListRequest> context) throws Exception {
        String pkg = context.getRequest().getPackageName();
        SpListResult result = InspectionClient.executeSpList(pkg);

        if (context.isCli()) {
            context.println("[" + pkg + "] SharedPreferences (" + result.getSpFiles().size() + "):", Colors.CYAN);
            for (SpListResult.SpFileInfo info : result.getSpFiles()) {
                context.println(String.format("  %-30s %8d bytes  %d keys  %s",
                        info.getName(), info.getSizeBytes(), info.getKeyCount(), info.getFile()), Colors.WHITE);
            }
        }

        return result;
    }
}
