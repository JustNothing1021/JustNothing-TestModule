package com.justnothing.testmodule.command.functions.help;

import static com.justnothing.testmodule.constants.CommandServer.CMD_HELP_VER;

import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.output.Colors;

import com.justnothing.testmodule.command.base.RegisterCommand;

@RegisterCommand("help")
public class HelpMain extends MainCommand<CommandRequest, CommandResult> {

    public HelpMain() {
        super("Help", CommandResult.class);
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: help [cmd_name]
                
                获取帮助.
                
                示例:
                    help               - 显示所有命令的帮助
                    help list          - 显示list命令的详细帮助
                    help watch         - 显示watch命令的详细帮助
                
                提示: 输入不带参数的help可以查看所有可用命令列表
                
                (Submodule help %s)
                
                """, CMD_HELP_VER);
    }

    @Override
    public CommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();
        if (args.length > 0) {
            String commandName = args[0];
            MainCommand command = CommandExecutor.getCommand(commandName);
            if (command != null) {
                context.println(command.getHelpText(), Colors.WHITE);
            } else {
                StringBuilder sb = new StringBuilder("未知的命令: " + commandName);
                sb.append("\n\n可用命令:\n");
                for (String name : CommandExecutor.getAllCommands().keySet()) {
                    sb.append("  ").append(name).append("\n");
                }
                context.println(sb.toString(), Colors.WHITE);
            }
        } else {
            context.println(CommandExecutor.getHelpText(), Colors.WHITE);
        }

        if (shouldReturnStructuredData(context)) {
            return createSuccessResult("帮助信息已显示");
        }

        return null;
    }
}
