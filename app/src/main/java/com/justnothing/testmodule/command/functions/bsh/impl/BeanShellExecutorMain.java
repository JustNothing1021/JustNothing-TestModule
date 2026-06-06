package com.justnothing.testmodule.command.functions.bsh.impl;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BEAN_SHELL_VER;

import java.util.Arrays;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdParamProcessor;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.bsh.request.BshClearRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshExecuteRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptCreateRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptDeleteRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptEditRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptExportRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptImportRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptListRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptRunRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptShowRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshVarsRequest;
import com.justnothing.testmodule.command.functions.bsh.response.BeanShellResult;
import com.justnothing.testmodule.command.output.Colors;

@Cmd(
    name = "bsh",
    description = "用BeanShell解释器执行代码",
    version = CMD_BEAN_SHELL_VER,
    defaultResultType = BeanShellResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "",
        request = BshExecuteRequest.class,
        handler = BshManageCommand.class,
        description = "执行BeanShell代码"
    ),
    @CmdRoutes.Route(
        path = "vars",
        request = BshVarsRequest.class,
        handler = BshQueryCommand.class,
        description = "显示BeanShell执行器的变量列表"
    ),
    @CmdRoutes.Route(
        path = "clear",
        request = BshClearRequest.class,
        handler = BshManageCommand.class,
        description = "清空BeanShell执行器的所有变量"
    ),
    @CmdRoutes.Route(
        path = "script",
        request = BshScriptCreateRequest.class,
        handler = BshManageCommand.class,
        description = "BeanShell脚本管理"
    ),
    @CmdRoutes.Route(
        path = "script:create",
        request = BshScriptCreateRequest.class,
        handler = BshManageCommand.class,
        description = "创建新脚本"
    ),
    @CmdRoutes.Route(
        path = "script:edit",
        request = BshScriptEditRequest.class,
        handler = BshManageCommand.class,
        description = "编辑脚本"
    ),
    @CmdRoutes.Route(
        path = "script:list",
        request = BshScriptListRequest.class,
        handler = BshQueryCommand.class,
        description = "列出所有脚本"
    ),
    @CmdRoutes.Route(
        path = "script:show",
        request = BshScriptShowRequest.class,
        handler = BshQueryCommand.class,
        description = "显示脚本内容"
    ),
    @CmdRoutes.Route(
        path = "script:delete",
        request = BshScriptDeleteRequest.class,
        handler = BshManageCommand.class,
        description = "删除脚本"
    ),
    @CmdRoutes.Route(
        path = "script:run",
        request = BshScriptRunRequest.class,
        handler = BshManageCommand.class,
        description = "执行脚本"
    ),
    @CmdRoutes.Route(
        path = "script:import",
        request = BshScriptImportRequest.class,
        handler = BshManageCommand.class,
        description = "导入脚本文件"
    ),
    @CmdRoutes.Route(
        path = "script:export",
        request = BshScriptExportRequest.class,
        handler = BshManageCommand.class,
        description = "导出脚本文件"
    )
})
public class BeanShellExecutorMain extends MainCommand<BeanShellResult> {

    public BeanShellExecutorMain() {
        super("bsh", BeanShellResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("bsh");
    }

    @Override
    public BeanShellResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        if (args.length < 1) {
            context.println(getHelpText(), Colors.WHITE);
            return createErrorResult("参数不足，使用 bsh <subcmd> [args...]");
        }

        String subCommand = args[0].toLowerCase();

        CommandRouter.RouteMatch match = CommandRouter.getInstance()
            .matchRoute("bsh", new String[]{subCommand});

        if (match == null || match.routeConfig() == null) {
            if (context.isCli()) {
                context.println("未知子命令: " + subCommand + ", 输入 bsh 获取帮助", Colors.RED);
            }
            throw new IllegalCommandLineArgumentException("未知子命令: " + subCommand);
        }

        Class<? extends CommandRequest> requestType = match.routeConfig().requestType();
        CommandRequest request = requestType.getDeclaredConstructor().newInstance();

        String[] remainingArgs = args.length > 1
            ? Arrays.copyOfRange(args, 1, args.length)
            : new String[0];
        CmdParamProcessor.parseCommandLineArgs(request, remainingArgs);

        context.setRequest(request);

        Class<?> handlerType = match.routeConfig().handlerType();
        Object handlerInstance = handlerType.getDeclaredConstructor().newInstance();

        if (handlerInstance instanceof AbstractBeanShellCommand) {
            return (BeanShellResult) ((AbstractBeanShellCommand) handlerInstance).execute(context);
        } else {
            throw new IllegalStateException("无法执行的命令类型: " + handlerType.getName());
        }
    }
}
