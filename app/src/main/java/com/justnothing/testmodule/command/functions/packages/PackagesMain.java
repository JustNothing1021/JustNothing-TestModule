package com.justnothing.testmodule.command.functions.packages;

import static com.justnothing.testmodule.constants.CommandServer.CMD_PACKAGES_VER;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.handlers.packages.PackagesRequestHandler;
import com.justnothing.testmodule.utils.reflect.ClassLoaderManager;

import java.util.List;
import java.util.Locale;

import com.justnothing.testmodule.command.base.RegisterCommand;

@RegisterCommand("packages")
public class PackagesMain extends MainCommand<PackagesRequest, PackagesResult> {

    public PackagesMain() {
        super("Packages", PackagesResult.class);
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: packages
                
                列出当前进程中所有已知的包名（ClassLoader）.
                
                示例:
                    packages
                
                提示: 使用 'methods -cl <package> <command>' 可以指定特定的ClassLoader来执行命令
                
                (Submodule packages %s)
                """, CMD_PACKAGES_VER);
    }

    @Override
    public PackagesResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        List<String> packages = ClassLoaderManager.getAllKnownPackages();
        context.println(String.format(Locale.getDefault(), "当前进程的ClassLoader (总计%d个):", packages.size()), Colors.CYAN);
        for (String pkg : packages) {
            context.println("  " + pkg, Colors.WHITE);
        }
        context.println("");
        context.println("提示: 用 'methods -cl <package> <command>' 可以指定ClassLoader", Colors.GRAY);

        // GUI/Agent模式：返回结构化数据
        if (context.isGui() || context.isAgent()) {
            PackagesRequestHandler handler = new PackagesRequestHandler();
            PackagesRequest request = new PackagesRequest();
            request.setRequestId(java.util.UUID.randomUUID().toString());
            return handler.handle(request);
        }

        return null;
    }
}
