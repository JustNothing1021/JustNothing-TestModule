package com.justnothing.testmodule.command.functions.trace;

import static com.justnothing.testmodule.constants.CommandServer.CMD_TRACE_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.logging.Logger;

import com.justnothing.testmodule.command.functions.trace.request.TraceAddRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceListRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceShowRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceExportRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceStopRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceClearRequest;
import com.justnothing.testmodule.command.functions.trace.impl.TraceManageCommand;
import com.justnothing.testmodule.command.functions.trace.impl.TraceQueryCommand;

@Cmd(
    name = "trace",
    description = "跟踪方法调用链，生成调用树",
    defaultResultType = TraceResult.class
)
@CmdRoutes({
        @CmdRoutes.Route(path = "add", request = TraceAddRequest.class, handler = TraceManageCommand.class, description = "添加trace任务"),
        @CmdRoutes.Route(path = "list", request = TraceListRequest.class, handler = TraceQueryCommand.class, description = "列出所有任务"),
        @CmdRoutes.Route(path = "show", request = TraceShowRequest.class, handler = TraceQueryCommand.class, description = "显示调用树"),
        @CmdRoutes.Route(path = "export", request = TraceExportRequest.class, handler = TraceQueryCommand.class, description = "导出结果"),
        @CmdRoutes.Route(path = "stop", request = TraceStopRequest.class, handler = TraceManageCommand.class, description = "停止任务"),
        @CmdRoutes.Route(path = "clear", request = TraceClearRequest.class, handler = TraceManageCommand.class, description = "清除所有任务")
    })
public class TraceMain extends MainCommand<TraceResult> {

    private static final Logger logger = Logger.getLoggerForName("TraceMain");

    public TraceMain() {
        super("trace", TraceResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("trace");
    }

    @Override
    public TraceResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        logger.debug("执行 trace 命令（fallback路径），参数: %s", java.util.Arrays.toString(args));

        if (args.length == 0) {
            context.println(getHelpText(), Colors.WHITE);
            return null;
        }

        context.println("提示: 使用 'trace help' 查看路由帮助", Colors.CYAN);
        return createErrorResult("旧路径已迁移到 @CmdRoutes，请使用新路径");
    }
}
