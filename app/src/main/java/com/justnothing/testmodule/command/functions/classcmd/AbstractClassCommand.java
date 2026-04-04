package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.CommandContext;
import com.justnothing.testmodule.command.output.Colors;

public abstract class AbstractClassCommand extends AbstractCommand implements ClassCommand {

    protected AbstractClassCommand(String commandName) {
        super(commandName);
    }

    @Override
    protected String executeInternal(CommandContext context) throws Exception {
        if (!(context instanceof ClassCommandContext)) {
            throw new IllegalArgumentException("需要 ClassCommandContext");
        }
        return executeInternal((ClassCommandContext) context);
    }

    protected abstract String executeInternal(ClassCommandContext context) throws Exception;

    protected String requireArgs(ClassCommandContext context, String[] args, int minArgs) {
        if (args.length < minArgs) {
            context.getLogger().warn("参数不足，需要至少" + minArgs + "个参数");
            return getHelpText();
        }
        return null;
    }
}
