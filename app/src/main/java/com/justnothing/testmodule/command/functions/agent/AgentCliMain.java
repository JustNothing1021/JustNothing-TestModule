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
    description = "跨应用 InspectionAgent IPC 桥接命令"
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "sp-list",
        request = AgentSpListRequest.class,
        handler = AgentSpListCommand.class,
        description = "列出目标应用的 SharedPreferences 文件"
    ),
    @CmdRoutes.Route(
        path = "sp-read",
        request = AgentSpReadRequest.class,
        handler = AgentSpReadCommand.class,
        description = "读取目标应用的 SharedPreferences"
    ),
    @CmdRoutes.Route(
        path = "sp-write",
        request = AgentSpWriteRequest.class,
        handler = AgentSpWriteCommand.class,
        description = "写入目标应用的 SharedPreferences"
    ),
    @CmdRoutes.Route(
        path = "db-list",
        request = AgentDbListRequest.class,
        handler = AgentDbListCommand.class,
        description = "列出目标应用的数据库文件"
    ),
    @CmdRoutes.Route(
        path = "db-query",
        request = AgentDbQueryRequest.class,
        handler = AgentDbQueryCommand.class,
        description = "查询目标应用的 SQLite 数据库"
    ),
    @CmdRoutes.Route(
        path = "db-tables",
        request = AgentDbTablesRequest.class,
        handler = AgentDbTablesCommand.class,
        description = "列出目标应用数据库的所有表"
    ),
    @CmdRoutes.Route(
        path = "list",
        request = AgentListRequest.class,
        handler = AgentListCommand.class,
        description = "列出所有在线的 InspectionAgent（自动清理死文件）"
    ),
    @CmdRoutes.Route(
        path = "start",
        request = AgentStartRequest.class,
        handler = AgentStartCommand.class,
        description = "请求启动目标应用的 InspectionAgent"
    ),
    @CmdRoutes.Route(
        path = "stop",
        request = AgentStopRequest.class,
        handler = AgentStopCommand.class,
        description = "停止目标应用的 InspectionAgent（关闭 ServerSocket + 清理文件）"
    ),
    @CmdRoutes.Route(
        path = "run",
        request = AgentRunRequest.class,
        handler = AgentRunCommand.class,
        description = "在目标应用上代理执行任意主服务命令"
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
