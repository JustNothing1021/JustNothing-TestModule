package com.justnothing.testmodule.command.functions.hook;

import static com.justnothing.testmodule.constants.CommandServer.CMD_HOOK_VER;

import com.justnothing.javainterpreter.evaluator.DynamicClassGenerator;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.reflect.DexClassDefiner;
import com.justnothing.testmodule.utils.logging.Logger;

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
    description = "动态Hook注入器, 通过脚本实现Hook功能",
    defaultResultType = HookListResult.class
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

    private static final Logger logger = Logger.getLoggerForName("HookMain");

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

    @Override
    public HookListResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        logger.debug("执行 hook 命令（fallback路径），参数: %s", java.util.Arrays.toString(args));

        if (args.length == 0) {
            context.println(getHelpText(), Colors.WHITE);
            return null;
        }

        context.println("提示: 使用 'hook help' 查看路由帮助", Colors.CYAN);
        return createErrorResult("旧路径已迁移到 @CmdRoutes，请使用新路径");
    }
}
