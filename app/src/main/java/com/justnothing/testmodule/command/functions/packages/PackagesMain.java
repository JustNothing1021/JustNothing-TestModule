package com.justnothing.testmodule.command.functions.packages;

import static com.justnothing.testmodule.constants.CommandServer.CMD_PACKAGES_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.reflect.ClassLoaderManager;

import java.util.List;
import java.util.Locale;

public class PackagesMain extends CommandBase {

    public PackagesMain() {
        super("packages");
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
    public void runMain(CommandExecutor.CmdExecContext context) {
        List<String> packages = ClassLoaderManager.getAllKnownPackages();
        context.println(String.format(Locale.getDefault(), "当前进程的ClassLoader (总计%d个):", packages.size()), Colors.CYAN);
        for (String pkg : packages) {
            context.println("  " + pkg, Colors.WHITE);
        }
        context.println("");
        context.println("提示: 用 'methods -cl <package> <command>' 可以指定ClassLoader", Colors.GRAY);
    }
}
