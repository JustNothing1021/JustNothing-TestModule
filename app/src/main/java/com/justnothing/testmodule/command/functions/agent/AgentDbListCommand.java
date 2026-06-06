package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.agent.DbListResult;
import com.justnothing.testmodule.command.agent.InspectionClient;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentDbListRequest;

public class AgentDbListCommand extends AbstractCommand<AgentDbListRequest, DbListResult> {

    public AgentDbListCommand() {
        super("agent db-list", AgentDbListRequest.class, DbListResult.class);
    }

    @Override
    protected DbListResult executeInternal(CommandExecutor.CmdExecContext<AgentDbListRequest> context) throws Exception {
        String pkg = context.getRequest().getPackageName();
        DbListResult result = InspectionClient.executeDbList(pkg);

        if (context.isCli()) {
            context.println("[" + pkg + "] Databases (" + result.getDbFiles().size() + "):", Colors.CYAN);
            for (DbListResult.DbFileInfo info : result.getDbFiles()) {
                String tag = info.isDatabase() ? "[DB]" : "[--]";
                context.println(String.format("  %s %-30s %8d bytes", tag,
                        info.getName(), info.getSizeBytes()), Colors.WHITE);
            }
        }

        return result;
    }
}
