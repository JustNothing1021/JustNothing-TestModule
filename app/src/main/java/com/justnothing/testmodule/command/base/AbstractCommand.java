package com.justnothing.testmodule.command.base;

import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

public abstract class AbstractCommand implements Command {
    protected final String commandName;

    protected AbstractCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public String execute(CommandContext context) {
        try {
            return executeInternal(context);
        } catch (Exception e) {
            return CommandExceptionHandler.handleException(
                commandName, 
                e, 
                context.getLogger(), 
                "执行" + commandName + "命令失败"
            );
        }
    }

    protected abstract String executeInternal(CommandContext context) throws Exception;

    protected String requireArgs(CommandContext context, int minArgs) {
        if (context.getArgs().length < minArgs) {
            context.getLogger().warn("参数不足，需要至少" + minArgs + "个参数");
            return getHelpText();
        }
        return null;
    }

    @Override
    public String getHelpText() {
        return "用法: " + commandName + " <args>\n" +
               "输入 " + commandName + " --help 查看详细帮助";
    }
}
