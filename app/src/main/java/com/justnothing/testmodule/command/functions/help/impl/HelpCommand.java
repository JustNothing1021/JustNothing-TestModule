package com.justnothing.testmodule.command.functions.help.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.output.Colors;

public class HelpCommand {

    public CommandResult execute(CommandExecutor.CmdExecContext<?> ctx) {
        String[] args = ctx.args();
        
        if (args.length > 0) {
            String commandName = args[0];
            MainCommand command = CommandExecutor.getCommand(commandName);
            if (command != null) {
                ctx.println(command.getHelpText(), Colors.WHITE);
            } else {
                StringBuilder sb = new StringBuilder("未知的命令: " + commandName);
                sb.append("\n\n可用命令:\n");
                for (String name : CommandExecutor.getAllCommands().keySet()) {
                    sb.append("  ").append(name).append("\n");
                }
                ctx.println(sb.toString(), Colors.WHITE);
            }
        } else {
            ctx.println(CommandExecutor.getHelpText(), Colors.WHITE);
        }

        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setMessage("帮助信息已显示");
        return result;
    }
}
