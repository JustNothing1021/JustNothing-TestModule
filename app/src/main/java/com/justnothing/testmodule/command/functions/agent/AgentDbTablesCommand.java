package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.agent.DbTablesResult;
import com.justnothing.testmodule.command.agent.InspectionClient;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentDbTablesRequest;

public class AgentDbTablesCommand extends AbstractCommand<AgentDbTablesRequest, DbTablesResult> {

    public AgentDbTablesCommand() {
        super("agent db-tables", AgentDbTablesRequest.class, DbTablesResult.class);
    }

    @Override
    protected DbTablesResult executeInternal(CommandExecutor.CmdExecContext<AgentDbTablesRequest> context) throws Exception {
        AgentDbTablesRequest req = context.getRequest();
        DbTablesResult result = InspectionClient.executeDbTables(req.getPackageName(), req.getDbName());

        if (context.isCli()) {
            context.println("[" + req.getPackageName() + "] Tables in " + req.getDbName()
                    + " (" + result.getTables().size() + "):", Colors.CYAN);
            for (String table : result.getTables()) {
                context.println("  " + table, Colors.WHITE);
            }
        }

        return result;
    }
}
