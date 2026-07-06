package com.justnothing.testmodule.command.functions.packages;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.base.MainCommand;
import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.command.Cmd;
import com.justnothing.testmodule.command.framework.base.command.CmdRoutes;
import com.justnothing.testmodule.command.framework.base.command.CommandRouter;
import com.justnothing.testmodule.command.framework.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.packages.request.PackagesRequest;
import com.justnothing.testmodule.utils.logging.Logger;

@Cmd(
    name = "packages",
    description = "列出当前进程的所有已知包名",
    defaultResultType = PackagesResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "list",
        request = PackagesRequest.class,
        handler = PackagesCommand.class,
        description = "列出所有包名"
    )
})
public class PackagesMain extends MainCommand<PackagesResult> {

    private final Logger logger = Logger.getLoggerForName("PackagesMain");

    public PackagesMain() {
        super("packages", PackagesResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("packages");
    }

    @Override
    public PackagesResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        logger.debug("执行 packages 命令 (新架构)");

        try {
            if (context.getRequest() == null) {
                context.setRequest(new PackagesRequest());
            }

            PackagesCommand command = new PackagesCommand();
            return command.execute(context);

        } catch (IllegalCommandLineArgumentException e) {
            throw e;
        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "packages", e, context, "执行 packages 命令失败"
            );
            return createErrorResult("执行 packages 命令失败: " + e.getMessage());
        }
    }
}
