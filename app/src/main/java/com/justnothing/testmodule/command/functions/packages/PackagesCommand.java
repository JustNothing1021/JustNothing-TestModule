package com.justnothing.testmodule.command.functions.packages;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.utils.reflect.ClassLoaderManager;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.command.output.Colors;

import java.util.List;

public class PackagesCommand extends AbstractCommand<PackagesRequest, PackagesResult> {

    private static final Logger logger = Logger.getLoggerForName("PackagesCommand");

    public PackagesCommand() {
        super("packages list", PackagesRequest.class, PackagesResult.class);
    }

    @Override
    protected PackagesResult executeInternal(CommandExecutor.CmdExecContext<PackagesRequest> context) throws Exception {
        List<String> packages = ClassLoaderManager.getAllKnownPackages();

        PackagesResult result = new PackagesResult();
        result.setPackages(packages);

        if (context.isCli()) {
            context.println("当前进程的ClassLoader:", Colors.CYAN);
            for (String pkg : packages) {
                context.println("  " + pkg, Colors.WHITE);
            }
        }

        logger.info("获取包列表完成，共 " + packages.size() + " 个包");
        return result;
    }

}
