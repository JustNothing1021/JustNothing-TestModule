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
    description = "动态Hook注入器, 通过脚本实现Hook功能"
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "add",
        request = HookAddRequest.class,
        handler = HookManageCommand.class,
        description = "添加Hook"
    ),
    @CmdRoutes.Route(
        path = "remove",
        request = HookRemoveRequest.class,
        handler = HookManageCommand.class,
        description = "移除指定Hook"
    ),
    @CmdRoutes.Route(
        path = "list",
        request = HookListRequest.class,
        handler = HookQueryCommand.class,
        description = "列出所有Hook"
    ),
    @CmdRoutes.Route(
        path = "info",
        request = HookInfoRequest.class,
        handler = HookQueryCommand.class,
        description = "显示Hook详细信息"
    ),
    @CmdRoutes.Route(
        path = "output",
        request = HookOutputRequest.class,
        handler = HookQueryCommand.class,
        description = "获取Hook输出"
    ),
    @CmdRoutes.Route(
        path = "enable",
        request = HookEnableRequest.class,
        handler = HookManageCommand.class,
        description = "启用Hook"
    ),
    @CmdRoutes.Route(
        path = "disable",
        request = HookDisableRequest.class,
        handler = HookManageCommand.class,
        description = "禁用Hook"
    ),
    @CmdRoutes.Route(
        path = "clear",
        request = HookClearRequest.class,
        handler = HookManageCommand.class,
        description = "清除所有Hook"
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
