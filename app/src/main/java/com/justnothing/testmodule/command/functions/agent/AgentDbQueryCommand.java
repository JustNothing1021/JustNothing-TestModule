package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.agent.DbQueryResult;
import com.justnothing.testmodule.command.agent.InspectionClient;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.agent.request.AgentDbQueryRequest;

import java.util.Map;

public class AgentDbQueryCommand extends AbstractCommand<AgentDbQueryRequest, DbQueryResult> {

    public AgentDbQueryCommand() {
        super("agent db-query", AgentDbQueryRequest.class, DbQueryResult.class);
    }

    @Override
    protected DbQueryResult executeInternal(CommandExecutor.CmdExecContext<AgentDbQueryRequest> context) throws Exception {
        AgentDbQueryRequest req = context.getRequest();
        DbQueryResult result = InspectionClient.executeDbQuery(
                req.getPackageName(), req.getDbName(), req.getSql(), req.getLimit());

        if (context.isCli()) {
            context.println("[" + req.getPackageName() + "] DB Query: " + req.getDbName(), Colors.CYAN);
            context.println("Columns: " + result.getColumns() + " | Rows: " + result.getRowCount(), Colors.WHITE);
            context.println("---", Colors.WHITE);
            for (Map<String, Object> row : result.getRows()) {
                StringBuilder sb = new StringBuilder("  ");
                for (String col : result.getColumns()) {
                    sb.append(col).append("=").append(row.get(col)).append("  ");
                }
                context.println(sb.toString(), Colors.WHITE);
            }
        }

        return result;
    }
}
