package com.justnothing.testmodule.command.functions.agent;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.output.Colors;
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
    description = "跨应用 InspectionAgent IPC 桥接命令",
    defaultResultType = CommandResult.class
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

    @Override
    public CommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        if (context.getRequest() == null) {
            context.println("用法: agent <子命令> <packageName> [参数...]", Colors.DEFAULT);
            context.println("", Colors.DEFAULT);
            context.println("子命令:", Colors.CYAN);
            context.println("  sp-list <pkg>              列出 SP 文件", Colors.WHITE);
            context.println("  sp-read <pkg> <name> [key] 读取 SP", Colors.WHITE);
            context.println("  sp-write <pkg> <n> <k> <v> [type] 写入 SP", Colors.WHITE);
            context.println("  db-list <pkg>              列出数据库", Colors.WHITE);
            context.println("  db-query <pkg> <db> <sql>  查询数据库", Colors.WHITE);
            context.println("  db-tables <pkg> <db>       列出表", Colors.WHITE);
            context.println("", Colors.DEFAULT);
            context.println("生命周期:", Colors.CYAN);
            context.println("  list                      列出所有在线 Agent（自动清理死文件）", Colors.WHITE);
            context.println("  start <pkg>               请求启动目标应用的 Agent", Colors.WHITE);
            context.println("  stop <pkg>                停止目标应用的 Agent", Colors.WHITE);
            context.println("  run <pkg> <command>       在目标应用上代理执行任意命令", Colors.WHITE);
            context.println("", Colors.DEFAULT);
            context.println("示例: agent run com.target class info java.lang.String", Colors.GREEN);
            CommandResult result = new CommandResult();
            result.setSuccess(false);
            result.setMessage("缺少子命令");
            return result;
        }
        try {
            return CommandRouter.getInstance().dispatch(context);
        } catch (Throwable t) {
            throw t instanceof Exception ? (Exception) t : new RuntimeException(t);
        }
    }
}
