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
    description = "用BeanShell解释器执行代码",
    version = CMD_BEAN_SHELL_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "run_code",
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
        path = "script/edit",
        request = BshScriptEditRequest.class,
        handler = BshManageCommand.class,
        description = "编辑脚本"
    ),
    @CmdRoutes.Route(
        path = "script/list",
        request = BshScriptListRequest.class,
        handler = BshQueryCommand.class,
        description = "列出所有脚本"
    ),
    @CmdRoutes.Route(
        path = "script/show",
        request = BshScriptShowRequest.class,
        handler = BshQueryCommand.class,
        description = "显示脚本内容"
    ),
    @CmdRoutes.Route(
        path = "script/delete",
        request = BshScriptDeleteRequest.class,
        handler = BshManageCommand.class,
        description = "删除脚本"
    ),
    @CmdRoutes.Route(
        path = "script/run",
        request = BshScriptRunRequest.class,
        handler = BshManageCommand.class,
        description = "执行脚本"
    ),
    @CmdRoutes.Route(
        path = "script/import",
        request = BshScriptImportRequest.class,
        handler = BshManageCommand.class,
        description = "导入脚本文件"
    ),
    @CmdRoutes.Route(
        path = "script/export",
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
}
