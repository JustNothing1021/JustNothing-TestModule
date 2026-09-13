package com.justnothing.testmodule.command.functions.exportcontext;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;

@Cmd(
    name = "export-context",
    group = "system",
    description = "导出设备上下文信息, 包括HTTP配置, 设备标识等",
    version = "1.0.0"
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "",
        request = ExportContextRequest.class,
        handler = ExportContextCommand.class,
        description = "导出设备上下文信息"
    )
})
public class ExportContextMain extends MainCommand<ExportContextResult> {

    public ExportContextMain() {
        super("export-context", ExportContextResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("export-context");
    }
}
