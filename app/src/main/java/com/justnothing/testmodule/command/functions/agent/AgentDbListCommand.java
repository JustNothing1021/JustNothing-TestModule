package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.functions.agent.handlers.DbListResult;
import com.justnothing.testmodule.command.functions.agent.handlers.InspectionClient;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentDbListRequest;

import java.util.Locale;

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
                context.println(String.format(Locale.getDefault(),
                        "  %s %-30s %8d bytes", tag,
                        info.getName(), info.getSizeBytes()), Colors.WHITE);
            }
        }

        return result;
    }
}
