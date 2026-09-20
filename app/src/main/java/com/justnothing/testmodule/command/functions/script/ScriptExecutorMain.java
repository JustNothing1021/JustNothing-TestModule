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
    description = "JustNothing 脚本解释器 - 执行/管理 Java 脚本"
)
@CmdRoutes({
    @CmdRoutes.Route(path = "create", request = ScriptCreateRequest.class, handler = ScriptCrudCommand.class, description = "创建新脚本"),
    @CmdRoutes.Route(path = "list", request = ScriptListRequest.class, handler = ScriptManageCommand.class, description = "列出所有脚本"),
    @CmdRoutes.Route(path = "vars", request = ScriptVarsRequest.class, handler = ScriptManageCommand.class, description = "列出脚本执行器变量"),
    @CmdRoutes.Route(path = "show", request = ScriptShowRequest.class, handler = ScriptCrudCommand.class, description = "显示脚本内容"),
    @CmdRoutes.Route(path = "delete", request = ScriptDeleteRequest.class, handler = ScriptCrudCommand.class, description = "删除脚本"),
    @CmdRoutes.Route(path = "run", request = ScriptRunRequest.class, handler = ScriptExecCommand.class, description = "执行脚本"),
    @CmdRoutes.Route(path = "import", request = ScriptImportRequest.class, handler = ScriptExecCommand.class, description = "导入脚本文件"),
    @CmdRoutes.Route(path = "export", request = ScriptExportRequest.class, handler = ScriptExecCommand.class, description = "导出脚本文件"),
    @CmdRoutes.Route(path = "manage", request = ScriptManageRequest.class, handler = ScriptManageCommand.class, description = "交互式脚本管理器"),
    @CmdRoutes.Route(path = "interactive", request = ScriptInteractiveRequest.class, handler = ScriptExecCommand.class, description = "启动交互REPL执行器"),
    @CmdRoutes.Route(path = "permission/grant", request = ScriptPermGrantRequest.class, handler = ScriptPermissionCommand.class, description = "授予权限"),
    @CmdRoutes.Route(path = "permission/deny", request = ScriptPermDenyRequest.class, handler = ScriptPermissionCommand.class, description = "拒绝权限"),
    @CmdRoutes.Route(path = "permission/preset", request = ScriptPermPresetRequest.class, handler = ScriptPermissionCommand.class, description = "应用权限预设"),
    @CmdRoutes.Route(path = "permission/reset", request = ScriptPermResetRequest.class, handler = ScriptPermissionCommand.class, description = "重置权限配置"),
    @CmdRoutes.Route(path = "permission/list", request = ScriptPermListRequest.class, handler = ScriptPermissionCommand.class, description = "列出所有权限类型"),
    @CmdRoutes.Route(path = "permission/show-config", request = ScriptPermShowConfigRequest.class, handler = ScriptPermissionCommand.class, description = "显示当前权限配置"),
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
