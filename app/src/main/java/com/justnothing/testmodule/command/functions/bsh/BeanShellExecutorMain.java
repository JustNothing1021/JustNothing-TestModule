package com.justnothing.testmodule.command.functions.bsh;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BEAN_SHELL_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.bsh.impl.BshManageCommand;
import com.justnothing.testmodule.command.functions.bsh.impl.BshQueryCommand;
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

@Cmd(
    name = "bsh",
    description = BshTexts.CMD_BSH_DESC,
    version = CMD_BEAN_SHELL_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "run_code",
        request = BshExecuteRequest.class,
        handler = BshManageCommand.class,
        description = BshTexts.ROUTE_BSH_RUN_CODE_DESC
    ),
    @CmdRoutes.Route(
        path = "vars",
        request = BshVarsRequest.class,
        handler = BshQueryCommand.class,
        description = BshTexts.ROUTE_BSH_VARS_DESC
    ),
    @CmdRoutes.Route(
        path = "clear",
        request = BshClearRequest.class,
        handler = BshManageCommand.class,
        description = BshTexts.ROUTE_BSH_CLEAR_DESC
    ),
    @CmdRoutes.Route(
        path = "script",
        request = BshScriptCreateRequest.class,
        handler = BshManageCommand.class,
        description = BshTexts.ROUTE_BSH_SCRIPT_DESC
    ),
    @CmdRoutes.Route(
        path = "script/edit",
        request = BshScriptEditRequest.class,
        handler = BshManageCommand.class,
        description = BshTexts.ROUTE_BSH_SCRIPT_EDIT_DESC
    ),
    @CmdRoutes.Route(
        path = "script/list",
        request = BshScriptListRequest.class,
        handler = BshQueryCommand.class,
        description = BshTexts.ROUTE_BSH_SCRIPT_LIST_DESC
    ),
    @CmdRoutes.Route(
        path = "script/show",
        request = BshScriptShowRequest.class,
        handler = BshQueryCommand.class,
        description = BshTexts.ROUTE_BSH_SCRIPT_SHOW_DESC
    ),
    @CmdRoutes.Route(
        path = "script/delete",
        request = BshScriptDeleteRequest.class,
        handler = BshManageCommand.class,
        description = BshTexts.ROUTE_BSH_SCRIPT_DELETE_DESC
    ),
    @CmdRoutes.Route(
        path = "script/run",
        request = BshScriptRunRequest.class,
        handler = BshManageCommand.class,
        description = BshTexts.ROUTE_BSH_SCRIPT_RUN_DESC
    ),
    @CmdRoutes.Route(
        path = "script/import",
        request = BshScriptImportRequest.class,
        handler = BshManageCommand.class,
        description = BshTexts.ROUTE_BSH_SCRIPT_IMPORT_DESC
    ),
    @CmdRoutes.Route(
        path = "script/export",
        request = BshScriptExportRequest.class,
        handler = BshManageCommand.class,
        description = BshTexts.ROUTE_BSH_SCRIPT_EXPORT_DESC
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
}
