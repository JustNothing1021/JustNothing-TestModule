package com.justnothing.testmodule.command.functions.alias.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.command.RegisterCommand;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.command.SupportsRequests;
import com.justnothing.testmodule.command.functions.alias.request.AliasListRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasAddRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasRemoveRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasClearRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.command.functions.alias.util.AliasManager;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.base.AbstractCommand;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

@RegisterCommand("alias")
@SupportsRequests(AliasRequest.class)
public class AliasMain extends MainCommand<AliasResult> {

    private final Logger logger = Logger.getLoggerForName("AliasMain");

    public AliasMain() {
        super("alias", AliasResult.class);
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
        String subCommand = resolveSubCommand(context);

        Object commandObj = AliasCommandRegistry.getCommand(subCommand);
        if (commandObj == null) {
            if (context.isCli()) {
                context.println("未知子命令: " + subCommand + ", 输入 alias 获取帮助", Colors.RED);
            }
            throw new IllegalArgumentException("未知子命令: " + subCommand);
        }

        logger.debug("执行 alias 子命令: " + subCommand);

        ensureRequestExists(context, subCommand);

        if (commandObj instanceof AbstractAliasCommand) {
            return (AliasResult) ((AbstractAliasCommand<?, ?>) commandObj).execute(context);
        } else if (commandObj instanceof AbstractCommand) {
            return (AliasResult) ((AbstractCommand<?, ?>) commandObj).execute(context);
        } else {
            throw new IllegalStateException("无法执行的命令类型: " + commandObj.getClass());
        }
    }

    private void ensureRequestExists(CommandExecutor.CmdExecContext<CommandRequest> context, String subCommand) throws Exception {
        if (context.getRequest() != null) return;

        String[] args = context.args();
        switch (subCommand) {
            case "add":
                if (args.length >= 3) {
                    AliasAddRequest addReq = new AliasAddRequest();
                    addReq.setName(args[1]);
                    String command = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    addReq.setCommand(command);
                    context.setRequest(addReq);
                }
                break;
            case "remove":
                if (args.length >= 2) {
                    AliasRemoveRequest removeReq = new AliasRemoveRequest();
                    removeReq.setName(args[1]);
                    context.setRequest(removeReq);
                }
                break;
            case "list":
                context.setRequest(new AliasListRequest());
                break;
            case "clear":
                context.setRequest(new AliasClearRequest());
                break;
        }
    }

    private String resolveSubCommand(CommandExecutor.CmdExecContext<CommandRequest> context) {
        String[] args = context.args();

        if (args.length >= 1) {
            return args[0].toLowerCase();
        }

        CommandRequest request = context.getRequest();
        if (request != null) {
            SubCommand annotation = request.getClass()
                .getAnnotation(SubCommand.class);
            if (annotation != null) {
                return annotation.value();
            }
            return request.getCommandType().toLowerCase().replaceFirst("alias", "");
        }

        return "list";
    }
}
