package com.justnothing.testmodule.command.functions.packages;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.*;
import com.justnothing.testmodule.command.base.command.CommandInfo;
import com.justnothing.testmodule.command.base.command.RegisterCommand;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.command.SubCommands;
import com.justnothing.testmodule.command.base.command.SupportsRequests;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.utils.logging.Logger;

@CommandInfo(
    name = "packages",
    group = "general",
    description = "列出当前进程的所有已知包名",
    helpText = """
            语法: packages
            
            列出当前进程的所有已知包名。
            
            示例:
                packages
            """,
    resultType = PackagesResult.class,
    defaultSubcommand = "list"
)
@SubCommands({
    @SubCommand(value = "list", request = PackagesRequest.class, description = "列出所有包名")
})
@RegisterCommand("packages")
@SupportsRequests(PackagesRequest.class)
public class PackagesMain extends MainCommand<PackagesResult> {

    private final Logger logger = Logger.getLoggerForName("PackagesMain");

    public PackagesMain() {
        super("packages", PackagesResult.class);
    }

    @Override
    public PackagesResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        logger.debug("执行 packages 命令 (使用 @CommandInfo 声明式架构)");

        if (context.getRequest() == null) {
            context.setRequest(new PackagesRequest());
        }

        PackagesCommand command = new PackagesCommand();
        return command.execute(context);
    }
}
