package com.justnothing.testmodule.command.functions.hook;

import com.justnothing.engine.codegen.DynamicClassGenerator;
import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.hook.response.HookListResult;
import com.justnothing.testmodule.utils.reflect.DexClassDefiner;

import com.justnothing.testmodule.command.functions.hook.request.HookAddRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookRemoveRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookListRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookInfoRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookOutputRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookEnableRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookDisableRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookClearRequest;
import com.justnothing.testmodule.command.functions.hook.impl.HookManageCommand;
import com.justnothing.testmodule.command.functions.hook.impl.HookQueryCommand;

@Cmd(
    name = "hook",
    description = HookTexts.CMD_HOOK_DESC
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "add",
        request = HookAddRequest.class,
        handler = HookManageCommand.class,
        description = HookTexts.ROUTE_HOOK_ADD_DESC
    ),
    @CmdRoutes.Route(
        path = "remove",
        request = HookRemoveRequest.class,
        handler = HookManageCommand.class,
        description = HookTexts.ROUTE_HOOK_REMOVE_DESC
    ),
    @CmdRoutes.Route(
        path = "list",
        request = HookListRequest.class,
        handler = HookQueryCommand.class,
        description = HookTexts.ROUTE_HOOK_LIST_DESC
    ),
    @CmdRoutes.Route(
        path = "info",
        request = HookInfoRequest.class,
        handler = HookQueryCommand.class,
        description = HookTexts.ROUTE_HOOK_INFO_DESC
    ),
    @CmdRoutes.Route(
        path = "output",
        request = HookOutputRequest.class,
        handler = HookQueryCommand.class,
        description = HookTexts.ROUTE_HOOK_OUTPUT_DESC
    ),
    @CmdRoutes.Route(
        path = "enable",
        request = HookEnableRequest.class,
        handler = HookManageCommand.class,
        description = HookTexts.ROUTE_HOOK_ENABLE_DESC
    ),
    @CmdRoutes.Route(
        path = "disable",
        request = HookDisableRequest.class,
        handler = HookManageCommand.class,
        description = HookTexts.ROUTE_HOOK_DISABLE_DESC
    ),
    @CmdRoutes.Route(
        path = "clear",
        request = HookClearRequest.class,
        handler = HookManageCommand.class,
        description = HookTexts.ROUTE_HOOK_CLEAR_DESC
    )
})
public class HookMain extends MainCommand<HookListResult> {

    static {
        DynamicClassGenerator.setDefaultClassDefiner(DexClassDefiner.getInstance());
    }

    public HookMain() {
        super("hook", HookListResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("hook");
    }
}
