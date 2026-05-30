package com.justnothing.testmodule.command.functions.alias;

import java.util.Arrays;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CmdParamProcessor;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.alias.impl.AbstractAliasCommand;
import com.justnothing.testmodule.command.functions.alias.impl.AliasAddCommand;
import com.justnothing.testmodule.command.functions.alias.impl.AliasClearCommand;
import com.justnothing.testmodule.command.functions.alias.impl.AliasCommandRegistry;
import com.justnothing.testmodule.command.functions.alias.impl.AliasListCommand;
import com.justnothing.testmodule.command.functions.alias.impl.AliasRemoveCommand;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.alias.request.AliasAddRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasListRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasRemoveRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasClearRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.command.functions.alias.util.AliasManager;
import com.justnothing.testmodule.constants.CommandServer;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.command.base.AbstractCommand;

import java.io.File;
import java.util.Map;

@Cmd(
    version = CommandServer.CMD_ALIAS_VER,
    name = "alias",
    description = "管理命令别名，用于简化常用命令",
    defaultResultType = AliasResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "add",
        request = AliasAddRequest.class,
        handler = AliasAddCommand.class,
        description = "添加新的命令别名"
    ),
    @CmdRoutes.Route(
        path = "list",
        request = AliasListRequest.class,
        handler = AliasListCommand.class,
        description = "列出所有已定义的别名"
    ),
    @CmdRoutes.Route(
        path = "remove",
        request = AliasRemoveRequest.class,
        handler = AliasRemoveCommand.class,
        description = "删除指定的别名"
    ),
    @CmdRoutes.Route(
        path = "clear",
        request = AliasClearRequest.class,
        handler = AliasClearCommand.class,
        description = "清空所有别名"
    )
})
public class AliasMain extends MainCommand<AliasResult> {

    private final Logger logger = Logger.getLoggerForName("AliasMain");

    public AliasMain() {
        super("alias", AliasResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("alias");
    }

    /**
     * 解析命令中的别名 (CommandExecutor 调用)
     */
    public static String resolveAlias(String command) {
        if (command == null || command.trim().isEmpty()) return command;
        AliasManager aliasManager = getAliasManager();
        Map<String, String> aliases = aliasManager.getAllAliases();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            if (command.startsWith(entry.getKey() + " ") || command.equals(entry.getKey())) {
                return entry.getValue() + command.substring(entry.getKey().length());
            }
        }
        return command;
    }

    private static AliasManager getAliasManager() {
        String dataDir = DataDirectoryManager.getMethodsCmdlineDataDirectory();
        return AliasManager.getInstance(new File(dataDir));
    }

    @Override
    public AliasResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        logger.debug("执行 alias 命令，参数: " + Arrays.toString(args));

        try {
            if (args.length < 1) {
                context.println(getHelpText(), Colors.WHITE);
                return createErrorResult("参数不足，使用 alias <subcmd> [args...]");
            }

            String subCommand = args[0].toLowerCase();

            Object commandObj = AliasCommandRegistry.getCommand(subCommand);
            if (commandObj == null) {
                if (context.isCli()) {
                    context.println("未知子命令: " + subCommand + ", 输入 alias 获取帮助", Colors.RED);
                }
                throw new IllegalCommandLineArgumentException("未知子命令: " + subCommand);
            }

            logger.debug("执行 alias 子命令: " + subCommand);

            CommandRouter.RouteMatch match = CommandRouter.getInstance()
                .matchRoute("alias", new String[]{subCommand});

            CommandRequest request;
            if (match != null && match.routeConfig() != null) {
                Class<? extends CommandRequest> requestType = match.routeConfig().requestType();
                request = requestType.getDeclaredConstructor().newInstance();
                String[] remainingArgs = args.length > 1
                    ? Arrays.copyOfRange(args, 1, args.length)
                    : new String[0];
                CmdParamProcessor.parseCommandLineArgs(request, remainingArgs);
            } else {
                request = new AliasListRequest();
            }

            context.setRequest(request);

            if (commandObj instanceof AbstractAliasCommand) {
                return (AliasResult) ((AbstractAliasCommand<?, ?>) commandObj).execute(context);
            } else if (commandObj instanceof AbstractCommand) {
                return (AliasResult) ((AbstractCommand<?, ?>) commandObj).execute(context);
            } else {
                throw new IllegalStateException("无法执行的命令类型: " + commandObj.getClass());
            }

        } catch (IllegalCommandLineArgumentException e) {
            throw e;
        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "alias", e, context, "执行 alias 命令失败"
            );
            return createErrorResult("执行 alias 命令失败: " + e.getMessage());
        }
    }
}
