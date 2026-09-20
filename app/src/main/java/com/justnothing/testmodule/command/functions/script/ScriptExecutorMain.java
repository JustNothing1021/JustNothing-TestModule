package com.justnothing.testmodule.command.functions.script;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SCRIPT_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.script.impl.ScriptCrudCommand;
import com.justnothing.testmodule.command.functions.script.impl.ScriptExecCommand;
import com.justnothing.testmodule.command.functions.script.impl.ScriptManageCommand;
import com.justnothing.testmodule.command.functions.script.impl.ScriptPermissionCommand;

import com.justnothing.testmodule.command.functions.script.request.*;
import com.justnothing.testmodule.command.functions.script.response.ScriptResult;

@Cmd(
    name = "script",
    description = ScriptTexts.CMD_SCRIPT_DESC
)
@CmdRoutes({
    @CmdRoutes.Route(path = "create", request = ScriptCreateRequest.class, handler = ScriptCrudCommand.class, description = ScriptTexts.ROUTE_SCRIPT_CREATE_DESC),
    @CmdRoutes.Route(path = "list", request = ScriptListRequest.class, handler = ScriptManageCommand.class, description = ScriptTexts.ROUTE_SCRIPT_LIST_DESC),
    @CmdRoutes.Route(path = "vars", request = ScriptVarsRequest.class, handler = ScriptManageCommand.class, description = ScriptTexts.ROUTE_SCRIPT_VARS_DESC),
    @CmdRoutes.Route(path = "show", request = ScriptShowRequest.class, handler = ScriptCrudCommand.class, description = ScriptTexts.ROUTE_SCRIPT_SHOW_DESC),
    @CmdRoutes.Route(path = "delete", request = ScriptDeleteRequest.class, handler = ScriptCrudCommand.class, description = ScriptTexts.ROUTE_SCRIPT_DELETE_DESC),
    @CmdRoutes.Route(path = "run", request = ScriptRunRequest.class, handler = ScriptExecCommand.class, description = ScriptTexts.ROUTE_SCRIPT_RUN_DESC),
    @CmdRoutes.Route(path = "import", request = ScriptImportRequest.class, handler = ScriptExecCommand.class, description = ScriptTexts.ROUTE_SCRIPT_IMPORT_DESC),
    @CmdRoutes.Route(path = "export", request = ScriptExportRequest.class, handler = ScriptExecCommand.class, description = ScriptTexts.ROUTE_SCRIPT_EXPORT_DESC),
    @CmdRoutes.Route(path = "manage", request = ScriptManageRequest.class, handler = ScriptManageCommand.class, description = ScriptTexts.ROUTE_SCRIPT_MANAGE_DESC),
    @CmdRoutes.Route(path = "interactive", request = ScriptInteractiveRequest.class, handler = ScriptExecCommand.class, description = ScriptTexts.ROUTE_SCRIPT_INTERACTIVE_DESC),
    @CmdRoutes.Route(path = "permission/grant", request = ScriptPermGrantRequest.class, handler = ScriptPermissionCommand.class, description = ScriptTexts.ROUTE_SCRIPT_PERMISSION_GRANT_DESC),
    @CmdRoutes.Route(path = "permission/deny", request = ScriptPermDenyRequest.class, handler = ScriptPermissionCommand.class, description = ScriptTexts.ROUTE_SCRIPT_PERMISSION_DENY_DESC),
    @CmdRoutes.Route(path = "permission/preset", request = ScriptPermPresetRequest.class, handler = ScriptPermissionCommand.class, description = ScriptTexts.ROUTE_SCRIPT_PERMISSION_PRESET_DESC),
    @CmdRoutes.Route(path = "permission/reset", request = ScriptPermResetRequest.class, handler = ScriptPermissionCommand.class, description = ScriptTexts.ROUTE_SCRIPT_PERMISSION_RESET_DESC),
    @CmdRoutes.Route(path = "permission/list", request = ScriptPermListRequest.class, handler = ScriptPermissionCommand.class, description = ScriptTexts.ROUTE_SCRIPT_PERMISSION_LIST_DESC),
    @CmdRoutes.Route(path = "permission/show-config", request = ScriptPermShowConfigRequest.class, handler = ScriptPermissionCommand.class, description = ScriptTexts.ROUTE_SCRIPT_PERMISSION_SHOW_CONFIG_DESC),
})
public class ScriptExecutorMain extends MainCommand<ScriptResult> {

    private final String commandName;

    public ScriptExecutorMain() {
        super("script", ScriptResult.class);
        this.commandName = "script";
    }

    public ScriptExecutorMain(String commandName) {
        super("ScriptExecutor", ScriptResult.class);
        this.commandName = commandName;
    }

    @Override
    public String getHelpText() {
        return switch (commandName) {
            case "sclear" -> String.format("""
                    语法: sclear

                    清空脚本执行器的所有变量.

                    示例:
                        sclear

                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            case "svars" -> String.format("""
                    语法: svars

                    显示脚本执行器的变量列表.

                    示例:
                        svars

                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            case "srun" -> String.format("""
                    语法: srun <code>

                    快捷执行脚本代码.
                    具体执行逻辑与script run相同.
                    (注: 运行script可以查看说明)

                    示例:
                        srun 'String a = "114514"; println(a);'
                        srun 'for (int i = 0; i < 10; i++) println(i);'

                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            case "sinteractive" -> String.format("""
                    语法: sinteractive

                    进入交互式脚本执行模式.

                    多行模式:
                        :multi     - 进入多行模式
                        :eval      - 执行多行代码
                        :clear     - 清空缓冲区
                        (自动检测括号未闭合时也会进入多行模式)

                    调试:
                        setPrintAST(true)  - 开启AST打印
                        setPrintAST(false) - 关闭AST打印

                    退出命令:
                        exit, quit  - 退出交互式模式

                    示例:
                        sinteractive

                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            default -> CommandRouter.getInstance().generateHelpForCommand("script");
        };
    }
}
