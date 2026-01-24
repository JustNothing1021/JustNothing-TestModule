package com.justnothing.testmodule.command.functions.packages;

import static com.justnothing.testmodule.constants.CommandServer.CMD_PACKAGES_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.utils.data.ClassLoaderManager;

import java.util.List;

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
    public String runMain(CommandExecutor.CmdExecContext context) {
        List<String> packages = ClassLoaderManager.getAllKnownPackages();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("当前进程的ClassLoader (总计%d个):\n", packages.size()));
        for (String pkg : packages) {
            sb.append("  ").append(pkg).append("\n");
        }
        sb.append("\n提示: 用 'methods -cl <package> <command>' 可以指定ClassLoader");
        return sb.toString();
    }
}
