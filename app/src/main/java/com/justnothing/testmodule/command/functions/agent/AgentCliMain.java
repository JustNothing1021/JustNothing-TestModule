package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.agent.impl.AgentDbListCommand;
import com.justnothing.testmodule.command.functions.agent.impl.AgentDbQueryCommand;
import com.justnothing.testmodule.command.functions.agent.impl.AgentDbTablesCommand;
import com.justnothing.testmodule.command.functions.agent.impl.AgentListCommand;
import com.justnothing.testmodule.command.functions.agent.impl.AgentRunCommand;
import com.justnothing.testmodule.command.functions.agent.impl.AgentSpListCommand;
import com.justnothing.testmodule.command.functions.agent.impl.AgentSpReadCommand;
import com.justnothing.testmodule.command.functions.agent.impl.AgentSpWriteCommand;
import com.justnothing.testmodule.command.functions.agent.impl.AgentStartCommand;
import com.justnothing.testmodule.command.functions.agent.impl.AgentStopCommand;
import com.justnothing.testmodule.command.functions.agent.request.AgentSpListRequest;
import com.justnothing.testmodule.command.functions.agent.request.AgentSpReadRequest;
import com.justnothing.testmodule.command.functions.agent.request.AgentSpWriteRequest;
import com.justnothing.testmodule.command.functions.agent.request.AgentDbListRequest;
import com.justnothing.testmodule.command.functions.agent.request.AgentDbQueryRequest;
import com.justnothing.testmodule.command.functions.agent.request.AgentDbTablesRequest;
import com.justnothing.testmodule.command.functions.agent.request.AgentListRequest;
import com.justnothing.testmodule.command.functions.agent.request.AgentStartRequest;
import com.justnothing.testmodule.command.functions.agent.request.AgentStopRequest;
import com.justnothing.testmodule.command.functions.agent.request.AgentRunRequest;

@Cmd(
    name = "agent",
    description = AgentTexts.CMD_AGENT_DESC
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "sp-list",
        request = AgentSpListRequest.class,
        handler = AgentSpListCommand.class,
        description = AgentTexts.ROUTE_AGENT_SP_LIST_DESC
    ),
    @CmdRoutes.Route(
        path = "sp-read",
        request = AgentSpReadRequest.class,
        handler = AgentSpReadCommand.class,
        description = AgentTexts.ROUTE_AGENT_SP_READ_DESC
    ),
    @CmdRoutes.Route(
        path = "sp-write",
        request = AgentSpWriteRequest.class,
        handler = AgentSpWriteCommand.class,
        description = AgentTexts.ROUTE_AGENT_SP_WRITE_DESC
    ),
    @CmdRoutes.Route(
        path = "db-list",
        request = AgentDbListRequest.class,
        handler = AgentDbListCommand.class,
        description = AgentTexts.ROUTE_AGENT_DB_LIST_DESC
    ),
    @CmdRoutes.Route(
        path = "db-query",
        request = AgentDbQueryRequest.class,
        handler = AgentDbQueryCommand.class,
        description = AgentTexts.ROUTE_AGENT_DB_QUERY_DESC
    ),
    @CmdRoutes.Route(
        path = "db-tables",
        request = AgentDbTablesRequest.class,
        handler = AgentDbTablesCommand.class,
        description = AgentTexts.ROUTE_AGENT_DB_TABLES_DESC
    ),
    @CmdRoutes.Route(
        path = "list",
        request = AgentListRequest.class,
        handler = AgentListCommand.class,
        description = AgentTexts.ROUTE_AGENT_LIST_DESC
    ),
    @CmdRoutes.Route(
        path = "start",
        request = AgentStartRequest.class,
        handler = AgentStartCommand.class,
        description = AgentTexts.ROUTE_AGENT_START_DESC
    ),
    @CmdRoutes.Route(
        path = "stop",
        request = AgentStopRequest.class,
        handler = AgentStopCommand.class,
        description = AgentTexts.ROUTE_AGENT_STOP_DESC
    ),
    @CmdRoutes.Route(
        path = "run",
        request = AgentRunRequest.class,
        handler = AgentRunCommand.class,
        description = AgentTexts.ROUTE_AGENT_RUN_DESC
    )
})
public class AgentCliMain extends MainCommand<CommandResult> {

    public AgentCliMain() {
        super("agent", CommandResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("agent");
    }
}
