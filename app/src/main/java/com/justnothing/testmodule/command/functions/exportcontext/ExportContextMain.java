package com.justnothing.testmodule.command.functions.exportcontext;

import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.CommandExecutor;

@Cmd(
    name = "export-context",
    group = "system",
    description = "导出设备上下文信息, 包括HTTP配置, 设备标识等",
    version = "1.0.0",
    defaultResultType = ExportContextResult.class
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

    @Override
    public ExportContextResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        ExportContextCommand command = new ExportContextCommand();
        if (context.getRequest() == null) {
            context.setRequest(new ExportContextRequest());
        }
        return command.execute(context);
    }
}
