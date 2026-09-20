package com.justnothing.testmodule.command.functions.alias;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.alias.impl.AliasAddCommand;
import com.justnothing.testmodule.command.functions.alias.impl.AliasClearCommand;
import com.justnothing.testmodule.command.functions.alias.impl.AliasListCommand;
import com.justnothing.testmodule.command.functions.alias.impl.AliasRemoveCommand;
import com.justnothing.testmodule.command.functions.alias.request.AliasAddRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasListRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasRemoveRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasClearRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.command.functions.alias.util.AliasManager;
import com.justnothing.testmodule.constants.CommandServer;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;

import java.io.File;
import java.util.Map;

@Cmd(
    version = CommandServer.CMD_ALIAS_VER,
    name = "alias",
    description = AliasTexts.CMD_ALIAS_DESC
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "add",
        request = AliasAddRequest.class,
        handler = AliasAddCommand.class,
        description = AliasTexts.ROUTE_ALIAS_ADD_DESC
    ),
    @CmdRoutes.Route(
        path = "list",
        request = AliasListRequest.class,
        handler = AliasListCommand.class,
        description = AliasTexts.ROUTE_ALIAS_LIST_DESC
    ),
    @CmdRoutes.Route(
        path = "remove",
        request = AliasRemoveRequest.class,
        handler = AliasRemoveCommand.class,
        description = AliasTexts.ROUTE_ALIAS_REMOVE_DESC
    ),
    @CmdRoutes.Route(
        path = "clear",
        request = AliasClearRequest.class,
        handler = AliasClearCommand.class,
        description = AliasTexts.ROUTE_ALIAS_CLEAR_DESC
    )
})
public class AliasMain extends MainCommand<AliasResult> {

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
}
