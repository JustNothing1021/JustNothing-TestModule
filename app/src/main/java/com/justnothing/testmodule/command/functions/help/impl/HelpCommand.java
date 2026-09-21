package com.justnothing.testmodule.command.functions.help.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.model.NoArgRequest;
import com.justnothing.testmodule.command.framework.output.Colors;

public class HelpCommand extends AbstractCommand<NoArgRequest, CommandResult> {

    public HelpCommand() {
        super("help", NoArgRequest.class, CommandResult.class);
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<NoArgRequest> ctx) {
        String[] args = ctx.args();

        if (args.length > 0) {
            String commandName = args[0];
            MainCommand<? extends CommandResult> command = CommandExecutor.getCommand(commandName);
            if (command != null) {
                ctx.println(command.getHelpText(), Colors.WHITE);
            } else {
                StringBuilder sb = new StringBuilder(
                        CliMessages.ERR_UNKNOWN_COMMAND.format(commandName));
                // 「可用命令」复用顶层帮助里那个小节标题，别在命令清单上再译一个变体。
                sb.append("\n\n").append(CliMessages.HELP_GROUP_GENERAL.text()).append(":\n");
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
        result.setMessage(Text.zhEn("帮助信息已显示", "Help shown").text());
        return result;
    }
}