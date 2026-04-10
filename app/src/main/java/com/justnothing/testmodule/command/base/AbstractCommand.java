package com.justnothing.testmodule.command.base;

import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

public abstract class AbstractCommand implements Command {
    protected final String commandName;

    protected AbstractCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public void execute(CommandContext context) {
        try {
            executeInternal(context);
        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                commandName, 
                e, 
                context.getExecContext(),
                "执行" + commandName + "命令失败"
            );
        }
    }

    protected abstract void executeInternal(CommandContext context) throws Exception;


    @Override
    public String getHelpText() {
        return "用法: " + commandName + " <args>\n" +
               "输入 " + commandName + " --help 查看详细帮助";
    }
}
